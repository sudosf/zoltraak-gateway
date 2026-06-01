package com.zoltraak.gateway.adapters.ollama;

import com.zoltraak.gateway.domain.models.ollama.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OllamaPort {
    Flux<OllamaGenerateResponse> generate(OllamaGenerateRequest request);

    Flux<OllamaChatResponse> chat(OllamaChatRequest request);

    Mono<OllamaModelsResponse> getTags();

    Mono<OllamaModelsResponse> getPs();

    Mono<OllamaVersionResponse> getVersion();

    Mono<Boolean> isHealthy();
}
