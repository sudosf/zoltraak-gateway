package com.zoltraak.gateway.adapters.ollama;

import com.zoltraak.gateway.adapters.gpu.GpuProvider;
import com.zoltraak.gateway.annotations.Adapter;
import com.zoltraak.gateway.exception.ExceptionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

@Slf4j
@Adapter
@Primary
public class PodOllamaAdapter implements OllamaClient {
    private static final String GENERATE = "/api/generate";
    private static final String CHAT = "/v1/chat/completions";
    private static final String EMBED = "/api/embed";
    private static final String SHOW = "/api/show";
    private static final String VERSION = "/api/version";
    private static final String PS = "/api/ps";
    private static final String TAGS = "/api/tags";

    private final GpuProvider gpuProvider;
    private final WebClient webClient;

    public PodOllamaAdapter(GpuProvider gpuProvider, @Qualifier("ollamaWebClient") WebClient webClient) {
        this.gpuProvider = gpuProvider;
        this.webClient = webClient;
    }

    @Override
    public Flux<byte[]> generate(Flux<byte[]> request, HttpHeaders headers) {
        return postAsFlux(new RequestDetails(GENERATE, request, headers));
    }

    @Override
    public Flux<byte[]> chat(Flux<byte[]> request, HttpHeaders headers) {
        return postAsFlux(new RequestDetails(CHAT, request, headers));
    }

    @Override
    public Mono<byte[]> embed(Flux<byte[]> request, HttpHeaders headers) {
        return postAsMono(new RequestDetails(EMBED, request, headers));
    }

    @Override
    public Mono<byte[]> show(Flux<byte[]> request, HttpHeaders headers) {
        return postAsMono(new RequestDetails(SHOW, request, headers));
    }

    @Override
    public Mono<byte[]> getTags(HttpHeaders headers) {
        return getAsMono(new RequestDetails(TAGS, Flux.empty(), headers));
    }

    @Override
    public Mono<byte[]> getPs(HttpHeaders headers) {
        return getAsMono(new RequestDetails(PS, Flux.empty(), headers));
    }

    @Override
    public Mono<byte[]> getVersion(HttpHeaders headers) {
        return getAsMono(new RequestDetails(VERSION, Flux.empty(), headers));
    }

    @Override
    public Mono<Boolean> isHealthy() {
        return gpuProvider
                .getConnectionDetails()
                .flatMap(connDetails -> webClient.get()
                        .uri(connDetails.ollamaUrl())
                        .retrieve()
                        .toBodilessEntity()
                        .onErrorMap(WebClientResponseException.class,
                                ex -> new OllamaException(
                                        ex.getStatusCode().value(),
                                        ex.getResponseBodyAsByteArray()
                                ))
                        .map(response -> response.getStatusCode().is2xxSuccessful())
                        .onErrorReturn(false)
                );
    }

    private Mono<byte[]> postAsMono(RequestDetails requestDetails) {
        return fetchAsMono(HttpMethod.POST, requestDetails);
    }

    private Mono<byte[]> getAsMono(RequestDetails requestDetails) {
        return fetchAsMono(HttpMethod.GET, requestDetails);
    }

    private Flux<byte[]> postAsFlux(RequestDetails requestDetails) {
        Consumer<HttpHeaders> headers = forwardableHeaders(requestDetails.headers());
        LogContext logContext = new LogContext(HttpMethod.POST, requestDetails.path());

        return gpuProvider
                .getConnectionDetails()
                .flatMapMany(connDetails ->
                        webClient.post()
                                .uri(connDetails.ollamaUrl() + requestDetails.path())
                                .headers(headers)
                                .body(requestDetails.body(), byte[].class)
                                .retrieve()
                                .bodyToFlux(byte[].class)
                                .onErrorMap(WebClientResponseException.class,
                                        ex -> new OllamaException(
                                                ex.getStatusCode().value(),
                                                ex.getResponseBodyAsByteArray())
                                ))
                .doOnSubscribe(_ -> logRequest(logContext))
                .doOnError(ex -> logFailure(logContext, ex));
    }

    private Mono<byte[]> fetchAsMono(HttpMethod method, RequestDetails requestDetails) {
        Consumer<HttpHeaders> headers = forwardableHeaders(requestDetails.headers());
        LogContext logContext = new LogContext(method, requestDetails.path());

        return gpuProvider
                .getConnectionDetails()
                .flatMap(connDetails ->
                        webClient.method(method)
                                .uri(connDetails.ollamaUrl() + requestDetails.path())
                                .headers(headers)
                                .body(requestDetails.body(), byte[].class)
                                .retrieve()
                                .bodyToMono(byte[].class)
                                .onErrorMap(WebClientResponseException.class,
                                        ex -> new OllamaException(
                                                ex.getStatusCode().value(),
                                                ex.getResponseBodyAsByteArray())
                                ))
                .doOnSubscribe(_ -> logRequest(logContext))
                .doOnError(ex -> logFailure(logContext, ex));
    }

    private Consumer<HttpHeaders> forwardableHeaders(HttpHeaders requestHeaders) {
        return headers -> {
            headers.addAll(requestHeaders);
            headers.remove(HttpHeaders.HOST);
            headers.remove(HttpHeaders.ORIGIN);
        };
    }

    private void logRequest(LogContext logContext) {
        log.debug("Ollama {} {}", logContext.method(), logContext.path());
    }

    private void logFailure(LogContext logContext, Throwable ex) {
        log.debug("Ollama {} {} failed: {}",
                logContext.method(), logContext.path(), ExceptionUtils.getRootCauseMessage(ex));
    }

    private record RequestDetails(String path, Flux<byte[]> body, HttpHeaders headers) {
    }

    private record LogContext(HttpMethod method, String path) {
    }
}
