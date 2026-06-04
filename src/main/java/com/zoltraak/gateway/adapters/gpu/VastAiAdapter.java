package com.zoltraak.gateway.adapters.gpu;

import com.zoltraak.gateway.adapters.gpu.model.*;
import com.zoltraak.gateway.annotations.Adapter;
import com.zoltraak.gateway.config.properties.OllamaProperties;
import com.zoltraak.gateway.domain.enums.GpuProvider;
import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.domain.models.provider.PodConnectionDetails;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

@Adapter
@ConditionalOnProperty(name = "zoltraak.provider.active", havingValue = "vastai")
public class VastAiAdapter implements GpuProviderPort {

    private final WebClient webClient;
    private final OllamaProperties ollamaProperties;
    private Mono<VastAiInstance> instanceMetadata;

    public VastAiAdapter(@Qualifier("providerWebClient") WebClient webClient, OllamaProperties ollamaProperties) {
        this.webClient = webClient;
        this.ollamaProperties = ollamaProperties;
    }

    @PostConstruct
    void init() {
        this.instanceMetadata = fetchInstances()
                .flatMapIterable(VastAiInstancePage::instances)
                .next()
                .switchIfEmpty(Mono.error(new ProviderException(
                        GpuProvider.VASTAI, 404, "No active GPU instances found on Vast.ai")))
                .cache(
                        _ -> Duration.ofDays(365),
                        _ -> Duration.ZERO,
                        () -> Duration.ZERO
                );
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
        return instanceMetadata
                .flatMap(metadata -> fetchInstance(metadata.getId()))
                .switchIfEmpty(Mono.error(new ProviderException(
                        GpuProvider.VASTAI, 500, "Empty response from Vast.ai")))
                .map(instance -> switch (instance.instances().getActualStatus()) {
                    case "running" -> PodStatus.READY;
                    case "loading" -> PodStatus.WARMING;
                    case null -> PodStatus.STARTING;
                    default -> PodStatus.STOPPED;
                });
    }

    @Override
    public Mono<PodConnectionDetails> getConnectionDetails() {
        return instanceMetadata
                .switchIfEmpty(Mono.error(new ProviderException(
                        GpuProvider.VASTAI, 500, "Empty response from Vast.ai")))
                .map(instance -> {
                    String ipAddress = instance.getPublicIpaddr();
                    String ollamaInstancePort = getOllamaInstancePort(instance);
                    String ollamaUrl = "http://%s:%s".formatted(ipAddress, ollamaInstancePort);
                    return new PodConnectionDetails(ollamaUrl, null);
                });
    }

    private Mono<Void> changeState(String state) {
        return instanceMetadata.flatMap(instance ->
                webClient.put()
                        .uri("/api/v0/instances/%s".formatted(instance.getId()))
                        .bodyValue(new VastAiManageRequest(state))
                        .retrieve()
                        .onStatus(code -> code.is4xxClientError() || code.is5xxServerError(),
                                handleErrorResponse())
                        .bodyToMono(Void.class)
        );
    }

    private String getOllamaInstancePort(VastAiInstance instance) {
        int ollamaInternalPort = ollamaProperties.getGpuPod().getPort();
        List<VastAiPortBinding> portBindings = instance.getPorts().get(ollamaInternalPort + "/tcp");
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
                .bodyToMono(responseType);
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