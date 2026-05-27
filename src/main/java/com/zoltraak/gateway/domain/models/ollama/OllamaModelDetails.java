package com.zoltraak.gateway.domain.models.ollama;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OllamaModelDetails(
        String format,
        String family,
        List<String> families,
        String parameterSize,
        String parentModel,
        String quantizationLevel
) {
}
