package com.zoltraak.gateway.adapters.ollama;

import com.zoltraak.gateway.adapters.gpu.GpuProviderPort;
import com.zoltraak.gateway.annotations.Adapter;
import com.zoltraak.gateway.domain.models.ollama.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Adapter
@Primary
public class PodOllamaAdapter implements OllamaPort {
    private final GpuProviderPort gpuProviderPort;
    private final WebClient webClient;

    public PodOllamaAdapter(GpuProviderPort gpuProviderPort, @Qualifier("ollamaWebClient") WebClient webClient) {
        this.gpuProviderPort = gpuProviderPort;
        this.webClient = webClient;
    }

    @Override
    public Mono<OllamaGenerateResponse> generate(OllamaGenerateRequest request) {
        return null;
    }

    @Override
    public Mono<OllamaChatResponse> chat(OllamaChatRequest request) {
        return null;
    }

    @Override
    public Mono<OllamaTagsResponse> getTags() {
        return gpuProviderPort
                .getConnectionDetails()
                .flatMap(connDetails ->
                        webClient.get()
                                .uri(connDetails.ollamaUrl() + "/api/tags")
                                .retrieve()
                                .bodyToMono(OllamaTagsResponse.class)
                );
    }


    @Override
    public Mono<Boolean> isHealthy() {
        return gpuProviderPort
                .getConnectionDetails()
                .flatMap(connDetails ->
                        webClient.get()
                                .uri(connDetails.ollamaUrl())
                                .retrieve()
                                .toBodilessEntity()
                                .map(response -> response.getStatusCode().is2xxSuccessful())
                );
    }
}
