package com.zoltraak.gateway.adapters.gpu.model;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record VastAiInstanceResponse(
        Long id,
        String actualStatus,
        String publicIpaddr,
        Map<String, List<VastAiPortBinding>> ports
) {
}
