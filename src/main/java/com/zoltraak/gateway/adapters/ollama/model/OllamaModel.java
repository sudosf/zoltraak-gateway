package com.zoltraak.gateway.adapters.ollama.model;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OllamaModel(
        String name,
        String model,
        String modifiedAt,
        Long size,
        String digest
) {
}
