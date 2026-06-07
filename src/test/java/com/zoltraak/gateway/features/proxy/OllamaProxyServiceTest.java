package com.zoltraak.gateway.features.proxy;


import com.zoltraak.gateway.adapters.ollama.OllamaPort;
import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.domain.models.ollama.*;
import com.zoltraak.gateway.features.gpu.GpuLifecycleManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OllamaProxyServiceTest {

    @Mock
    private OllamaPort ollamaPort;

    @Mock
    private GpuLifecycleManager gpuLifecycleManager;

    private OllamaProxyService service;

    @BeforeEach
    void setUp() {
        service = new OllamaProxyService(ollamaPort, gpuLifecycleManager);
    }

    @Nested
    @Disabled("Pending Request Queue implementation") // TODO
    class CheckPodReady {

        @Nested
        class WhenPod_IsOperational {

            OllamaVersionResponse response = new OllamaVersionResponse("0.1.0");

            @ParameterizedTest
            @EnumSource(value = PodStatus.class, names = {"WARMING", "READY"})
            void operationProceeds(PodStatus status) {
                when(gpuLifecycleManager.getStatus()).thenReturn(status);
                when(ollamaPort.getVersion()).thenReturn(Mono.just(response));

                StepVerifier.create(service.getVersion())
                        .expectNext(response)
                        .verifyComplete();
            }

            @ParameterizedTest
            @EnumSource(value = PodStatus.class, names = {"WARMING", "READY"})
            void returnsVersion_andResetsIdleTimer(PodStatus status) {
                when(gpuLifecycleManager.getStatus()).thenReturn(status);
                when(ollamaPort.getVersion()).thenReturn(Mono.just(response));

                StepVerifier.create(service.getVersion()).expectNext(response).verifyComplete();

                verify(gpuLifecycleManager).resetIdleTimer();
            }
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
                    null, null,
                    null, null, null
            );

            when(ollamaPort.chat(request)).thenReturn(Flux.just(response));

            StepVerifier.create(service.forwardChat(request))
                    .expectNext(response)
                    .verifyComplete();
        }

        @Test
        void forwardGenerate_streamsResponsesFromPort() {
            OllamaGenerateRequest request = new OllamaGenerateRequest(
                    "llama3", "hello", List.of(),
                    false, false, null
            );

            OllamaGenerateResponse response = new OllamaGenerateResponse(
                    "llama3", null, "response text",
                    null, true, null
            );

            when(ollamaPort.generate(request)).thenReturn(Flux.just(response));

            StepVerifier.create(service.forwardGenerate(request))
                    .expectNext(response)
                    .verifyComplete();
        }

        @Test
        void getTags_returnsPortResponse() {
            OllamaModelsResponse response = new OllamaModelsResponse(List.of());

            when(ollamaPort.getTags()).thenReturn(Mono.just(response));

            StepVerifier.create(service.getTags())
                    .expectNext(response)
                    .verifyComplete();
        }
    }
}