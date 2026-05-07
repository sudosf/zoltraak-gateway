package com.zoltraak.gateway.adapters.gpu;

import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.domain.shared.PodConnectionDetails;

public interface GpuProviderPort {
    void start();

    void stop();

    PodStatus getStatus();

    PodConnectionDetails getConnectionDetails();
}
