package com.zoltraak.gateway.features.gpu.process;

import com.zoltraak.gateway.annotations.BackgroundProcess;
import com.zoltraak.gateway.config.properties.QueueProperties;
import com.zoltraak.gateway.features.gpu.RequestQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@BackgroundProcess
public class ExpiredRequestSweeper {

    private final RequestQueue requestQueue;
    private final QueueProperties queueProperties;

    public ExpiredRequestSweeper(RequestQueue requestQueue, QueueProperties queueProperties) {
        this.requestQueue = requestQueue;
        this.queueProperties = queueProperties;
    }

    @Scheduled(fixedDelayString = "${zoltraak.gpu.idle-check-interval-seconds}s")
    void sweep() {
        if (requestQueue.isEmpty()) return;
        requestQueue.evictExpiredRequests(queueProperties.getMaxWaitMinutes());
    }
}
