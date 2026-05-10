package com.zoltraak.gateway.adapters.ollama.model;

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
