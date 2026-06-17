package com.zoltraak.gateway.config.properties;

import com.zoltraak.gateway.domain.enums.GpuProviderType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zoltraak.provider")
public class ProviderProperties {
    private GpuProviderType active;
    private Integer responseTimeoutSeconds;
    private Integer idCacheHours;
    private RunPodConfig runpod;
    private VastAiConfig vastAi;

    @Data
    public static class RunPodConfig {
        private String apiKey;
        private String baseUrl;
    }

    @Data
    public static class VastAiConfig {
        private String apiKey;
        private String baseUrl;
    }
}
