package com.zoltraak.gateway.adapters.gpu.runpod.model;

import java.util.Map;

public record RunpodPodResponse(
        String id,
        String desiredStatus,
        String publicIp,
        Map<String, Integer> portMappings
) {
}
