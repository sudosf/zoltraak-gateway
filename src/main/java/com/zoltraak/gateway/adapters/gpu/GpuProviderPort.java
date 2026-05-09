package com.zoltraak.gateway.adapters.gpu;

import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.domain.shared.PodConnectionDetails;
import reactor.core.publisher.Mono;

public interface GpuProviderPort {
    Mono<Void> start();

    Mono<Void> stop();

    Mono<PodStatus> getStatus();

    Mono<PodConnectionDetails> getConnectionDetails();
}