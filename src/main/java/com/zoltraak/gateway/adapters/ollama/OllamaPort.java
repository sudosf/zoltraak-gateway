package com.zoltraak.gateway.adapters.ollama;

import com.zoltraak.gateway.domain.models.ollama.*;
import reactor.core.publisher.Mono;

public interface OllamaPort {
    Mono<OllamaGenerateResponse> generate(OllamaGenerateRequest request);

    Mono<OllamaChatResponse> chat(OllamaChatRequest request);

    Mono<OllamaModelsResponse> getTags();

    Mono<OllamaModelsResponse> getPs();

    Mono<OllamaVersionResponse> getVersion();

    Mono<Boolean> isHealthy();
}
