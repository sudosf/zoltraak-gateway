package com.zoltraak.gateway.adapters.gpu;

import com.zoltraak.gateway.adapters.gpu.model.*;
import com.zoltraak.gateway.annotations.Adapter;
import com.zoltraak.gateway.config.properties.OllamaProperties;
import com.zoltraak.gateway.domain.enums.GpuProvider;
import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.domain.models.provider.PodConnectionDetails;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Adapter
public class VastAiAdapter implements GpuProviderPort {

    private final WebClient webClient;
    private final OllamaProperties ollamaProperties;
    private Mono<Long> instanceId;

    public VastAiAdapter(@Qualifier("providerWebClient") WebClient webClient, OllamaProperties ollamaProperties) {
        this.webClient = webClient;
        this.ollamaProperties = ollamaProperties;
    }

    @PostConstruct
    void init() {
        this.instanceId = getInstanceId();
    }

    @Override
    public Mono<Void> start() {
        return changeState("running");
    }

    @Override
    public Mono<Void> stop() {
        return changeState("stopped");
    }

    @Override
    public Mono<PodStatus> getStatus() {
        return resolveInstance()
                .map(response -> switch (response.instances().getActualStatus()) {
                    case "running", "loading" -> PodStatus.WARMING;
                    case null -> PodStatus.STARTING;
                    default -> PodStatus.STOPPED;
                });
    }

    @Override
    public Mono<PodConnectionDetails> getConnectionDetails() {
        return resolveInstance()
                .map(VastAiInstanceResponse::instances)
                .map(instance -> {
                    String ipAddress = instance.getPublicIpaddr();
                    String ollamaInstancePort = getOllamaInstancePort(instance);
                    String ollamaUrl = "http://%s:%s".formatted(ipAddress, ollamaInstancePort);

                    log.debug("Vast.ai resolved ollama url={}", ollamaUrl);
                    return new PodConnectionDetails(ollamaUrl, null);
                });
    }

    private Mono<Void> changeState(String state) {
        return instanceId.flatMap(id -> {
                    log.info("Vast.ai [{}] instance={}", state, id);
                    return webClient.put()
                            .uri("/api/v0/instances/%s".formatted(id))
                            .bodyValue(new VastAiManageRequest(state))
                            .retrieve()
                            .onStatus(code -> code.is4xxClientError() || code.is5xxServerError(),
                                    handleErrorResponse())
                            .bodyToMono(Void.class)
                            .doOnSuccess(_ -> log.debug("Vast.ai [{}] completed instance={}", state, id))
                            .doOnError(e -> log.warn("Vast.ai [{}] failed instance={}: {}", state, id, e.getMessage()));
                }
        );
    }

    private Mono<VastAiInstanceResponse> resolveInstance() {
        return instanceId
                .flatMap(this::fetchInstance)
                .flatMap(response -> {
                    if (response.instances() != null) return Mono.just(response);

                    log.warn("Vast.ai instance id not found, refreshing cache");

                    this.instanceId = getInstanceId();
                    return this.instanceId
                            .flatMap(this::fetchInstance)
                            .filter(res -> res.instances() != null)
                            .switchIfEmpty(Mono.error(new ProviderException(
                                    GpuProvider.VASTAI, 404, "No active GPU instances found on Vast.ai"))
                            );
                });
    }

    private Mono<Long> getInstanceId() {
        return fetchInstances()
                .flatMapIterable(VastAiInstancePage::instances)
                .next()
                .map(VastAiInstance::getId)
                .switchIfEmpty(Mono.error(new ProviderException(
                        GpuProvider.VASTAI, 404, "No active GPU instances found on Vast.ai")))
                .cache(
                        _ -> Duration.ofHours(3),
                        _ -> Duration.ZERO,
                        () -> Duration.ZERO
                );
    }

    private String getOllamaInstancePort(VastAiInstance instance) {
        int ollamaInternalPort = ollamaProperties.getGpuPod().getPort();

        Map<String, List<VastAiPortBinding>> ports = instance.getPorts();
        if (ports == null) {
            throw new ProviderException(GpuProvider.VASTAI, 503, "Port bindings not yet available");
        }

        List<VastAiPortBinding> portBindings = ports.get(ollamaInternalPort + "/tcp");
        return portBindings.getFirst().hostPort();
    }

    private Mono<VastAiInstancePage> fetchInstances() {
        return getAsMono("/api/v1/instances", VastAiInstancePage.class);
    }

    private Mono<VastAiInstanceResponse> fetchInstance(Long id) {
        return getAsMono("/api/v0/instances/%s".formatted(id), VastAiInstanceResponse.class);
    }

    private <T> Mono<T> getAsMono(String path, Class<T> responseType) {
        return webClient
                .get()
                .uri(path)
                .retrieve()
                .onStatus(code -> code.is4xxClientError() || code.is5xxServerError(),
                        handleErrorResponse())
                .bodyToMono(responseType)
                .doOnSubscribe(_ -> log.debug("Vast.ai GET {}", path))
                .doOnError(e -> log.warn("Vast.ai GET {} failed: {}", path, e.getMessage()));
    }

    private Function<ClientResponse, Mono<? extends Throwable>> handleErrorResponse() {
        return response -> response.bodyToMono(String.class)
                .defaultIfEmpty("No response body")
                .map(body -> new ProviderException(
                        GpuProvider.VASTAI,
                        response.statusCode().value(),
                        body));
    }
}