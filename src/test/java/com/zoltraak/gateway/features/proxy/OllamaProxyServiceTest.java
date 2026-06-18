package com.zoltraak.gateway.features.proxy;


import com.zoltraak.gateway.adapters.ollama.OllamaClient;
import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.features.gpu.GpuLifecycleManager;
import com.zoltraak.gateway.features.gpu.RequestQueue;
import com.zoltraak.gateway.features.gpu.model.QueuedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OllamaProxyServiceTest {

    @Mock
    private OllamaClient ollamaClient;
    @Mock
    private GpuLifecycleManager gpuLifecycleManager;
    @Mock
    private RequestQueue requestQueue;

    private OllamaProxyService service;

    @BeforeEach
    void setUp() {
        service = new OllamaProxyService(ollamaClient, gpuLifecycleManager, requestQueue);
    }

    @Nested
    class WhenPodIsAvailable {
        byte[] response = "{\"version\": \"0.1.0\"}".getBytes();

        @Test
        void whenReady_operationProceeds() {
            when(gpuLifecycleManager.getStatus()).thenReturn(PodStatus.READY);
            when(ollamaClient.getVersion(any(HttpHeaders.class))).thenReturn(Mono.just(response));

            StepVerifier.create(service.getVersion(new HttpHeaders()))
                    .expectNext(response)
                    .verifyComplete();
        }

        @Test
        void whenReady_resetsIdleTimer() {
            when(gpuLifecycleManager.getStatus()).thenReturn(PodStatus.READY);
            when(ollamaClient.getVersion(any(HttpHeaders.class))).thenReturn(Mono.just(response));

            StepVerifier.create(service.getVersion(new HttpHeaders())).expectNextCount(1).verifyComplete();

            verify(gpuLifecycleManager).resetIdleTimer();
        }

        @Test
        void whenDegraded_operationProceeds() {
            when(gpuLifecycleManager.getStatus()).thenReturn(PodStatus.DEGRADED);
            when(ollamaClient.getVersion(any(HttpHeaders.class))).thenReturn(Mono.just(response));

            StepVerifier.create(service.getVersion(new HttpHeaders()))
                    .expectNext(response)
                    .verifyComplete();
        }

        @Test
        void whenDegraded_doesNotResetIdleTimer() {
            when(gpuLifecycleManager.getStatus()).thenReturn(PodStatus.DEGRADED);
            when(ollamaClient.getVersion(any(HttpHeaders.class))).thenReturn(Mono.just(response));

            StepVerifier.create(service.getVersion(new HttpHeaders())).expectNextCount(1).verifyComplete();

            verify(gpuLifecycleManager, never()).resetIdleTimer();
        }
    }

    @Nested
    class WhenPodIsNotAvailable {

        @Test
        void whenStopped_requestsStart_andEnqueuesRequest() {
            when(gpuLifecycleManager.getStatus()).thenReturn(PodStatus.STOPPED);
            when(gpuLifecycleManager.requestStart()).thenReturn(Mono.empty());

            service.getVersion(new HttpHeaders()).subscribe();

            verify(gpuLifecycleManager).requestStart();
            verify(requestQueue).enqueue(any(QueuedRequest.class));
        }

        @ParameterizedTest
        @EnumSource(value = PodStatus.class, names = {"WARMING", "STARTING", "STOPPING"})
        void whenTransitioning_enqueuesRequest_withoutRequestingStart(PodStatus status) {
            when(gpuLifecycleManager.getStatus()).thenReturn(status);

            service.getVersion(new HttpHeaders()).subscribe();

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
            byte[] request = "{\"model\": \"llama3\", \"messages\": [], \"stream\": false, \"raw\": false}".getBytes();
            byte[] response = "{\"model\": \"llama3\", \"done\": true}".getBytes();

            when(ollamaClient.chat(any(), any(HttpHeaders.class))).thenReturn(Flux.just(response));

            StepVerifier.create(service.forwardChat(Flux.just(request), new HttpHeaders()))
                    .expectNextCount(1)
                    .verifyComplete();
            verify(ollamaClient).chat(any(), any(HttpHeaders.class));
        }

        @Test
        void forwardGenerate_callsGenerateOnPort() {
            byte[] requestBytes = "{\"model\": \"llama3\", \"prompt\": \"hello\", \"messages\": [], \"stream\": false, \"raw\": false}".getBytes();
            byte[] response = "{\"model\": \"llama3\", \"response\": \"response text\", \"done\": true}".getBytes();

            when(ollamaClient.generate(any(), any(HttpHeaders.class))).thenReturn(Flux.just(response));

            StepVerifier.create(service.forwardGenerate(Flux.just(requestBytes), new HttpHeaders()))
                    .expectNextCount(1)
                    .verifyComplete();
            verify(ollamaClient).generate(any(), any(HttpHeaders.class));
        }

        @Test
        void getTags_returnsPortResponse() {
            byte[] response = "{\"models\": []}".getBytes();

            when(ollamaClient.getTags(any(HttpHeaders.class))).thenReturn(Mono.just(response));

            StepVerifier.create(service.getTags(new HttpHeaders()))
                    .expectNextCount(1)
                    .verifyComplete();
            verify(ollamaClient).getTags(any(HttpHeaders.class));
        }

        @Test
        void embed_returnsPortResponse() {
            byte[] requestBytes = "{\"model\": \"embeddinggemma\", \"input\": \"hello\"}".getBytes();
            byte[] response = "{\"model\": \"embeddinggemma\", \"embeddings\": [[0.01, 0.02]]}".getBytes();

            when(ollamaClient.embed(any(), any(HttpHeaders.class))).thenReturn(Mono.just(response));

            StepVerifier.create(service.embed(Flux.just(requestBytes), new HttpHeaders()))
                    .expectNextCount(1)
                    .verifyComplete();
            verify(ollamaClient).embed(any(), any(HttpHeaders.class));
        }

        @Test
        void show_returnsPortResponse() {
            byte[] requestBytes = "{\"model\": \"llama3\"}".getBytes();
            byte[] response = "{\"modelfile\": \"FROM llama3\"}".getBytes();

            when(ollamaClient.show(any(), any(HttpHeaders.class))).thenReturn(Mono.just(response));

            StepVerifier.create(service.show(Flux.just(requestBytes), new HttpHeaders()))
                    .expectNextCount(1)
                    .verifyComplete();
            verify(ollamaClient).show(any(), any(HttpHeaders.class));
        }
    }
}
