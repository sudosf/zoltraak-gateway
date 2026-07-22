package com.zoltraak.gateway.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zoltraak.ollama")
public class OllamaProperties {

    private GpuPodConfig gpuPod;
    private LocalConfig local;

    @Data
    public static class GpuPodConfig {
        private Integer port;
    }

    @Data
    public static class LocalConfig {
        private String url;
        private Integer port;
    }
}
