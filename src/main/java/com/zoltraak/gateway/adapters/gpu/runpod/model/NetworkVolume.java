package com.zoltraak.gateway.adapters.gpu.runpod.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NetworkVolume(
        String id,
        String name,
        int size
) {
}