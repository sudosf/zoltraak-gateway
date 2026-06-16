package com.zoltraak.gateway.adapters.gpu.vastai.model;

import java.util.List;

public record VastAiInstancePage(
        List<VastAiInstance> instances
) {
}
