package com.zoltraak.gateway.adapters.gpu;

import com.zoltraak.gateway.config.properties.OllamaProperties;
import com.zoltraak.gateway.domain.enums.GpuProvider;
import com.zoltraak.gateway.domain.enums.PodStatus;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VastAiAdapterTest {

    private MockWebServer mockWebServer;
    private VastAiAdapter adapter;
    private OllamaProperties ollamaProperties;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = "http://localhost:" + mockWebServer.getPort();

        OllamaProperties.GpuPodConfig gpuPodConfig = mock(OllamaProperties.GpuPodConfig.class);
        when(gpuPodConfig.getPort()).thenReturn(11434);

        this.ollamaProperties = mock(OllamaProperties.class);
        when(ollamaProperties.getGpuPod()).thenReturn(gpuPodConfig);

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();

        adapter = new VastAiAdapter(webClient, ollamaProperties);
        adapter.init();
        enqueueInstancePage();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    private void enqueueInstancePage() {
        String json = """
                {
                    "instances": [
                        {
                            "id": 12345,
                            "actual_status": "null",
                            "public_ipaddr": "1.2.3.4",
                            "ports": {
                                "11434/tcp": [{"hostIp": "0.0.0.0", "hostPort": "55000"}]
                            }
                        }
                    ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json));
    }

    private void enqueueInstanceResponse(String actualStatus) {
        String statusValue = actualStatus.equals("null")
                ? "null"
                : "\"" + actualStatus + "\"";

        String json = """
                {
                    "instances": {
                            "id": 12345,
                            "actual_status": %s,
                            "public_ipaddr": "1.2.3.4",
                            "ports": {
                                "11434/tcp": [{"hostIp": "0.0.0.0", "hostPort": "55000"}]
                            }
                        }
                }
                """.formatted(statusValue);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json));
    }

    @Nested
    class WhenInitializationFails {
        @Test
        void throwsProviderException_whenInitializationErrors() throws IOException {
            try (MockWebServer localServer = new MockWebServer()) {
                VastAiAdapter brokenAdapter = buildIsolatedAdapter(localServer,
                        new MockResponse().setResponseCode(500));

                StepVerifier.create(brokenAdapter.getConnectionDetails())
                        .expectError(ProviderException.class)
                        .verify();
            }
        }

        @Test
        void throwsProviderException_whenResponseIsEmpty() throws IOException {
            try (MockWebServer localServer = new MockWebServer()) {
                MockResponse emptyResponse = new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("");

                VastAiAdapter brokenAdapter = buildIsolatedAdapter(localServer, emptyResponse);

                StepVerifier.create(brokenAdapter.getConnectionDetails())
                        .expectError(ProviderException.class)
                        .verify();
            }
        }

        @Test
        void throwsProviderException_whenInstanceListIsEmpty() throws IOException {
            try (MockWebServer localServer = new MockWebServer()) {
                String emptyListJson = """
                        {
                            "instances": []
                        }
                        """;

                MockResponse emptyArrayResponse = new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(emptyListJson);

                VastAiAdapter missingPodAdapter = buildIsolatedAdapter(localServer, emptyArrayResponse);

                StepVerifier.create(missingPodAdapter.getConnectionDetails())
                        .expectErrorMatches(throwable -> throwable instanceof ProviderException ex
                                && ex.getHttpStatusCode() == 404
                                && ex.getProvider() == GpuProvider.VASTAI)
                        .verify();
            }
        }

        private VastAiAdapter buildIsolatedAdapter(MockWebServer localServer, MockResponse response) throws IOException {
            localServer.start();
            localServer.enqueue(response);

            WebClient localClient = WebClient.builder()
                    .baseUrl("http://localhost:" + localServer.getPort())
                    .build();

            VastAiAdapter isolatedAdapter = new VastAiAdapter(localClient, ollamaProperties);
            isolatedAdapter.init();
            return isolatedAdapter;
        }
    }


    @Nested
    class WhenStartingPod {

        @Test
        void sendsRunningState_toVastAi() throws InterruptedException {
            mockWebServer.enqueue(new MockResponse().setResponseCode(200));

            StepVerifier.create(adapter.start())
                    .verifyComplete();

            mockWebServer.takeRequest();
            RecordedRequest request = mockWebServer.takeRequest();
            assertThat(request.getBody().readUtf8()).contains("\"state\":\"running\"");
        }
    }

    @Nested
    class WhenStoppingPod {

        @Test
        void sendsStoppedState_toVastAi() throws InterruptedException {
            mockWebServer.enqueue(new MockResponse().setResponseCode(200));

            StepVerifier.create(adapter.stop())
                    .verifyComplete();

            mockWebServer.takeRequest();
            RecordedRequest request = mockWebServer.takeRequest();
            assertThat(request.getBody().readUtf8()).contains("\"state\":\"stopped\"");
        }
    }

    @Nested
    class WhenGettingStatus {

        @Test
        void returnsReady_whenInstanceIsRunning() {
            enqueueInstanceResponse("running");

            StepVerifier.create(adapter.getStatus())
                    .expectNext(PodStatus.READY)
                    .verifyComplete();
        }

        @Test
        void returnsWarming_whenInstanceIsLoading() {
            enqueueInstanceResponse("loading");

            StepVerifier.create(adapter.getStatus())
                    .expectNext(PodStatus.WARMING)
                    .verifyComplete();
        }

        @Test
        void returnsStopped_whenInstanceStatusIsUnrecognised() {
            enqueueInstanceResponse("exited");

            StepVerifier.create(adapter.getStatus())
                    .expectNext(PodStatus.STOPPED)
                    .verifyComplete();
        }

        @Test
        void returnsStarting_whenActualStatusIsNull() {
            enqueueInstanceResponse("null");

            StepVerifier.create(adapter.getStatus())
                    .expectNext(PodStatus.STARTING)
                    .verifyComplete();
        }
    }

    @Nested
    class WhenGettingConnectionDetails {

        @Test
        void returnsCorrectUrl_whenInstanceIsRunning() {
            StepVerifier.create(adapter.getConnectionDetails())
                    .expectNextMatches(details ->
                            details.ollamaUrl().equals("http://1.2.3.4:55000"))
                    .verifyComplete();
        }
    }
}