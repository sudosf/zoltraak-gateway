package com.zoltraak.gateway.domain.models.ollama;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OllamaModel(
        String name,
        String model,
        String modifiedAt,
        Long size,
        String digest,
        OllamaModelDetails details
) {
}
