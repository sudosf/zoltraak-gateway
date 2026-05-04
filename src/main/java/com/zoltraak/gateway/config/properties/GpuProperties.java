package com.zoltraak.gateway.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zoltraak.gpu")
@Data
public class GpuProperties {
    private Integer idleTimeoutMinutes;
    private Integer warmupTimeoutSeconds;
    private Integer warmupPollIntervalSeconds;
}
