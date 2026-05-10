package com.zoltraak.gateway.adapters.ollama.model;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OllamaChatResponse(
        String model,
        String createdAt,
        OllamaMessage message,
        boolean done,
        String doneReason
) {
}
