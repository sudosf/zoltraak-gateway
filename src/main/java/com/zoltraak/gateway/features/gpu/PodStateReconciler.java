package com.zoltraak.gateway.features.gpu;

import com.zoltraak.gateway.adapters.gpu.GpuProviderPort;
import com.zoltraak.gateway.adapters.gpu.ProviderException;
import com.zoltraak.gateway.annotations.BackgroundProcess;
import com.zoltraak.gateway.domain.enums.PodStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@BackgroundProcess
public class PodStateReconciler {

    private final GpuProviderPort gpuProviderPort;
    private final GpuLifecycleManager gpuLifecycleManager;

    public PodStateReconciler(GpuProviderPort gpuProviderPort, GpuLifecycleManager gpuLifecycleManager) {
        this.gpuProviderPort = gpuProviderPort;
        this.gpuLifecycleManager = gpuLifecycleManager;
    }

    @Scheduled(fixedDelayString = "${zoltraak.gpu.reconciler-poll-interval-seconds}s",
            initialDelayString = "${zoltraak.gpu.reconciler-poll-initial-delay-seconds}s")
    void reconcile() {
        log.debug("Reconciling GPU pod state");

        gpuProviderPort
                .getStatus()
                .subscribe(
                        gpuLifecycleManager::onExternalStateDrift,
                        error -> {
                            if (error instanceof ProviderException pe && pe.getHttpStatusCode() == 404) {
                                log.debug("No active GPU instance on Vast.ai, treating as stopped");
                                gpuLifecycleManager.onExternalStateDrift(PodStatus.STOPPED);
                            } else {
                                log.error("Failed to reconcile GPU pod state", error);
                            }
                        }
                );
    }
}
