package com.zoltraak.gateway.domain.models.ollama;

import java.util.List;

public record OllamaChatRequest(
        String model,
        List<OllamaMessage> messages,
        Boolean stream,
        Boolean think
) {
}
