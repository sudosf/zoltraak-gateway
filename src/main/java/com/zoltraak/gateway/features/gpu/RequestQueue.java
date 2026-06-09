package com.zoltraak.gateway.features.gpu;

import com.zoltraak.gateway.domain.enums.GatewayErrorCode;
import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.domain.exception.PodNotReadyException;
import com.zoltraak.gateway.domain.exception.RequestExpiredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Component
public class RequestQueue {

    private final ConcurrentLinkedQueue<QueuedRequest> queue = new ConcurrentLinkedQueue<>();

    public void enqueue(QueuedRequest request) {
        queue.add(request);
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void onPodReady() {
        log.info("GPU pod is ready, processing [{}] queued requests", queue.size());

        QueuedRequest request;
        while ((request = dequeue()) != null) {
            log.debug("GPU pod request processing id={}", request.requestId());
            request.task().run();
        }
    }

    public void onPodStartFailed(PodStatus status) {
        log.warn("GPU pod start failed, rejecting [{}] queued requests", queue.size());
        dequeueFailedRequests(new PodNotReadyException(
                status,
                GatewayErrorCode.POD_START_FAILED,
                "GPU pod failed to start"));
    }

    public void onPodDegraded() {
        log.warn("GPU pod ollama timeout, rejecting [{}] queued requests", queue.size());

        dequeueFailedRequests(new PodNotReadyException(
                PodStatus.DEGRADED,
                GatewayErrorCode.WARMUP_TIMEOUT,
                "GPU pod failed to initialize ollama"));
    }

    public void evictExpiredRequests(int maxWaitMinutes) {

        LocalDateTime now = LocalDateTime.now();
        Iterator<QueuedRequest> it = queue.iterator();
        while (it.hasNext()) {
            QueuedRequest request = it.next();
            long minutesElapsed = ChronoUnit.MINUTES.between(request.enqueuedAt(), now);
            if (minutesElapsed > maxWaitMinutes) {
                it.remove();
                request.onFailure().accept(new RequestExpiredException(GatewayErrorCode.QUEUE_TIMEOUT, "Request timed out"));
            }
        }
    }

    private void dequeueFailedRequests(PodNotReadyException ex) {
        QueuedRequest request;
        while ((request = dequeue()) != null) {
            request.onFailure().accept(ex);
        }
    }

    private QueuedRequest dequeue() {
        return queue.poll();
    }
}