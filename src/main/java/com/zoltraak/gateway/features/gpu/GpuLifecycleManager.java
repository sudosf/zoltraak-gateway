package com.zoltraak.gateway.features.gpu;

import com.zoltraak.gateway.adapters.gpu.GpuProviderPort;
import com.zoltraak.gateway.config.properties.ProviderProperties;
import com.zoltraak.gateway.domain.enums.PodStatus;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
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

    @PostConstruct
    void init() {
        gpuProviderPort.getStatus()
                .doOnSuccess(status -> {
                    podState.setStatus(status);
                    if (status != PodStatus.STOPPED) {
                        podState.setLastActivityAt(LocalDateTime.now());
                    }
                })
                .doOnError(e -> log.warn("GPU pod could not sync status on startup, defaulting to STOPPED: {}", e.getMessage()))
                .onErrorComplete()
                .subscribe();
    }

    public Mono<Void> requestStart() {
        PodStatus current = podState.getStatus();
        if (current == PodStatus.STARTING
                || current == PodStatus.READY
                || current == PodStatus.WARMING) {
            log.debug("GPU pod already {}, ignoring start request", current);
            return Mono.empty();
        }

        log.info("GPU pod start requested, status={} -> {}", current, PodStatus.STARTING);
        podState.setLastActivityAt(LocalDateTime.now());
        podState.setStatus(PodStatus.STARTING);

        return gpuProviderPort.start()
                .doOnSuccess(_ -> {
                    podState.setStatus(PodStatus.WARMING);
                    podState.setSessionStartedAt(LocalDateTime.now());
                    log.info("GPU pod started, status={}", PodStatus.WARMING);
                })
                .doOnError(e -> {
                    log.warn("GPU pod start failed, rolling back status={}", current, e);
                    podState.setStatus(current);
                });
    }

    public Mono<Void> requestShutdown() {
        PodStatus current = podState.getStatus();
        if (current == PodStatus.STOPPED || current == PodStatus.STOPPING) {
            log.debug("GPU pod already {}, ignoring shutdown request", current);
            return Mono.empty();
        }
        // TODO: check in-flight count via RequestQueue

        podState.setStatus(PodStatus.STOPPING);
        return gpuProviderPort.stop()
                .doOnSuccess(_ -> {
                    podState.setStatus(PodStatus.STOPPED);
                    log.info("GPU pod stopped, status={}", PodStatus.STOPPED);
                })
                .doOnError(e -> {
                    log.warn("GPU pod shutdown failed, rolling back status={}", current, e);
                    podState.setStatus(current);
                    podState.setLastActivityAt(LocalDateTime.now());
                });
    }

    public PodStatus getStatus() {
        return podState.getStatus();
    }

    public void setStatus(PodStatus status) {
        podState.setStatus(status);
    }

    public LocalDateTime getLastActivityAt() {
        return podState.getLastActivityAt();
    }

    public LocalDateTime getSessionStartedAt() {
        return podState.getSessionStartedAt();
    }

    public void resetIdleTimer() {
        podState.setLastActivityAt(LocalDateTime.now());
    }
}
