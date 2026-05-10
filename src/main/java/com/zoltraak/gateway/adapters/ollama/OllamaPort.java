package com.zoltraak.gateway.adapters.ollama;

import com.zoltraak.gateway.adapters.ollama.model.*;
import reactor.core.publisher.Mono;

public interface OllamaPort {
    Mono<OllamaGenerateResponse> generate(OllamaGenerateRequest request);

    Mono<OllamaChatResponse> chat(OllamaChatRequest request);

    Mono<OllamaTagsResponse> getTags();

    Mono<Boolean> isHealthy();
}
