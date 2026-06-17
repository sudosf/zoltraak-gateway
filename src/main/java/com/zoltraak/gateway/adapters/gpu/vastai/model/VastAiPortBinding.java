package com.zoltraak.gateway.adapters.gpu.vastai.model;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public record VastAiPortBinding(
        String hostIp,
        String hostPort
) {
}
