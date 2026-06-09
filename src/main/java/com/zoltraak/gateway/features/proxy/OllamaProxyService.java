package com.zoltraak.gateway.features.proxy;

import com.zoltraak.gateway.adapters.ollama.OllamaPort;
import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.domain.models.ollama.*;
import com.zoltraak.gateway.features.gpu.GpuLifecycleManager;
import com.zoltraak.gateway.features.gpu.QueuedRequest;
import com.zoltraak.gateway.features.gpu.RequestQueue;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

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

    public Flux<OllamaChatResponse> forwardChat(OllamaChatRequest request) {
        return withPodReady(ollamaPort.chat(request));
    }

    public Flux<OllamaGenerateResponse> forwardGenerate(OllamaGenerateRequest request) {
        return withPodReady(ollamaPort.generate(request));
    }

    public Mono<OllamaModelsResponse> getTags() {
        return withPodReady(ollamaPort.getTags());
    }

    public Mono<OllamaModelsResponse> getPs() {
        return withPodReady(ollamaPort.getPs());
    }

    public Mono<OllamaVersionResponse> getVersion() {
        return withPodReady(ollamaPort.getVersion());
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
            gpuLifecycleManager.requestStart().subscribe();
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
