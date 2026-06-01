package com.zoltraak.gateway.adapters.ollama;

import com.zoltraak.gateway.adapters.gpu.GpuProviderPort;
import com.zoltraak.gateway.annotations.Adapter;
import com.zoltraak.gateway.domain.models.ollama.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
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
    public Flux<OllamaGenerateResponse> generate(OllamaGenerateRequest request) {
        return gpuProviderPort
                .getConnectionDetails()
                .flatMapMany(connDetails ->
                        webClient.post()
                                .uri(connDetails.ollamaUrl() + "/api/generate")
                                .bodyValue(request)
                                .retrieve()
                                .bodyToFlux(OllamaGenerateResponse.class)
                );
    }

    @Override
    public Flux<OllamaChatResponse> chat(OllamaChatRequest request) {
        return gpuProviderPort
                .getConnectionDetails()
                .flatMapMany(connDetails ->
                        webClient.post()
                                .uri(connDetails.ollamaUrl() + "/api/chat")
                                .bodyValue(request)
                                .retrieve()
                                .bodyToFlux(OllamaChatResponse.class)
                );
    }

    @Override
    public Mono<OllamaModelsResponse> getTags() {
        return gpuProviderPort
                .getConnectionDetails()
                .flatMap(connDetails ->
                        webClient.get()
                                .uri(connDetails.ollamaUrl() + "/api/tags")
                                .retrieve()
                                .bodyToMono(OllamaModelsResponse.class)
                );
    }

    @Override
    public Mono<OllamaModelsResponse> getPs() {
        return gpuProviderPort
                .getConnectionDetails()
                .flatMap(connDetails ->
                        webClient.get()
                                .uri(connDetails.ollamaUrl() + "/api/ps")
                                .retrieve()
                                .bodyToMono(OllamaModelsResponse.class)
                );
    }

    @Override
    public Mono<OllamaVersionResponse> getVersion() {
        return gpuProviderPort
                .getConnectionDetails()
                .flatMap(connDetails ->
                        webClient.get()
                                .uri(connDetails.ollamaUrl() + "/api/version")
                                .retrieve()
                                .bodyToMono(OllamaVersionResponse.class)
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
