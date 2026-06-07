package com.zoltraak.gateway.features.proxy;

import com.zoltraak.gateway.adapters.ollama.OllamaPort;
import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.domain.models.ollama.*;
import com.zoltraak.gateway.features.gpu.GpuLifecycleManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OllamaProxyService {

    private final OllamaPort ollamaPort;
    private final GpuLifecycleManager gpuLifecycleManager;

    public OllamaProxyService(OllamaPort ollamaPort, GpuLifecycleManager gpuLifecycleManager) {
        this.ollamaPort = ollamaPort;
        this.gpuLifecycleManager = gpuLifecycleManager;
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
        return checkPodReady().then(operation);
    }

    public <T> Flux<T> withPodReady(Flux<T> operation) {
        return checkPodReady().thenMany(operation);
    }

    private Mono<Void> checkPodReady() {
        // TODO add incoming req to queue
        PodStatus status = gpuLifecycleManager.getStatus();

        if (status == PodStatus.READY) {
            gpuLifecycleManager.resetIdleTimer();
            return Mono.empty();
        }

        if (status == PodStatus.DEGRADED) {
            return Mono.empty();
        }

        if (status == PodStatus.STOPPED || status == PodStatus.STOPPING) {
            return gpuLifecycleManager.requestStart()
                    .then(Mono.error(new RuntimeException("OllamaService - GPU pod not ready")));
        }

        return Mono.error(new RuntimeException("OllamaService - GPU pod not ready, status=" + status));
    }
}
