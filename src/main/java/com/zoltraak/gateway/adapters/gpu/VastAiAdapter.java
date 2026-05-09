package com.zoltraak.gateway.adapters.gpu;

import com.zoltraak.gateway.adapters.gpu.model.VastAiInstanceWrapper;
import com.zoltraak.gateway.adapters.gpu.model.VastAiManageRequest;
import com.zoltraak.gateway.annotations.Adapter;
import com.zoltraak.gateway.config.properties.OllamaProperties;
import com.zoltraak.gateway.config.properties.ProviderProperties;
import com.zoltraak.gateway.domain.enums.GpuProvider;
import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.domain.shared.PodConnectionDetails;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Adapter
@ConditionalOnProperty(name = "zoltraak.provider.active", havingValue = "vastai")
public class VastAiAdapter implements GpuProviderPort {

    private final WebClient webClient;
    private final OllamaProperties ollamaProperties;
    private final String instanceUri;

    public VastAiAdapter(ProviderProperties providerProperties, @Qualifier("providerWebClient") WebClient webClient, OllamaProperties ollamaProperties) {
        this.webClient = webClient;
        this.ollamaProperties = ollamaProperties;
        this.instanceUri = "/api/v0/instances/%s".formatted(providerProperties.getVastAi().getInstanceId());
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
        return fetchInstance()
                .switchIfEmpty(Mono.error(new ProviderException(
                        GpuProvider.VASTAI, 500, "Empty response from Vast.ai")))
                .map(wrapper -> switch (wrapper.instances().actualStatus()) {
                    case "running" -> PodStatus.READY;
                    case "loading" -> PodStatus.WARMING;
                    case null -> PodStatus.STARTING;
                    default -> PodStatus.STOPPED;
                });
    }

    @Override
    public Mono<PodConnectionDetails> getConnectionDetails() {
        return fetchInstance()
                .switchIfEmpty(Mono.error(new ProviderException(
                        GpuProvider.VASTAI, 500, "Empty response from Vast.ai")))
                .map(wrapper -> {
                    String ipAddress = wrapper.instances().publicIpaddr();
                    String ollamaUrl = "http://%s:%d".formatted(ipAddress, ollamaProperties.getGpuPod().getPort());
                    return new PodConnectionDetails(ollamaUrl, null);
                });
    }

    private Mono<VastAiInstanceWrapper> fetchInstance() {
        return webClient
                .get()
                .uri(instanceUri)
                .retrieve()
                .onStatus(code -> code.is4xxClientError() || code.is5xxServerError(),
                        handleErrorResponse())
                .bodyToMono(VastAiInstanceWrapper.class);
    }

    private Mono<Void> changeState(String state) {
        return webClient
                .put()
                .uri(instanceUri)
                .bodyValue(new VastAiManageRequest(state))
                .retrieve()
                .onStatus(code -> code.is4xxClientError() || code.is5xxServerError(),
                        handleErrorResponse())
                .bodyToMono(Void.class);
    }

    private Function<ClientResponse, Mono<? extends Throwable>> handleErrorResponse() {
        return response -> response.bodyToMono(String.class)
                .map(body -> new ProviderException(
                        GpuProvider.VASTAI,
                        response.statusCode().value(),
                        body));
    }
}