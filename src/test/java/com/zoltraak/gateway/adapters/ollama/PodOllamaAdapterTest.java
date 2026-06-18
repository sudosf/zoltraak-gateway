package com.zoltraak.gateway.adapters.ollama;

import com.zoltraak.gateway.adapters.gpu.GpuProvider;
import com.zoltraak.gateway.domain.models.provider.PodConnectionDetails;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

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

            StepVerifier.create(adapter.getVersion(new HttpHeaders()))
                    .expectNextMatches(bytes -> new String(bytes).contains("\"version\": \"0.9.0\""))
                    .verifyComplete();
        }

        @Test
        void errorsOut_whenOllamaIsDown() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));

            StepVerifier.create(adapter.getVersion(new HttpHeaders()))
                    .expectError(OllamaException.class)
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

            StepVerifier.create(adapter.getTags(new HttpHeaders()))
                    .expectNextMatches(bytes -> new String(bytes).contains("\"name\": \"llama3\""))
                    .verifyComplete();
        }

        @Test
        void errorsOut_whenOllamaIsDown() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));

            StepVerifier.create(adapter.getTags(new HttpHeaders()))
                    .expectError(OllamaException.class)
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

            StepVerifier.create(adapter.getPs(new HttpHeaders()))
                    .expectNextMatches(bytes -> new String(bytes).contains("\"name\": \"llama3\""))
                    .verifyComplete();
        }

        @Test
        void errorsOut_whenOllamaIsDown() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));

            StepVerifier.create(adapter.getPs(new HttpHeaders()))
                    .expectError(OllamaException.class)
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

            byte[] requestBody = """
                    {"model":"llama3","prompt":"say hello","stream":true}
                    """.getBytes();

            Mono<String> response = adapter.generate(Flux.just(requestBody), new HttpHeaders())
                    .map(String::new)
                    .reduce(String::concat);

            StepVerifier.create(response)
                    .expectNextMatches(body -> body.contains("\"response\":\"Hello\"") && body.contains("\"done\":true"))
                    .verifyComplete();
        }

        @Test
        void errorsOut_whenOllamaIsDown() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));

            StepVerifier.create(adapter.generate(Flux.just("{}".getBytes()), new HttpHeaders()))
                    .expectError(OllamaException.class)
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

            byte[] requestBody = """
                    {"model":"llama3","messages":[{"role":"user","content":"say hello"}],"stream":true}
                    """.getBytes();

            Mono<String> response = adapter.chat(Flux.just(requestBody), new HttpHeaders())
                    .map(String::new)
                    .reduce(String::concat);

            StepVerifier.create(response)
                    .expectNextMatches(body -> body.contains("\"content\":\"Hi\"") && body.contains("\"done\":true"))
                    .verifyComplete();
        }

        @Test
        void errorsOut_whenOllamaIsDown() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));

            StepVerifier.create(adapter.chat(Flux.just("{}".getBytes()), new HttpHeaders()))
                    .expectError(OllamaException.class)
                    .verify();
        }
    }


    @Nested
    class WhenEmbedding {

        @Test
        void returnsEmbeddings_whenOllamaResponds() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""
                            {"model":"embeddinggemma","embeddings":[[0.01,0.02,0.03]]}
                            """));

            byte[] requestBody = """
                    {"model":"embeddinggemma","input":"Why is the sky blue?"}
                    """.getBytes();

            StepVerifier.create(adapter.embed(Flux.just(requestBody), new HttpHeaders()))
                    .expectNextMatches(bytes -> new String(bytes).contains("\"embeddings\""))
                    .verifyComplete();
        }

        @Test
        void errorsOut_whenOllamaIsDown() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));

            StepVerifier.create(adapter.embed(Flux.just("{}".getBytes()), new HttpHeaders()))
                    .expectError(OllamaException.class)
                    .verify();
        }
    }

    @Nested
    class WhenShowing {

        @Test
        void returnsModelDetails_whenOllamaResponds() {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""
                            {"modelfile":"FROM llama3","parameters":"num_ctx 4096","template":"{{ .Prompt }}"}
                            """));

            byte[] requestBody = """
                    {"model":"llama3"}
                    """.getBytes();

            StepVerifier.create(adapter.show(Flux.just(requestBody), new HttpHeaders()))
                    .expectNextMatches(bytes -> new String(bytes).contains("\"modelfile\""))
                    .verifyComplete();
        }

        @Test
        void errorsOut_whenOllamaIsDown() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));

            StepVerifier.create(adapter.show(Flux.just("{}".getBytes()), new HttpHeaders()))
                    .expectError(OllamaException.class)
                    .verify();
        }
    }
}