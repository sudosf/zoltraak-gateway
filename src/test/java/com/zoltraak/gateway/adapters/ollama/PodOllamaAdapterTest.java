package com.zoltraak.gateway.adapters.ollama;

import com.zoltraak.gateway.adapters.gpu.GpuProvider;
import com.zoltraak.gateway.domain.models.ollama.*;
import com.zoltraak.gateway.domain.models.provider.PodConnectionDetails;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// TODO create and handle ollama exceptions
class PodOllamaAdapterTest {

    private MockWebServer mockWebServer;
    private PodOllamaAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = "http://localhost:" + mockWebServer.getPort() + "/";

        GpuProvider gpuProvider = mock(GpuProvider.class);
        when(gpuProvider.getConnectionDetails())
                .thenReturn(Mono.just(new PodConnectionDetails(baseUrl, null)));

        WebClient webClient = WebClient.create();
        adapter = new PodOllamaAdapter(gpuProvider, webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    private String modelsJson() {
        return """
                {
                    "models": [{
                        "name": "llama3",
                        "model": "llama3:latest",
                        "modified_at": "2024-01-01T00:00:00Z",
                        "size": 4000000000,
                        "digest": "abc123",
                        "details": {
                            "format": "gguf",
                            "family": "llama",
                            "families": ["llama"],
                            "parameter_size": "8B",
                            "quantization_level": "Q4_0",
                            "parent_model": ""
                        }
                    }]
                }
                """;
    }

    @Nested
    class WhenCheckingHealth {

        @Test
        void returnsTrue_whenOllamaIsUp() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(200));

            StepVerifier.create(adapter.isHealthy())
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        void returnsFalse_whenOllamaIsDown() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));

            StepVerifier.create(adapter.isHealthy())
                    .expectNext(false)
                    .verifyComplete();
        }
    }

    @Nested
    class WhenGettingVersion {

        @Test
        void returnsVersion_whenOllamaResponds() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""
                            {"version": "0.9.0"}
                            """));

            StepVerifier.create(adapter.getVersion())
                    .expectNextMatches(response -> response.version().equals("0.9.0"))
                    .verifyComplete();
        }

        @Test
        void errorsOut_whenOllamaIsDown() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));

            StepVerifier.create(adapter.getVersion())
                    .expectError(WebClientResponseException.class)
                    .verify();
        }
    }

    @Nested
    class WhenGettingTags {

        @Test
        void returnsModels_whenOllamaResponds() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(modelsJson()));

            StepVerifier.create(adapter.getTags())
                    .expectNextMatches(response ->
                            response.models().size() == 1 &&
                                    response.models().getFirst().name().equals("llama3"))
                    .verifyComplete();
        }

        @Test
        void errorsOut_whenOllamaIsDown() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));

            StepVerifier.create(adapter.getTags())
                    .expectError(WebClientResponseException.class)
                    .verify();
        }
    }

    @Nested
    class WhenGettingPs {

        @Test
        void returnsRunningModels_whenOllamaResponds() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(modelsJson()));

            StepVerifier.create(adapter.getPs())
                    .expectNextMatches(response ->
                            response.models().size() == 1 &&
                                    response.models().getFirst().name().equals("llama3"))
                    .verifyComplete();
        }

        @Test
        void errorsOut_whenOllamaIsDown() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));

            StepVerifier.create(adapter.getPs())
                    .expectError(WebClientResponseException.class)
                    .verify();
        }
    }

    @Nested
    class WhenGenerating {

        @Test
        void streamsResponses_whenOllamaResponds() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/x-ndjson")
                    .setBody("""
                            {"model":"llama3","created_at":"2024-01-01","response":"Hello","done":false}
                            {"model":"llama3","created_at":"2024-01-01","response":"","done":true,"done_reason":"stop"}
                            """));

            OllamaGenerateRequest request = new OllamaGenerateRequest("llama3", "say hello", null, true, false, null);

            StepVerifier.create(adapter.generate(request))
                    .expectNextMatches(res -> res.response().equals("Hello") && !res.done())
                    .expectNextMatches(OllamaGenerateResponse::done)
                    .verifyComplete();
        }

        @Test
        void errorsOut_whenOllamaIsDown() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));

            OllamaGenerateRequest request = new OllamaGenerateRequest("llama3", "say hello", null, true, false, null);

            StepVerifier.create(adapter.generate(request))
                    .expectError(WebClientResponseException.class)
                    .verify();
        }
    }

    @Nested
    class WhenChatting {

        @Test
        void streamsResponses_whenOllamaResponds() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/x-ndjson")
                    .setBody("""
                            {"model":"llama3","created_at":"2024-01-01","message":{"role":"assistant","content":"Hi"},"done":false}
                            {"model":"llama3","created_at":"2024-01-01","message":{"role":"assistant","content":""},"done":true,"done_reason":"stop"}
                            """));

            OllamaMessage message = new OllamaMessage("user", "say hello", null, null);
            OllamaChatRequest request = new OllamaChatRequest("llama3", List.of(message), true, false);

            StepVerifier.create(adapter.chat(request))
                    .expectNextMatches(r -> r.message().content().equals("Hi") && !r.done())
                    .expectNextMatches(OllamaChatResponse::done)
                    .verifyComplete();
        }

        @Test
        void errorsOut_whenOllamaIsDown() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));

            OllamaMessage message = new OllamaMessage("user", "say hello", null, null);
            OllamaChatRequest request = new OllamaChatRequest("llama3", List.of(message), true, false);

            StepVerifier.create(adapter.chat(request))
                    .expectError(WebClientResponseException.class)
                    .verify();
        }
    }
}