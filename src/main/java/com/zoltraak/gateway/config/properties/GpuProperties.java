package com.zoltraak.gateway.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zoltraak.gpu")
public class GpuProperties {
    private Integer idleCheckTimeoutMinutes;
    private Integer idleCheckIntervalSeconds;
    private Integer warmupTimeoutMinutes;
    private Integer warmupPollIntervalSeconds;
    private Integer warmupPollInitialDelaySeconds;
    private Integer reconcilerPollIntervalSeconds;
    private Integer reconcilerPollInitialDelaySeconds;
}
