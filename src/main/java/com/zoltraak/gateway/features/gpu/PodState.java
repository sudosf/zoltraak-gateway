package com.zoltraak.gateway.features.gpu;

import com.zoltraak.gateway.domain.enums.GpuProvider;
import com.zoltraak.gateway.domain.enums.PodStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PodState {
    private PodStatus status;
    private GpuProvider provider;
    private LocalDateTime lastActivityAt;
    private LocalDateTime sessionStartedAt;
    private String podId;
}
