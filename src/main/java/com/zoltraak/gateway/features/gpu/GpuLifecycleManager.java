package com.zoltraak.gateway.features.gpu;

import com.zoltraak.gateway.adapters.gpu.GpuProviderPort;
import com.zoltraak.gateway.config.properties.ProviderProperties;
import com.zoltraak.gateway.domain.enums.PodStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

// TODO add logging
@Service
public class GpuLifecycleManager {

    private final GpuProviderPort gpuProviderPort;
    private final PodState podState;

    public GpuLifecycleManager(GpuProviderPort gpuProviderPort, ProviderProperties providerProperties) {
        this.gpuProviderPort = gpuProviderPort;
        this.podState = new PodState();
        this.podState.setStatus(PodStatus.STOPPED);
        this.podState.setProvider(providerProperties.getActive());
    }

    public PodStatus getStatus() {
        return podState.getStatus();
    }

    public Mono<Void> requestStart() {
        PodStatus current = podState.getStatus();
        if (current == PodStatus.STARTING
                || current == PodStatus.READY
                || current == PodStatus.WARMING) {
            return Mono.empty();
        }

        podState.setLastActivityAt(LocalDateTime.now());
        podState.setStatus(PodStatus.STARTING);

        return gpuProviderPort.start()
                .doOnSuccess(_ -> {
                    podState.setStatus(PodStatus.WARMING);
                    podState.setSessionStartedAt(LocalDateTime.now());
                })
                .doOnError(_ -> podState.setStatus(current));
    }

    public Mono<Void> requestShutdown() {
        PodStatus current = podState.getStatus();
        if (current == PodStatus.STOPPED || current == PodStatus.STOPPING) {
            return Mono.empty();
        }
        // TODO: check in-flight count via RequestQueue

        podState.setStatus(PodStatus.STOPPING);
        return gpuProviderPort.stop()
                .doOnSuccess(_ -> podState.setStatus(PodStatus.STOPPED))
                .doOnError(_ -> {
                    podState.setStatus(current);
                    podState.setLastActivityAt(LocalDateTime.now());
                });
    }

    public void resetIdleTimer() {
        // TODO
    }
}
