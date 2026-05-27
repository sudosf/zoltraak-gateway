package com.zoltraak.gateway.domain.models.ollama;

import java.util.List;

public record OllamaGenerateRequest(
        String model,
        String prompt,
        List<String> images,
        boolean stream,
        Boolean think,
        String system
) {
}
