package com.zoltraak.gateway.features.gpu;

import com.zoltraak.gateway.annotations.BackgroundProcess;
import com.zoltraak.gateway.config.properties.GpuProperties;
import com.zoltraak.gateway.domain.enums.PodStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@BackgroundProcess
public class IdleTimerChecker {

    private final GpuLifecycleManager gpuLifecycleManager;
    private final GpuProperties gpuProperties;

    public IdleTimerChecker(GpuLifecycleManager gpuLifecycleManager, GpuProperties gpuProperties) {
        this.gpuLifecycleManager = gpuLifecycleManager;
        this.gpuProperties = gpuProperties;
    }

    @Scheduled(fixedDelayString = "${zoltraak.gpu.idle-check-interval-seconds}s")
    void check() {
        PodStatus status = gpuLifecycleManager.getStatus();
        if (status == PodStatus.STOPPED || status == PodStatus.STOPPING) return;

        LocalDateTime lastActivityAt = gpuLifecycleManager.getLastActivityAt();
        if (lastActivityAt == null) return;

        LocalDateTime now = LocalDateTime.now();
        long minutesElapsed = ChronoUnit.MINUTES.between(lastActivityAt, now);

        if (minutesElapsed >= gpuProperties.getIdleCheckTimeoutMinutes()) {
            log.info("GPU pod idle for {}m, threshold {}m, requesting shutdown", minutesElapsed, gpuProperties.getIdleCheckTimeoutMinutes());

            gpuLifecycleManager.requestShutdown().subscribe(
                    null,
                    error -> log.error("GPU pod failed to shutdown GPU pod: ", error)
            );
        }
    }
}
