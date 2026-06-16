package com.zoltraak.gateway.features.gpu.process;

import com.zoltraak.gateway.adapters.gpu.GpuProvider;
import com.zoltraak.gateway.adapters.gpu.ProviderException;
import com.zoltraak.gateway.annotations.BackgroundProcess;
import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.exception.ExceptionUtils;
import com.zoltraak.gateway.features.gpu.GpuLifecycleManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@BackgroundProcess
public class PodStateReconciler {

    private final GpuProvider gpuProvider;
    private final GpuLifecycleManager gpuLifecycleManager;

    public PodStateReconciler(GpuProvider gpuProvider, GpuLifecycleManager gpuLifecycleManager) {
        this.gpuProvider = gpuProvider;
        this.gpuLifecycleManager = gpuLifecycleManager;
    }

    @Scheduled(fixedDelayString = "${zoltraak.gpu.reconciler-poll-interval-seconds}s",
            initialDelayString = "${zoltraak.gpu.reconciler-poll-initial-delay-seconds}s")
    void reconcile() {
        log.debug("Reconciling GPU pod state");

        gpuProvider
                .getStatus()
                .subscribe(
                        gpuLifecycleManager::onExternalStateDrift,
                        ex -> {
                            if (ex instanceof ProviderException pe && pe.getHttpStatusCode() == 404) {
                                log.debug("No active GPU instance on {}, treating as stopped", pe.getProvider());
                                gpuLifecycleManager.onExternalStateDrift(PodStatus.STOPPED);
                            } else {
                                log.error("Failed to reconcile GPU pod state, message = {}",
                                        ExceptionUtils.getRootCauseMessage(ex));
                            }
                        }
                );
    }
}
