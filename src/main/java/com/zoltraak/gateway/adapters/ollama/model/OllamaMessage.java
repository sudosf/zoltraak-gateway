package com.zoltraak.gateway.adapters.ollama.model;

import java.util.List;

public record OllamaMessage(
        String role,
        String content,
        String thinking,
        List<String> images
) {
}
