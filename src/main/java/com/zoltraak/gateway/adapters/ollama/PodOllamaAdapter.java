package com.zoltraak.gateway.adapters.ollama;

import com.zoltraak.gateway.adapters.gpu.GpuProvider;
import com.zoltraak.gateway.annotations.Adapter;
import com.zoltraak.gateway.exception.ExceptionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

@Slf4j
@Adapter
@Primary
public class PodOllamaAdapter implements OllamaPort {
    private final GpuProvider gpuProvider;
    private final WebClient webClient;

    public PodOllamaAdapter(GpuProvider gpuProvider, @Qualifier("ollamaWebClient") WebClient webClient) {
        this.gpuProvider = gpuProvider;
        this.webClient = webClient;
    }

    @Override
    public Flux<byte[]> generate(Flux<byte[]> request, HttpHeaders headers) {
        return postAsFlux(new RequestDetails("/api/generate", request, headers));
    }

    @Override
    public Flux<byte[]> chat(Flux<byte[]> request, HttpHeaders headers) {
        return postAsFlux(new RequestDetails("/v1/chat/completions", request, headers));
    }

    @Override
    public Mono<byte[]> embed(Flux<byte[]> request, HttpHeaders headers) {
        return postAsMono(new RequestDetails("/api/embed", request, headers));
    }

    @Override
    public Mono<byte[]> show(Flux<byte[]> request, HttpHeaders headers) {
        return postAsMono(new RequestDetails("/api/show", request, headers));
    }

    @Override
    public Mono<byte[]> getTags(HttpHeaders headers) {
        return getAsMono(new RequestDetails("/api/tags", null, headers));
    }

    @Override
    public Mono<byte[]> getPs(HttpHeaders headers) {
        return getAsMono(new RequestDetails("/api/ps", null, headers));
    }

    @Override
    public Mono<byte[]> getVersion(HttpHeaders headers) {
        return getAsMono(new RequestDetails("/api/version", null, headers));
    }

    @Override
    public Mono<Boolean> isHealthy() {
        return gpuProvider
                .getConnectionDetails()
                .flatMap(connDetails -> webClient.get()
                        .uri(connDetails.ollamaUrl())
                        .retrieve()
                        .toBodilessEntity()
                        .map(response -> response.getStatusCode().is2xxSuccessful())
                        .onErrorReturn(false)
                );
    }

    private Flux<byte[]> postAsFlux(RequestDetails requestDetails) {
        return gpuProvider
                .getConnectionDetails()
                .flatMapMany(connDetails ->
                        webClient.post()
                                .uri(connDetails.ollamaUrl() + requestDetails.path())
                                .headers(forwardableHeaders(requestDetails.headers()))
                                .body(requestDetails.body(), byte[].class)
                                .retrieve()
                                .bodyToFlux(byte[].class)
                                .doOnSubscribe(_ -> log.debug("Ollama POST {}", requestDetails.path()))
                                .doOnError(ex -> log.debug("Ollama POST {} failed: {}",
                                        requestDetails.path(), ExceptionUtils.getRootCauseMessage(ex))
                                ));
    }

    private Mono<byte[]> postAsMono(RequestDetails requestDetails) {
        return gpuProvider
                .getConnectionDetails()
                .flatMap(connDetails ->
                        webClient.post()
                                .uri(connDetails.ollamaUrl() + requestDetails.path())
                                .headers(forwardableHeaders(requestDetails.headers()))
                                .body(requestDetails.body(), byte[].class)
                                .retrieve()
                                .bodyToMono(byte[].class))
                .doOnSubscribe(_ -> log.debug("Ollama POST {}", requestDetails.path()))
                .doOnError(ex -> log.debug("Ollama POST {} failed: {}",
                        requestDetails.path(), ExceptionUtils.getRootCauseMessage(ex))
                );
    }

    private Mono<byte[]> getAsMono(RequestDetails requestDetails) {
        return gpuProvider
                .getConnectionDetails()
                .flatMap(connDetails ->
                        webClient.get()
                                .uri(connDetails.ollamaUrl() + requestDetails.path())
                                .headers(forwardableHeaders(requestDetails.headers()))
                                .retrieve()
                                .bodyToMono(byte[].class))
                .doOnSubscribe(_ -> log.debug("Ollama GET {}", requestDetails.path()))
                .doOnError(ex -> log.debug("Ollama GET {} failed: {}",
                        requestDetails.path(), ExceptionUtils.getRootCauseMessage(ex))
                );
    }

    private Consumer<HttpHeaders> forwardableHeaders(HttpHeaders requestHeaders) {
        return headers -> {
            headers.addAll(requestHeaders);
            headers.remove(HttpHeaders.HOST);
            headers.remove(HttpHeaders.ORIGIN);
        };
    }

    private record RequestDetails(String path, Flux<byte[]> body, HttpHeaders headers) {
    }
}
