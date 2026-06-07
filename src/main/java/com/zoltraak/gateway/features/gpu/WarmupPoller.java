package com.zoltraak.gateway.features.gpu;

import com.zoltraak.gateway.adapters.ollama.OllamaPort;
import com.zoltraak.gateway.annotations.BackgroundProcess;
import com.zoltraak.gateway.config.properties.GpuProperties;
import com.zoltraak.gateway.domain.enums.PodStatus;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@BackgroundProcess
public class WarmupPoller {

    private final GpuLifecycleManager gpuLifecycleManager;
    private final OllamaPort ollamaPort;
    private final GpuProperties gpuProperties;

    public WarmupPoller(GpuLifecycleManager gpuLifecycleManager, OllamaPort ollamaPort, GpuProperties gpuProperties) {
        this.gpuLifecycleManager = gpuLifecycleManager;
        this.ollamaPort = ollamaPort;
        this.gpuProperties = gpuProperties;
    }

    @Scheduled(fixedDelayString = "${zoltraak.gpu.warmup-poll-interval-seconds}s")
    void poll() {
        PodStatus status = gpuLifecycleManager.getStatus();
        if (status != PodStatus.WARMING && status != PodStatus.STARTING) return;

        LocalDateTime now = LocalDateTime.now();
        long minutesElapsed = ChronoUnit.MINUTES.between(gpuLifecycleManager.getSessionStartedAt(), now);

        if (minutesElapsed >= gpuProperties.getWarmupTimeoutMinutes()) {
            gpuLifecycleManager.setStatus(PodStatus.DEGRADED);
            return;
        }

        ollamaPort.isHealthy().subscribe(healthy -> {
            if (healthy) {
                gpuLifecycleManager.setStatus(PodStatus.READY);
            }
        });
    }
}

