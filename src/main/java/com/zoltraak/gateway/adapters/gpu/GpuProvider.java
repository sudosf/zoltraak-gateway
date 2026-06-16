package com.zoltraak.gateway.adapters.gpu;

import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.domain.models.provider.PodConnectionDetails;
import reactor.core.publisher.Mono;

public interface GpuProvider {
    Mono<Void> start();

    Mono<Void> stop();

    Mono<PodStatus> getStatus();

    Mono<PodConnectionDetails> getConnectionDetails();
}