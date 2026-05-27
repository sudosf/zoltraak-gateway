package com.zoltraak.gateway.domain.models.ollama;

import java.util.List;

public record OllamaMessage(
        String role,
        String content,
        String thinking,
        List<String> images
) {
}
