package com.zoltraak.gateway.features.proxy;

import com.zoltraak.gateway.adapters.ollama.OllamaPort;
import com.zoltraak.gateway.domain.enums.PodStatus;
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

    private final OllamaPort ollamaPort;
    private final GpuLifecycleManager gpuLifecycleManager;
    private final RequestQueue requestQueue;

    public OllamaProxyService(OllamaPort ollamaPort, GpuLifecycleManager gpuLifecycleManager, RequestQueue requestQueue) {
        this.ollamaPort = ollamaPort;
        this.gpuLifecycleManager = gpuLifecycleManager;
        this.requestQueue = requestQueue;
    }

    public Flux<byte[]> forwardChat(Flux<byte[]> request, HttpHeaders headers) {
        return withPodReady(ollamaPort.chat(request, headers));
    }

    public Flux<byte[]> forwardGenerate(Flux<byte[]> request, HttpHeaders headers) {
        return withPodReady(ollamaPort.generate(request, headers));
    }

    public Mono<byte[]> embed(Flux<byte[]> request, HttpHeaders headers) {
        return withPodReady(ollamaPort.embed(request, headers));
    }

    public Mono<byte[]> show(Flux<byte[]> request, HttpHeaders headers) {
        return withPodReady(ollamaPort.show(request, headers));
    }

    public Mono<byte[]> getTags(HttpHeaders headers) {
        return withPodReady(ollamaPort.getTags(headers));
    }

    public Mono<byte[]> getPs(HttpHeaders headers) {
        return withPodReady(ollamaPort.getPs(headers));
    }

    public Mono<byte[]> getVersion(HttpHeaders headers) {
        return withPodReady(ollamaPort.getVersion(headers));
    }

    public <T> Mono<T> withPodReady(Mono<T> operation) {
        if (isPodAvailable()) return operation;

        return Mono.create(sink -> requestQueue.enqueue(
                createQueuedRequest(
                        () -> operation.subscribe(sink::success, sink::error),
                        sink::error
                )));
    }

    public <T> Flux<T> withPodReady(Flux<T> operation) {
        if (isPodAvailable()) return operation;

        return Flux.create(sink -> requestQueue.enqueue(
                createQueuedRequest(
                        () -> operation.subscribe(sink::next, sink::error, sink::complete),
                        sink::error
                )));
    }

    private boolean isPodAvailable() {
        PodStatus status = gpuLifecycleManager.getStatus();

        if (status == PodStatus.READY) {
            gpuLifecycleManager.resetIdleTimer();
            return true;
        }

        if (status == PodStatus.DEGRADED) {
            return true;
        }

        if (status == PodStatus.STOPPED) {
            gpuLifecycleManager.requestStart()
                    .subscribe(null,
                            ex -> log.warn(
                                    "GPU pod failed to start, message = {}",
                                    ExceptionUtils.getRootCauseMessage(ex)
                            ));
        }

        return false;
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
