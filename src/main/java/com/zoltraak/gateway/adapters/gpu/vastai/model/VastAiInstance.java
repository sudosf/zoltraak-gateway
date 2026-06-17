package com.zoltraak.gateway.adapters.gpu.vastai.model;

import lombok.Data;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VastAiInstance {
    private Long id;
    private String actualStatus;
    private String publicIpaddr;
    private Map<String, List<VastAiPortBinding>> ports;
}
