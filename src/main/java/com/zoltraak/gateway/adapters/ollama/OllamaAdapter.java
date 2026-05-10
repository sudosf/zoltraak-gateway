package com.zoltraak.gateway.adapters.ollama;

import com.zoltraak.gateway.adapters.ollama.model.*;
import com.zoltraak.gateway.annotations.Adapter;
import reactor.core.publisher.Mono;

@Adapter
public class OllamaAdapter implements OllamaPort {
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
        return null;
    }

    @Override
    public Mono<Boolean> isHealthy() {
        return null;
    }
}
