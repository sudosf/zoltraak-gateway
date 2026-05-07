package com.zoltraak.gateway.adapters.gpu.model;

import java.util.List;

public record VastAiInstanceResponse(
        Long id,
        String actualStatus,
        String publicIpaddr,
        List<Integer> ports
) {
}
