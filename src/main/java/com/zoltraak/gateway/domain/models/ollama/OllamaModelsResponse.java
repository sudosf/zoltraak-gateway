package com.zoltraak.gateway.domain.models.ollama;

import java.util.List;

public record OllamaModelsResponse(
        List<OllamaModel> models
) {
}
