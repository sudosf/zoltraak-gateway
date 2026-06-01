package com.zoltraak.gateway.domain.models.ollama;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OllamaChatResponse(
        String model,
        String createdAt,
        OllamaMessage message,
        boolean done,
        String doneReason,
        Long totalDuration,
        Long loadDuration,
        Integer promptEvalCount,
        Long promptEvalDuration,
        Integer evalCount,
        Long evalDuration
) {
}