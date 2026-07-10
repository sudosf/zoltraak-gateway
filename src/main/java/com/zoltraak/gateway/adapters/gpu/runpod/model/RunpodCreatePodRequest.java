package com.zoltraak.gateway.adapters.gpu.runpod.model;

import java.util.List;

public record RunpodCreatePodRequest(
        String cloudType,
        String computeType,
        List<String> gpuTypeIds,
        Integer gpuCount,
        String gpuTypePriority,
        String imageName,
        String name,
        String networkVolumeId,
        List<String> ports,
        String volumeMountPath,
        Integer containerDiskInGb
) {
}