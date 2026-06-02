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
        return postAsFlux("/api/generate", request, OllamaGenerateResponse.class);
    }

    @Override
    public Flux<OllamaChatResponse> chat(OllamaChatRequest request) {
        return postAsFlux("/api/chat", request, OllamaChatResponse.class);
    }

    @Override
    public Mono<OllamaModelsResponse> getTags() {
        return getAsMono("/api/tags", OllamaModelsResponse.class);
    }

    @Override
    public Mono<OllamaModelsResponse> getPs() {
        return getAsMono("/api/ps", OllamaModelsResponse.class);
    }

    @Override
    public Mono<OllamaVersionResponse> getVersion() {
        return getAsMono("/api/version", OllamaVersionResponse.class);
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
                                .onErrorReturn(false)
                );
    }

    private <T, V> Flux<T> postAsFlux(String path, V body, Class<T> responseType) {
        return gpuProviderPort
                .getConnectionDetails()
                .flatMapMany(connDetails ->
                        webClient.post()
                                .uri(connDetails.ollamaUrl() + path)
                                .bodyValue(body)
                                .retrieve()
                                .bodyToFlux(responseType)
                );
    }

    private <T> Mono<T> getAsMono(String path, Class<T> responseType) {
        return gpuProviderPort
                .getConnectionDetails()
                .flatMap(connDetails ->
                        webClient.get()
                                .uri(connDetails.ollamaUrl() + path)
                                .retrieve()
                                .bodyToMono(responseType)
                );
    }
}
