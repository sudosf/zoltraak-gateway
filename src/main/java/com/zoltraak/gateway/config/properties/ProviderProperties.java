package com.zoltraak.gateway.config.properties;

import com.zoltraak.gateway.domain.enums.GpuProviderType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

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
        private CreateProperties create;

        @Data
        public static class CreateProperties {
            private Integer containerDiskInGb;
            private String cloudType;
            private String computeType;
            private List<String> gpuTypeIds;
            private Integer gpuCount;
            private String gpuTypePriority;
            private String imageName;
            private String name;
            private String networkVolumeId;
            private List<String> ports;
            private String volumeMountPath;
        }
    }

    @Data
    public static class VastAiConfig {
        private String apiKey;
        private String baseUrl;
    }
}
