package com.zoltraak.gateway.adapters.ollama;

import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface OllamaPort {
    Flux<byte[]> generate(Flux<byte[]> request, HttpHeaders headers);

    Flux<byte[]> chat(Flux<byte[]> request, HttpHeaders headers);

    Mono<byte[]> embed(Flux<byte[]> request, HttpHeaders headers);

    Mono<byte[]> show(Flux<byte[]> request, HttpHeaders headers);

    Mono<byte[]> getTags(HttpHeaders headers);

    Mono<byte[]> getPs(HttpHeaders headers);

    Mono<byte[]> getVersion(HttpHeaders headers);

    Mono<Boolean> isHealthy();
}
