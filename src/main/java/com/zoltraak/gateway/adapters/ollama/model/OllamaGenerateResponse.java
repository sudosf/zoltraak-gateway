package com.zoltraak.gateway.adapters.ollama.model;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OllamaGenerateResponse(
        String model,
        String createdAt,
        String response,
        String thinking,
        boolean done,
        String doneReason
) {
}
