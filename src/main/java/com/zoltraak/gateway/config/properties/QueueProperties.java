package com.zoltraak.gateway.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zoltraak.queue")
@Data
public class QueueProperties {
    private Integer maxWaitSeconds;
    private Integer expiryCheckIntervalSeconds;
}
