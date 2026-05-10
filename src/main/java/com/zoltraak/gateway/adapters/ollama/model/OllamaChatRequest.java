package com.zoltraak.gateway.adapters.ollama.model;

import java.util.List;

public record OllamaChatRequest(
        String model,
        List<OllamaMessage> messages,
        boolean stream,
        boolean think
) {
}
