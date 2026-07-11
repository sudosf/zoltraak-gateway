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
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Slf4j
@Adapter
public class RunpodAdapter implements GpuProvider {

    public static final String BASE_PATH = "/pods";

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
                    String path = "%s/%s/start".formatted(BASE_PATH, id);
                    return postAsMono(path);
                })
                .onErrorResume(ProviderException.class, ex -> {
                    if (ex.getHttpStatusCode() == 404) {
                        log.info("Runpod no active pod found, renting new pod...");
                        return rentNewPod();
                    }

                    log.info("Runpod start failed, deleting and renting new pod...");
                    return this.podId.get()
                            .flatMap(this::deletePod)
                            .onErrorResume(error -> {
                                log.warn("Delete failed during recovery, proceeding to renting a new pod, error = {}",
                                        ExceptionUtils.getRootCauseMessage(error));
                                return Mono.empty();
                            })
                            .then(rentNewPod());
                })
                .doOnSuccess(_ -> log.debug("Runpod start/create completed"))
                .doOnError(e ->
                        log.debug("Runpod start/create failed, error = {}", ExceptionUtils.getRootCauseMessage(e))
                );
    }

    @Override
    public Mono<Void> stop() {
        return this.podId.get()
                .flatMap(id -> {
                    log.info("Runpod stopping pod with id = {}", id);
                    String path = "%s/%s/stop".formatted(BASE_PATH, id);
                    return postAsMono(path)
                            .doOnSuccess(_ -> log.debug("Runpod stop completed for pod with id = {}", id))
                            .doOnError(e ->
                                    log.debug("Runpod stop failed for pod with id = {}, error = {}",
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
                            Mono<String> cachedId = fetchPodId();
                            this.podId.set(cachedId);
                            return cachedId.flatMap(this::fetchPodById);
                        }));
    }

    // TODO cache this with util
    private Mono<String> fetchPodId() {
        return webClient.get()
                .uri(BASE_PATH)
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

    private Mono<RunpodPodResponse> fetchPodById(String id) {
        return webClient.get()
                .uri("%s/{id}".formatted(BASE_PATH), id)
                .retrieve()
                .onStatus(code -> code.is4xxClientError() || code.is5xxServerError(),
                        handleErrorResponse())
                .bodyToMono(RunpodPodResponse.class)
                .doOnSubscribe(_ -> log.debug("Runpod GET fetching pod by id = {}", id))
                .doOnError(e -> log.debug("Runpod GET failed, id = {}, error = {}",
                        id, ExceptionUtils.getRootCauseMessage(e))
                );
    }

    private Mono<Void> rentNewPod() {
        return createPod()
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .flatMap(id -> {
                    Mono<String> cachedId = cachedId(id);
                    podId.set(cachedId);
                    return Mono.empty();
                });
    }

    private Mono<Void> deletePod(String podId) {
        return webClient.delete()
                .uri("%s/{podId}".formatted(BASE_PATH), podId)
                .retrieve()
                .toBodilessEntity()
                .then();
    }

    private Mono<String> createPod() {
        // TODO consolidate with postAsMono
        return webClient.post()
                .uri(BASE_PATH)
                .bodyValue(providerProperties.getRunpod().getCreate())
                .retrieve()
                .onStatus(code -> code.is4xxClientError() || code.is5xxServerError(), handleErrorResponse())
                .bodyToMono(RunpodPodResponse.class)
                .flatMap(pod -> {
                    log.info("Runpod rented new pod [{}]. region: {}, Cost: ${}/hr",
                            pod.machine().gpuTypeId(),
                            pod.machine().dataCenterId(),
                            pod.costPerHr());

                    return Mono.just(pod.id());
                });
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

    private Mono<String> cachedId(String id) {
        return Mono.just(id)
                .cache(
                        _ -> Duration.ofHours(providerProperties.getIdCacheHours()),
                        _ -> Duration.ZERO,
                        () -> Duration.ZERO
                );
    }
}
