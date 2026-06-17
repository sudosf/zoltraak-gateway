package com.zoltraak.gateway.adapters.gpu.runpod;

import com.zoltraak.gateway.adapters.gpu.GpuProvider;
import com.zoltraak.gateway.adapters.gpu.ProviderException;
import com.zoltraak.gateway.adapters.gpu.runpod.model.RunpodPodResponse;
import com.zoltraak.gateway.annotations.Adapter;
import com.zoltraak.gateway.config.properties.OllamaProperties;
import com.zoltraak.gateway.config.properties.ProviderProperties;
import com.zoltraak.gateway.domain.enums.GpuProviderType;
import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.domain.models.provider.PodConnectionDetails;
import com.zoltraak.gateway.exception.ExceptionUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Slf4j
@Adapter
public class RunpodAdapter implements GpuProvider {
    private final WebClient webClient;
    private final OllamaProperties ollamaProperties;
    private final ProviderProperties providerProperties;
    private final AtomicReference<Mono<String>> podId;

    public RunpodAdapter(
            @Qualifier("runpodWebClient") WebClient webClient,
            OllamaProperties ollamaProperties,
            ProviderProperties providerProperties) {
        this.webClient = webClient;
        this.ollamaProperties = ollamaProperties;
        this.providerProperties = providerProperties;
        this.podId = new AtomicReference<>();
    }

    @PostConstruct
    void init() {
        this.podId.set(fetchPodId());
    }

    @Override
    public Mono<Void> start() {
        return this.podId.get()
                .flatMap(id -> {
                    log.info("Runpod starting pod with id = {}", id);
                    String path = "/pods/%s/start".formatted(id);
                    return postAsMono(path)
                            .doOnSuccess(_ -> log.debug("Runpod start completed for pod with id = {}", id))
                            .doOnError(e ->
                                    log.warn("Runpod start failed for pod with id = {}, error = {}",
                                            id, ExceptionUtils.getRootCauseMessage(e))
                            );
                });
    }

    @Override
    public Mono<Void> stop() {
        return this.podId.get()
                .flatMap(id -> {
                    log.info("Runpod stopping pod with id = {}", id);
                    String path = "/pods/%s/stop".formatted(id);
                    return postAsMono(path)
                            .doOnSuccess(_ -> log.debug("Runpod stop completed for pod with id = {}", id))
                            .doOnError(e ->
                                    log.warn("Runpod stop failed for pod with id = {}, error = {}",
                                            id, ExceptionUtils.getRootCauseMessage(e))
                            );
                });
    }

    @Override
    public Mono<PodStatus> getStatus() {
        return resolvePod()
                .map(pod -> switch (pod.desiredStatus()) {
                    case "RUNNING" -> PodStatus.WARMING;
                    case "EXITED", "TERMINATED" -> PodStatus.STOPPED;
                    case null -> PodStatus.STARTING;
                    default -> {
                        log.warn("Runpod unrecognised status = {}, pod = {}", pod.desiredStatus(), pod.id());
                        yield PodStatus.STOPPED;
                    }
                });
    }

    @Override
    public Mono<PodConnectionDetails> getConnectionDetails() {
        return resolvePod()
                .map(pod -> {
                    int ollamaPort = ollamaProperties.getGpuPod().getPort();
                    String ollamaUrl = "https://%s-%d.proxy.runpod.net".formatted(pod.id(), ollamaPort);
                    log.debug("Runpod resolved ollama url = {}", ollamaUrl);

                    return new PodConnectionDetails(ollamaUrl, null);
                });
    }

    private Mono<RunpodPodResponse> resolvePod() {
        return this.podId.get()
                .flatMap(id -> fetchPodById(id)
                        .onErrorResume(ProviderException.class, ex -> {
                            if (ex.getHttpStatusCode() != 404) return Mono.error(ex);

                            log.warn("Runpod pod id not found, refreshing cache");
                            this.podId.set(fetchPodId());
                            return this.podId.get().flatMap(this::fetchPodById);
                        }));
    }

    private Mono<RunpodPodResponse> fetchPodById(String id) {
        return webClient.get()
                .uri("/pods/%s".formatted(id))
                .retrieve()
                .onStatus(code -> code.is4xxClientError() || code.is5xxServerError(),
                        handleErrorResponse())
                .bodyToMono(RunpodPodResponse.class)
                .doOnSubscribe(_ -> log.debug("Runpod GET fetching pod by id = {}", id))
                .doOnError(e -> log.warn("Runpod GET failed, id = {}, error = {}",
                        id, ExceptionUtils.getRootCauseMessage(e))
                );
    }

    private Mono<String> fetchPodId() {
        return webClient.get()
                .uri("/pods")
                .retrieve()
                .onStatus(code -> code.is4xxClientError() || code.is5xxServerError(),
                        handleErrorResponse())
                .bodyToFlux(RunpodPodResponse.class)
                .next()
                .map(RunpodPodResponse::id)
                .switchIfEmpty(Mono.error(new ProviderException(
                        GpuProviderType.RUNPOD, 404, "No active GPU pods found on Runpod")))
                .cache(
                        _ -> Duration.ofHours(providerProperties.getIdCacheHours()),
                        _ -> Duration.ZERO,
                        () -> Duration.ZERO
                );
    }

    private Mono<Void> postAsMono(String path) {
        return webClient.post()
                .uri(path)
                .retrieve()
                .onStatus(code -> code.is4xxClientError() || code.is5xxServerError(),
                        handleErrorResponse())
                .bodyToMono(Void.class);
    }

    private Function<ClientResponse, Mono<? extends Throwable>> handleErrorResponse() {
        return response -> response.bodyToMono(String.class)
                .defaultIfEmpty("No response body")
                .map(body -> new ProviderException(GpuProviderType.RUNPOD, response.statusCode().value(), body));
    }
}
