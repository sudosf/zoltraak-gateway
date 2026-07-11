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
    private RunPod runpod;
    private VastAi vastAi;

    @Data
    public static class RunPod {
        private String apiKey;
        private String baseUrl;
        private Create create = new Create();

        @Data
        public static class Create {
            private List<String> gpuTypeIds;
            private List<String> ports;
            private String imageName;
            private String name;
            private Integer gpuCount;
            private Integer volumeInGb;
            private Integer containerDiskInGb;
            private String volumeMountPath;
            private Boolean interruptible;
            private Boolean supportPublicIp;
            private Boolean globalNetworking;
            private String templateId;
            private String networkVolumeId;
            private List<String> dataCenterIds;
            private List<String> countryCodes;
            private String gpuTypePriority;
            private String dataCenterPriority;
            private Integer minVCPUPerGPU;
            private Integer minRAMPerGPU;
            private Integer vcpuCount;
            private Integer minDiskBandwidthMBps;
            private Integer minDownloadMbps;
            private Integer minUploadMbps;
        }
    }

    @Data
    public static class VastAi {
        private String apiKey;
        private String baseUrl;
    }
}
