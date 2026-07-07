package com.zoltraak.gateway.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zoltraak.security")
public class SecurityProperties {
    private String apiKey;
}
