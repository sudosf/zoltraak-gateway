package com.zoltraak.gateway.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zoltraak.provider")
@Data
public class ProviderProperties {
    private String active;
    private RunPodConfig runPod;
    private VastAiConfig vastAi;

    @Data
    public static class RunPodConfig {
        private String apiKey;
        private String podId;
        private String baseUrl;
    }

    @Data
    public static class VastAiConfig {
        private String apiKey;
        private String instanceId;
        private String baseUrl;
    }
}
