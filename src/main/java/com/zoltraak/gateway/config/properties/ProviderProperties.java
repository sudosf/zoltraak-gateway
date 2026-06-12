package com.zoltraak.gateway.config.properties;

import com.zoltraak.gateway.domain.enums.GpuProvider;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zoltraak.provider")
public class ProviderProperties {
    private GpuProvider active;
    private Integer timeoutSeconds;
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
        private String baseUrl;
    }
}
