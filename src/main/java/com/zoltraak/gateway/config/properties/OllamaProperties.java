package com.zoltraak.gateway.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zoltraak.ollama")
@Data
public class OllamaProperties {

    private GpuPodConfig gpuPod;
    private LocalConfig local;

    @Data
    public static class GpuPodConfig {
        private String modelVision;
        private Integer port;
    }

    @Data
    public static class LocalConfig {
        private String url;
        private Integer port;
        private Integer timeoutSeconds;
    }
}
