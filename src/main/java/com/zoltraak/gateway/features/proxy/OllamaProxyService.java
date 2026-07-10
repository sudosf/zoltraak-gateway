package com.zoltraak.gateway.features.proxy;

import com.zoltraak.gateway.adapters.ollama.OllamaClient;
import com.zoltraak.gateway.domain.enums.GatewayErrorCode;
import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.domain.exception.GatewayServiceException;
import com.zoltraak.gateway.exception.ExceptionUtils;
import com.zoltraak.gateway.features.gpu.GpuLifecycleManager;
import com.zoltraak.gateway.features.gpu.RequestQueue;
import com.zoltraak.gateway.features.gpu.model.QueuedRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Service
public class OllamaProxyService {

    private final OllamaClient ollamaClient;
    private final GpuLifecycleManager gpuLifecycleManager;
    private final RequestQueue requestQueue;

    public OllamaProxyService(OllamaClient ollamaClient, GpuLifecycleManager gpuLifecycleManager, RequestQueue requestQueue) {
        this.ollamaClient = ollamaClient;
        this.gpuLifecycleManager = gpuLifecycleManager;
        this.requestQueue = requestQueue;
    }

    public Flux<byte[]> forwardChat(Flux<byte[]> request, HttpHeaders headers) {
        return withPodReady(Flux.defer(() -> ollamaClient.chat(request, headers)));
    }

    public Flux<byte[]> forwardGenerate(Flux<byte[]> request, HttpHeaders headers) {
        return withPodReady(Flux.defer(() -> ollamaClient.generate(request, headers)));
    }

    public Mono<byte[]> embed(Flux<byte[]> request, HttpHeaders headers) {
        return withPodReady(Mono.defer(() -> ollamaClient.embed(request, headers)));
    }

    public Mono<byte[]> show(Flux<byte[]> request, HttpHeaders headers) {
        return withPodReady(Mono.defer(() -> ollamaClient.show(request, headers)));
    }

    public Mono<byte[]> getTags(HttpHeaders headers) {
        return withPodReady(Mono.defer(() -> ollamaClient.getTags(headers)));
    }

    public Mono<byte[]> getPs(HttpHeaders headers) {
        return withPodReady(Mono.defer(() -> ollamaClient.getPs(headers)));
    }

    public Mono<byte[]> getVersion(HttpHeaders headers) {
        return withPodReady(Mono.defer(() -> ollamaClient.getVersion(headers)));
    }

    public <T> Flux<T> withPodReady(Flux<T> operation) {

        if (isPodAvailable()) {
            gpuLifecycleManager.resetIdleTimer();
            return operation;
        }

        if (isPodDegraded()) {
            return Flux.error(
                    new GatewayServiceException(GatewayErrorCode.WARMUP_TIMEOUT, "GPU pod warmup timed out")
            );
        }

        requestStartIfStopped();
        return Flux.create(sink -> requestQueue.enqueue(
                createQueuedRequest(
                        () -> operation.subscribe(sink::next, sink::error, sink::complete),
                        sink::error
                )));
    }

    public <T> Mono<T> withPodReady(Mono<T> operation) {
        return withPodReady(operation.flux()).next();
    }

    private boolean isPodAvailable() {
        return gpuLifecycleManager.getStatus() == PodStatus.READY;
    }

    private boolean isPodDegraded() {
        return gpuLifecycleManager.getStatus() == PodStatus.DEGRADED;
    }

    private void requestStartIfStopped() {
        if (gpuLifecycleManager.getStatus() == PodStatus.STOPPED) {
            gpuLifecycleManager.requestStart()
                    .subscribe(null,
                            ex -> log.warn(
                                    "GPU pod failed to start, message = {}",
                                    ExceptionUtils.getRootCauseMessage(ex)
                            ));
        }
    }

    private QueuedRequest createQueuedRequest(Runnable task, Consumer<Throwable> onFailure) {
        return new QueuedRequest(
                UUID.randomUUID().toString(),
                LocalDateTime.now(),
                task,
                onFailure
        );
    }
}
