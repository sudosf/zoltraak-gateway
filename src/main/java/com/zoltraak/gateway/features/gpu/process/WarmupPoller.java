package com.zoltraak.gateway.features.gpu.process;

import com.zoltraak.gateway.adapters.ollama.OllamaClient;
import com.zoltraak.gateway.annotations.BackgroundProcess;
import com.zoltraak.gateway.config.properties.GpuProperties;
import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.exception.ExceptionUtils;
import com.zoltraak.gateway.features.gpu.GpuLifecycleManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@BackgroundProcess
public class WarmupPoller {

    private final GpuLifecycleManager gpuLifecycleManager;
    private final OllamaClient ollamaClient;
    private final GpuProperties gpuProperties;
    private long lastErrorLoggedMinute = -1;

    public WarmupPoller(GpuLifecycleManager gpuLifecycleManager, OllamaClient ollamaClient, GpuProperties gpuProperties) {
        this.gpuLifecycleManager = gpuLifecycleManager;
        this.ollamaClient = ollamaClient;
        this.gpuProperties = gpuProperties;
    }

    @Scheduled(fixedDelayString = "${zoltraak.gpu.warmup-poll-interval-seconds}s",
            initialDelayString = "${zoltraak.gpu.warmup-poll-initial-delay-seconds}s")
    void poll() {
        PodStatus status = gpuLifecycleManager.getStatus();
        if (status != PodStatus.WARMING && status != PodStatus.STARTING) return;

        LocalDateTime lastSessionStartedAt = gpuLifecycleManager.getSessionStartedAt();
        if (lastSessionStartedAt == null) return;

        LocalDateTime now = LocalDateTime.now();
        long minutesElapsed = ChronoUnit.MINUTES.between(lastSessionStartedAt, now);

        if (minutesElapsed >= gpuProperties.getWarmupTimeoutMinutes()) {
            log.warn("GPU pod warmup timed out after {}m, status = {}", minutesElapsed, PodStatus.DEGRADED);
            gpuLifecycleManager.onPodDegraded();
            return;
        }

        ollamaClient.isHealthy().subscribe(
                healthy -> {
                    if (healthy) {
                        log.info("GPU pod healthy after {}m, status = {}", minutesElapsed, PodStatus.READY);
                        gpuLifecycleManager.onPodReady();
                    }
                },
                error -> {
                    if (minutesElapsed > lastErrorLoggedMinute) {
                        log.warn("GPU pod health check failed: {}", ExceptionUtils.getRootCauseMessage(error));
                        lastErrorLoggedMinute = minutesElapsed;
                    }
                }
        );
    }
}

