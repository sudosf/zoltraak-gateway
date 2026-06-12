package com.zoltraak.gateway.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zoltraak.gpu")
public class GpuProperties {
    private Integer idleTimeoutMinutes;
    private Integer warmupTimeoutMinutes;
    private Integer warmupPollIntervalSeconds;
    private Integer idleCheckIntervalSeconds;
    private Integer reconcilerPollIntervalSeconds;
}
