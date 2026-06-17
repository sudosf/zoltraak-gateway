package com.zoltraak.gateway.features.gpu;

import com.zoltraak.gateway.domain.enums.GpuProviderType;
import com.zoltraak.gateway.domain.enums.PodStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PodState {
    private PodStatus status;
    private GpuProviderType provider;
    private LocalDateTime lastActivityAt;
    private LocalDateTime sessionStartedAt;
    private String podId;
}
