package com.zoltraak.gateway.adapters.ollama.model;

import java.util.List;

public record OllamaTagsResponse(
        List<OllamaModel> models
) {
}
