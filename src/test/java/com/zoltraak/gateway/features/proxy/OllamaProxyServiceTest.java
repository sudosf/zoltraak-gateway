package com.zoltraak.gateway.features.proxy;


import com.zoltraak.gateway.adapters.ollama.OllamaPort;
import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.domain.models.ollama.*;
import com.zoltraak.gateway.features.gpu.GpuLifecycleManager;
import com.zoltraak.gateway.features.gpu.QueuedRequest;
import com.zoltraak.gateway.features.gpu.RequestQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OllamaProxyServiceTest {

    @Mock
    private OllamaPort ollamaPort;
    @Mock
    private GpuLifecycleManager gpuLifecycleManager;
    @Mock
    private RequestQueue requestQueue;

    private OllamaProxyService service;

    @BeforeEach
    void setUp() {
        service = new OllamaProxyService(ollamaPort, gpuLifecycleManager, requestQueue);
    }

    @Nested
    class WhenPodIsAvailable {
        OllamaVersionResponse response = new OllamaVersionResponse("0.1.0");

        @Test
        void whenReady_operationProceeds() {
            when(gpuLifecycleManager.getStatus()).thenReturn(PodStatus.READY);
            when(ollamaPort.getVersion()).thenReturn(Mono.just(response));

            StepVerifier.create(service.getVersion())
                    .expectNext(response)
                    .verifyComplete();
        }

        @Test
        void whenReady_resetsIdleTimer() {
            when(gpuLifecycleManager.getStatus()).thenReturn(PodStatus.READY);
            when(ollamaPort.getVersion()).thenReturn(Mono.just(response));

            StepVerifier.create(service.getVersion()).expectNextCount(1).verifyComplete();

            verify(gpuLifecycleManager).resetIdleTimer();
        }

        @Test
        void whenDegraded_operationProceeds() {
            when(gpuLifecycleManager.getStatus()).thenReturn(PodStatus.DEGRADED);
            when(ollamaPort.getVersion()).thenReturn(Mono.just(response));

            StepVerifier.create(service.getVersion())
                    .expectNext(response)
                    .verifyComplete();
        }

        @Test
        void whenDegraded_doesNotResetIdleTimer() {
            when(gpuLifecycleManager.getStatus()).thenReturn(PodStatus.DEGRADED);
            when(ollamaPort.getVersion()).thenReturn(Mono.just(response));

            StepVerifier.create(service.getVersion()).expectNextCount(1).verifyComplete();

            verify(gpuLifecycleManager, never()).resetIdleTimer();
        }
    }

    @Nested
    class WhenPodIsNotAvailable {

        @Test
        void whenStopped_requestsStart_andEnqueuesRequest() {
            when(gpuLifecycleManager.getStatus()).thenReturn(PodStatus.STOPPED);
            when(gpuLifecycleManager.requestStart()).thenReturn(Mono.empty());

            service.getVersion().subscribe();

            verify(gpuLifecycleManager).requestStart();
            verify(requestQueue).enqueue(any(QueuedRequest.class));
        }

        @ParameterizedTest
        @EnumSource(value = PodStatus.class, names = {"WARMING", "STARTING", "STOPPING"})
        void whenTransitioning_enqueuesRequest_withoutRequestingStart(PodStatus status) {
            when(gpuLifecycleManager.getStatus()).thenReturn(status);

            service.getVersion().subscribe();

            verify(gpuLifecycleManager, never()).requestStart();
            verify(requestQueue).enqueue(any(QueuedRequest.class));
        }
    }

    @Nested
    class Forwarding {

        @BeforeEach
        void podReady() {
            when(gpuLifecycleManager.getStatus()).thenReturn(PodStatus.READY);
        }

        @Test
        void forwardChat_streamsResponsesFromPort() {
            OllamaChatRequest request = new OllamaChatRequest(
                    "llama3", List.of(), false, false
            );

            OllamaChatResponse response = new OllamaChatResponse(
                    "llama3", null, null,
                    true, null, null,
                    null, null, null,
                    null, null
            );

            when(ollamaPort.chat(request)).thenReturn(Flux.just(response));

            StepVerifier.create(service.forwardChat(request))
                    .expectNextCount(1)
                    .verifyComplete();
            verify(ollamaPort).chat(request);
        }

        @Test
        void forwardGenerate_callsGenerateOnPort() {
            OllamaGenerateRequest request = new OllamaGenerateRequest(
                    "llama3", "hello", List.of(),
                    false, false, null
            );

            when(ollamaPort.generate(request)).thenReturn(Flux.just(
                    new OllamaGenerateResponse(
                            "llama3", null, "response text",
                            null, true, null)
            ));

            StepVerifier.create(service.forwardGenerate(request))
                    .expectNextCount(1)
                    .verifyComplete();

            verify(ollamaPort).generate(request);
        }

        @Test
        void getTags_returnsPortResponse() {
            OllamaModelsResponse response = new OllamaModelsResponse(List.of());
            when(ollamaPort.getTags()).thenReturn(Mono.just(response));

            StepVerifier.create(service.getTags())
                    .expectNextCount(1)
                    .verifyComplete();
            verify(ollamaPort).getTags();
        }
    }
}
