package com.zoltraak.gateway.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zoltraak.queue")
public class QueueProperties {
    private Integer maxWaitMinutes;
    private Integer expiryCheckIntervalSeconds;
}
