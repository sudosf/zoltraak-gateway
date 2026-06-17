package com.zoltraak.gateway.features.gpu;

import com.zoltraak.gateway.adapters.gpu.GpuProvider;
import com.zoltraak.gateway.config.properties.ProviderProperties;
import com.zoltraak.gateway.domain.enums.GpuProviderType;
import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.exception.ExceptionUtils;
import com.zoltraak.gateway.features.gpu.model.ProviderRequest;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
public class GpuLifecycleManager {

    private final GpuProvider gpuProvider;
    private final PodState podState;
    private final RequestQueue requestQueue;
    private final ProviderProperties providerProperties;

    public GpuLifecycleManager(GpuProvider gpuProvider, ProviderProperties providerProperties, RequestQueue requestQueue) {
        this.gpuProvider = gpuProvider;
        this.podState = new PodState();
        this.podState.setStatus(PodStatus.STOPPED);
        this.requestQueue = requestQueue;
        this.providerProperties = providerProperties;
    }

    @PostConstruct
    void init() {
        gpuProvider.getStatus()
                .doOnSuccess(status -> {
                    log.info("GPU pod startup status: {}", status);
                    onExternalStateDrift(status);
                })
                .doOnError(ex -> log.warn(
                        "GPU pod could not sync status on startup, defaulting to status = {}, message = {}",
                        PodStatus.STOPPED, ExceptionUtils.getRootCauseMessage(ex)))
                .onErrorComplete()
                .subscribe();
    }

    public Mono<Void> requestStart() {
        PodStatus current = podState.getStatus();
        if (current == PodStatus.STARTING
                || current == PodStatus.READY
                || current == PodStatus.WARMING
                || current == PodStatus.DEGRADED) {
            log.debug("GPU pod already {}, ignoring start request", current);
            return Mono.empty();
        }

        log.info("GPU pod start requested, status = {} -> {}", current, PodStatus.STARTING);
        podState.setLastActivityAt(LocalDateTime.now());
        podState.setStatus(PodStatus.STARTING);

        return gpuProvider.start()
                .doOnSuccess(_ -> {
                    podState.setStatus(PodStatus.WARMING);
                    podState.setSessionStartedAt(LocalDateTime.now());
                    log.info("GPU pod started, status = {}", PodStatus.WARMING);
                })
                .doOnError(ex -> {
                    log.debug("GPU pod start failed, rolling back status = {}, message = {}", current, ExceptionUtils.getRootCauseMessage(ex));
                    podState.setStatus(current);
                    requestQueue.onPodStartFailed(current);
                });
    }

    public Mono<Void> requestShutdown() {
        PodStatus current = podState.getStatus();
        if (current == PodStatus.STOPPED || current == PodStatus.STOPPING) {
            log.debug("GPU pod already {}, ignoring shutdown request", current);
            return Mono.empty();
        }

        if (!requestQueue.isEmpty()) {
            log.warn("GPU Pod shutdown rejected, request queue is not empty.");
            return Mono.empty();
        }

        podState.setStatus(PodStatus.STOPPING);
        return gpuProvider.stop()
                .doOnSuccess(_ -> {
                    podState.setStatus(PodStatus.STOPPED);
                    log.info("GPU pod stopped, status = {}", PodStatus.STOPPED);
                })
                .doOnError(ex -> {
                    log.warn("GPU pod shutdown failed, rolling back status = {}, message = {}",
                            current, ExceptionUtils.getRootCauseMessage(ex)
                    );
                    podState.setStatus(current);
                    podState.setLastActivityAt(LocalDateTime.now());
                });
    }

    public void switchProvider(ProviderRequest request) {
        GpuProviderType currentProvider = this.providerProperties.getActive();
        log.info("GPU provider switch requested, from {} to {}", currentProvider, request.provider());
        this.providerProperties.setActive(request.provider());
    }

    public void onPodReady() {
        podState.setStatus(PodStatus.READY);
        requestQueue.onPodReady();
    }

    public void onPodDegraded() {
        podState.setStatus(PodStatus.DEGRADED);
        requestQueue.onPodDegraded();
    }

    public void onExternalStateDrift(PodStatus externalStatus) {
        PodStatus currentStatus = podState.getStatus();
        if (currentStatus == externalStatus) return;
        if (currentStatus == PodStatus.STOPPING) return;
        if (currentStatus == PodStatus.READY && externalStatus == PodStatus.WARMING) return;

        log.warn("GPU pod external state drift detected, external = {}, current = {}", externalStatus, currentStatus);

        if (externalStatus == PodStatus.WARMING || externalStatus == PodStatus.STARTING) {
            podState.setStatus(externalStatus);
            podState.setSessionStartedAt(LocalDateTime.now());
            podState.setLastActivityAt(LocalDateTime.now());
        } else {
            podState.setStatus(externalStatus);
        }

        log.info("GPU pod status updated to {}", externalStatus);
    }

    public PodStatus getStatus() {
        return podState.getStatus();
    }


    public LocalDateTime getLastActivityAt() {
        return podState.getLastActivityAt();
    }

    public LocalDateTime getSessionStartedAt() {
        return podState.getSessionStartedAt();
    }

    public void resetIdleTimer() {
        log.debug("GPU pod resetting idle timer");
        podState.setLastActivityAt(LocalDateTime.now());
    }
}
