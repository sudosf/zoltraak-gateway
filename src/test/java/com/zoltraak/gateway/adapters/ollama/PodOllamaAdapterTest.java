package com.zoltraak.gateway.adapters.ollama;

import com.zoltraak.gateway.adapters.gpu.GpuProviderPort;
import com.zoltraak.gateway.domain.models.ollama.*;
import com.zoltraak.gateway.domain.models.provider.PodConnectionDetails;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PodOllamaAdapterTest {

    private MockWebServer mockWebServer;
    private PodOllamaAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = "http://localhost:" + mockWebServer.getPort() + "/";

        GpuProviderPort gpuProviderPort = mock(GpuProviderPort.class);
        when(gpuProviderPort.getConnectionDetails())
                .thenReturn(Mono.just(new PodConnectionDetails(baseUrl, null)));

        WebClient webClient = WebClient.create();
        adapter = new PodOllamaAdapter(gpuProviderPort, webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void isHealthy_returnsTrue_whenOllamaIsUp() {
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(200)
        );

        StepVerifier.create(adapter.isHealthy())
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void isHealthy_returnsFalse_whenOllamaIsDown() {
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(500)
        );

        StepVerifier.create(adapter.isHealthy())
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void getVersion_returnsVersion_whenOllamaResponds() {
        String json = """
                {"version": "0.9.0"}
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        );

        StepVerifier.create(adapter.getVersion())
                .expectNextMatches(response -> response.version().equals("0.9.0"))
                .verifyComplete();
    }

    @Test
    void getVersion_errorsOut_whenOllamaIsDown() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(adapter.getVersion())
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    void getTags_returnsModels_whenOllamaResponds() {
        String json = """
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

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        );

        StepVerifier.create(adapter.getTags())
                .expectNextMatches(response ->
                        response.models().size() == 1 &&
                                response.models().getFirst().name().equals("llama3"))
                .verifyComplete();
    }

    @Test
    void getTags_errorsOut_whenOllamaIsDown() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(adapter.getTags())
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    void getPs_returnsRunningModels_whenOllamaResponds() {
        String json = """
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

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        );

        StepVerifier.create(adapter.getPs())
                .expectNextMatches(response ->
                        response.models().size() == 1 &&
                                response.models().getFirst().name().equals("llama3"))
                .verifyComplete();
    }

    @Test
    void getPs_errorsOut_whenOllamaIsDown() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(adapter.getPs())
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    void generate_streamsResponses_whenOllamaResponds() {
        String ndjson = """
                {"model":"llama3","created_at":"2024-01-01","response":"Hello","done":false}
                {"model":"llama3","created_at":"2024-01-01","response":"","done":true,"done_reason":"stop"}
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/x-ndjson")
                .setBody(ndjson)
        );

        OllamaGenerateRequest request = new OllamaGenerateRequest("llama3", "say hello", null, true, false, null);

        StepVerifier.create(adapter.generate(request))
                .expectNextMatches(res -> res.response().equals("Hello") && !res.done())
                .expectNextMatches(OllamaGenerateResponse::done)
                .verifyComplete();
    }

    @Test
    void generate_errorsOut_whenOllamaIsDown() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        OllamaGenerateRequest request = new OllamaGenerateRequest("llama3", "say hello", null, true, false, null);

        StepVerifier.create(adapter.generate(request))
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    void chat_streamsResponses_whenOllamaResponds() {
        String ndjson = """
                {"model":"llama3","created_at":"2024-01-01","message":{"role":"assistant","content":"Hi"},"done":false}
                {"model":"llama3","created_at":"2024-01-01","message":{"role":"assistant","content":""},"done":true,"done_reason":"stop"}
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/x-ndjson")
                .setBody(ndjson)
        );

        OllamaMessage message = new OllamaMessage("user", "say hello", null, null);
        OllamaChatRequest request = new OllamaChatRequest("llama3", List.of(message), true, false);

        StepVerifier.create(adapter.chat(request))
                .expectNextMatches(r -> r.message().content().equals("Hi") && !r.done())
                .expectNextMatches(OllamaChatResponse::done)
                .verifyComplete();
    }

    @Test
    void chat_errorsOut_whenOllamaIsDown() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        OllamaMessage message = new OllamaMessage("user", "say hello", null, null);
        OllamaChatRequest request = new OllamaChatRequest("llama3", List.of(message), true, false);

        StepVerifier.create(adapter.chat(request))
                .expectError(WebClientResponseException.class)
                .verify();
    }
}
