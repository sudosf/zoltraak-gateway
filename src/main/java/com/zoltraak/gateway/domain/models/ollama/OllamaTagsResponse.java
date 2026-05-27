package com.zoltraak.gateway.domain.models.ollama;

import java.util.List;

public record OllamaTagsResponse(
        List<OllamaModel> models
) {
}
