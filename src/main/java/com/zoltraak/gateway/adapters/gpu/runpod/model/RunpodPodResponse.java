package com.zoltraak.gateway.adapters.gpu.runpod.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RunpodPodResponse(
        String id,
        String desiredStatus,
        String name,
        String networkVolumeId,
        List<String> ports,
        String volumeMountPath,
        Machine machine,
        NetworkVolume networkVolume,
        Double costPerHr
) {
}