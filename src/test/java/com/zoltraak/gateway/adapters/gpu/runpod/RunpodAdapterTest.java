package com.zoltraak.gateway.adapters.gpu.runpod;

import com.zoltraak.gateway.adapters.gpu.ProviderException;
import com.zoltraak.gateway.config.properties.OllamaProperties;
import com.zoltraak.gateway.config.properties.ProviderProperties;
import com.zoltraak.gateway.domain.enums.GpuProviderType;
import com.zoltraak.gateway.domain.enums.PodStatus;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Exceptions;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RunpodAdapterTest {
    private final String POD_ID = "pod-12345";

    private MockWebServer mockWebServer;
    private RunpodAdapter adapter;
    private OllamaProperties ollamaProperties;
    private ProviderProperties providerProperties;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = "http://localhost:" + mockWebServer.getPort();

        OllamaProperties.GpuPodConfig gpuPodConfig = mock(OllamaProperties.GpuPodConfig.class);
        when(gpuPodConfig.getPort()).thenReturn(11434);

        this.ollamaProperties = mock(OllamaProperties.class);
        when(ollamaProperties.getGpuPod()).thenReturn(gpuPodConfig);

        ProviderProperties.RunPod.Create create = mock(ProviderProperties.RunPod.Create.class);
        ProviderProperties.RunPod runPod = mock(ProviderProperties.RunPod.class);
        when(runPod.getCreate()).thenReturn(create);

        this.providerProperties = mock(ProviderProperties.class);
        when(providerProperties.getRunpod()).thenReturn(runPod);

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();

        adapter = new RunpodAdapter(webClient, ollamaProperties, providerProperties);
        adapter.init();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    private void enqueuePodListPage(String id) {
        String json = """
                [
                    {
                        "id": "%s",
                        "desiredStatus": "RUNNING"
                    }
                ]
                """.formatted(id);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json));
    }

    private void enqueuePodListPage() {
        enqueuePodListPage(POD_ID);
    }

    private void enqueuePodResponse(String desiredStatus, String podId) {
        String statusValue = desiredStatus.equals("null")
                ? "null"
                : "\"" + desiredStatus + "\"";

        String json = """
                {
                    "id": "%s",
                    "desiredStatus": %s,
                    "costPerHr": 0.5,
                    "machine": {
                        "gpuTypeId": "RTX3090",
                        "dataCenterId": "US-1"
                    }
                }
                """.formatted(podId, statusValue);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json));
    }

    private void enqueuePodResponse(String desiredStatus) {
        enqueuePodResponse(desiredStatus, POD_ID);
    }

    @Nested
    class WhenInitializationFails {

        @Test
        void throwsProviderException_whenInitializationErrors() throws IOException {
            try (MockWebServer localServer = new MockWebServer()) {
                RunpodAdapter brokenAdapter = buildIsolatedAdapter(localServer,
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
                        .setResponseCode(404)
                        .setHeader("Content-Type", "application/json")
                        .setBody("");

                RunpodAdapter brokenAdapter = buildIsolatedAdapter(localServer, emptyResponse);

                StepVerifier.create(brokenAdapter.getConnectionDetails())
                        .expectError(ProviderException.class)
                        .verify();
            }
        }

        @Test
        void throwsProviderException_whenPodListIsEmpty() throws IOException {
            try (MockWebServer localServer = new MockWebServer()) {
                MockResponse emptyArrayResponse = new MockResponse()
                        .setResponseCode(404)
                        .setHeader("Content-Type", "application/json")
                        .setBody("[]");

                RunpodAdapter missingPodAdapter = buildIsolatedAdapter(localServer, emptyArrayResponse);

                StepVerifier.create(missingPodAdapter.getConnectionDetails())
                        .expectErrorMatches(throwable -> throwable instanceof ProviderException ex
                                && ex.getHttpStatusCode() == 404
                                && ex.getProvider() == GpuProviderType.RUNPOD)
                        .verify();
            }
        }

        private RunpodAdapter buildIsolatedAdapter(MockWebServer localServer, MockResponse response) throws IOException {
            localServer.start();
            localServer.enqueue(response);

            WebClient localClient = WebClient.builder()
                    .baseUrl("http://localhost:" + localServer.getPort())
                    .build();

            RunpodAdapter isolatedAdapter = new RunpodAdapter(localClient, ollamaProperties, providerProperties);
            isolatedAdapter.init();
            return isolatedAdapter;
        }
    }

    @Nested
    class WhenStartingPod {

        @BeforeEach
        void setUp() {
            enqueuePodListPage();
        }

        @Test
        void sendsStartRequest_toRunpod() throws InterruptedException {
            mockWebServer.enqueue(new MockResponse().setResponseCode(200));

            StepVerifier.create(adapter.start())
                    .verifyComplete();

            mockWebServer.takeRequest();
            RecordedRequest request = mockWebServer.takeRequest();
            assertThat(request.getMethod()).isEqualTo("POST");
            assertThat(request.getPath()).isEqualTo("/pods/pod-12345/start");
        }

        @Nested
        class WhenStartFails {

            @Test
            void rentsNewPod_whenPodNotFound() throws InterruptedException {
                mockWebServer.enqueue(new MockResponse().setResponseCode(404));
                enqueuePodResponse("RUNNING");

                StepVerifier.create(adapter.start())
                        .verifyComplete();

                mockWebServer.takeRequest();
                RecordedRequest startRequest = mockWebServer.takeRequest();
                RecordedRequest createRequest = mockWebServer.takeRequest();

                assertThat(startRequest.getPath()).isEqualTo("%s/%s/start".formatted(RunpodAdapter.BASE_PATH, POD_ID));
                assertThat(createRequest.getPath()).isEqualTo(RunpodAdapter.BASE_PATH);
            }

            @Test
            @Disabled("pending optimization for retry delay")
            void propagatesError_whenRentingNewPodExhaustsRetries() {
                mockWebServer.enqueue(new MockResponse().setResponseCode(404));
                for (int i = 0; i < 4; i++) {
                    mockWebServer.enqueue(new MockResponse().setResponseCode(500));
                }

                StepVerifier.create(adapter.start())
                        .expectErrorMatches(throwable ->
                                Exceptions.isRetryExhausted(throwable)
                                        && throwable.getCause() instanceof ProviderException)
                        .verify(Duration.ofSeconds(10));
            }

            @Test
            void deletesAndRentsNewPod_whenStartFails_andDeleteSucceeds() {
                mockWebServer.enqueue(new MockResponse().setResponseCode(500));
                mockWebServer.enqueue(new MockResponse().setResponseCode(200));
                enqueuePodResponse("RUNNING");

                StepVerifier.create(adapter.start())
                        .verifyComplete();
            }

            @Test
            void rentsNewPod_whenStartFails_andDeleteAlsoFails() {
                mockWebServer.enqueue(new MockResponse().setResponseCode(500));
                mockWebServer.enqueue(new MockResponse().setResponseCode(500));
                enqueuePodResponse("RUNNING");

                StepVerifier.create(adapter.start())
                        .verifyComplete();
            }

            @Test
            @Disabled("pending optimization for retry delay")
            void propagatesError_whenDeleteSucceeds_whenRentingNewPodExhaustsRetries() {
                mockWebServer.enqueue(new MockResponse().setResponseCode(500));
                mockWebServer.enqueue(new MockResponse().setResponseCode(200));
                for (int i = 0; i < 4; i++) {
                    mockWebServer.enqueue(new MockResponse().setResponseCode(500));
                }

                StepVerifier.create(adapter.start())
                        .expectErrorMatches(throwable ->
                                Exceptions.isRetryExhausted(throwable)
                                        && throwable.getCause() instanceof ProviderException)
                        .verify(Duration.ofSeconds(10));
            }
        }

    }

    @Nested
    class WhenStoppingPod {

        @BeforeEach
        void setUp() {
            enqueuePodListPage();
        }

        @Test
        void sendsStopRequest_toRunpod() throws InterruptedException {
            mockWebServer.enqueue(new MockResponse().setResponseCode(200));

            StepVerifier.create(adapter.stop())
                    .verifyComplete();

            mockWebServer.takeRequest();
            RecordedRequest request = mockWebServer.takeRequest();
            assertThat(request.getMethod()).isEqualTo("POST");
            assertThat(request.getPath()).isEqualTo("/pods/pod-12345/stop");
        }
    }

    @Nested
    class WhenGettingStatus {

        @BeforeEach
        void setUp() {
            enqueuePodListPage();
        }

        @Test
        void returnsWarming_whenDesiredStatusIsRunning() {
            enqueuePodResponse("RUNNING");

            StepVerifier.create(adapter.getStatus())
                    .expectNext(PodStatus.WARMING)
                    .verifyComplete();
        }

        @Test
        void returnsStopped_whenDesiredStatusIsExited() {
            enqueuePodResponse("EXITED");

            StepVerifier.create(adapter.getStatus())
                    .expectNext(PodStatus.STOPPED)
                    .verifyComplete();
        }

        @Test
        void returnsStopped_whenDesiredStatusIsTerminated() {
            enqueuePodResponse("TERMINATED");

            StepVerifier.create(adapter.getStatus())
                    .expectNext(PodStatus.STOPPED)
                    .verifyComplete();
        }

        @Test
        void returnsStarting_whenDesiredStatusIsNull() {
            enqueuePodResponse("null");

            StepVerifier.create(adapter.getStatus())
                    .expectNext(PodStatus.STARTING)
                    .verifyComplete();
        }

        @Test
        void returnsStopped_whenDesiredStatusIsUnrecognised() {
            enqueuePodResponse("SOME_UNKNOWN_STATUS");

            StepVerifier.create(adapter.getStatus())
                    .expectNext(PodStatus.STOPPED)
                    .verifyComplete();
        }

        @Test
        void throwsProviderException_whenPodWasDeletedExternally() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(404));
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(404)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[]"));

            StepVerifier.create(adapter.getStatus())
                    .expectError(ProviderException.class)
                    .verify();
        }
    }

    @Nested
    class WhenGettingConnectionDetails {

        @BeforeEach
        void setUp() {
            enqueuePodListPage();
        }

        @Test
        void returnsCorrectUrl_whenPodIsRunning() {
            enqueuePodResponse("RUNNING");

            StepVerifier.create(adapter.getConnectionDetails())
                    .expectNextMatches(details ->
                            details.ollamaUrl().equals("https://pod-12345-11434.proxy.runpod.net"))
                    .verifyComplete();
        }

        @Test
        void recoversViaRefresh_whenPodWasDeletedExternally() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(404));
            enqueuePodListPage("pod-67890");
            enqueuePodResponse("RUNNING", "pod-67890");

            StepVerifier.create(adapter.getConnectionDetails())
                    .expectNextMatches(details ->
                            details.ollamaUrl().equals("https://pod-67890-11434.proxy.runpod.net"))
                    .verifyComplete();
        }

        @Test
        void throwsProviderException_whenPodWasDeletedExternally() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(404));
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("[]"));

            StepVerifier.create(adapter.getConnectionDetails())
                    .expectError(ProviderException.class)
                    .verify();
        }
    }
}
