package com.zoltraak.gateway.adapters.gpu;

import com.zoltraak.gateway.config.properties.OllamaProperties;
import com.zoltraak.gateway.config.properties.ProviderProperties;
import com.zoltraak.gateway.domain.enums.PodStatus;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = "http://localhost:" + mockWebServer.getPort();

        ProviderProperties.VastAiConfig vastAiConfig = mock(ProviderProperties.VastAiConfig.class);
        when(vastAiConfig.getInstanceId()).thenReturn("test-instance-123");

        ProviderProperties providerProperties = mock(ProviderProperties.class);
        when(providerProperties.getVastAi()).thenReturn(vastAiConfig);

        OllamaProperties.GpuPodConfig gpuPodConfig = mock(OllamaProperties.GpuPodConfig.class);
        when(gpuPodConfig.getPort()).thenReturn(11434);

        OllamaProperties ollamaProperties = mock(OllamaProperties.class);
        when(ollamaProperties.getGpuPod()).thenReturn(gpuPodConfig);

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();

        adapter = new VastAiAdapter(providerProperties, webClient, ollamaProperties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void start_sendsRunningState_toVastAi() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        StepVerifier.create(adapter.start())
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getBody().readUtf8()).contains("\"state\":\"running\"");
    }

    @Test
    void start_throwsProviderException_whenVastAiErrors() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(adapter.start())
                .expectError(ProviderException.class)
                .verify();
    }

    @Test
    void stop_sendsStoppedState_toVastAi() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        StepVerifier.create(adapter.stop())
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getBody().readUtf8()).contains("\"state\":\"stopped\"");
    }

    @Test
    void stop_throwsProviderException_whenVastAiErrors() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(adapter.stop())
                .expectError(ProviderException.class)
                .verify();
    }

    @Test
    void getStatus_returnsReady_whenInstanceIsRunning() {
        String json = """
                {"instances": {"actual_status": "running", "public_ipaddr": "1.2.3.4"}}
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        );

        StepVerifier.create(adapter.getStatus())
                .expectNext(PodStatus.READY)
                .verifyComplete();
    }

    @Test
    void getStatus_returnsWarming_whenInstanceIsLoading() {
        String json = """
                {"instances": {"actual_status": "loading", "public_ipaddr": "1.2.3.4"}}
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        );

        StepVerifier.create(adapter.getStatus())
                .expectNext(PodStatus.WARMING)
                .verifyComplete();
    }

    @Test
    void getStatus_returnsStopped_whenInstanceStatusIsUnrecognised() {
        String json = """
                {"instances": {"actual_status": "exited", "public_ipaddr": "1.2.3.4"}}
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        );

        StepVerifier.create(adapter.getStatus())
                .expectNext(PodStatus.STOPPED)
                .verifyComplete();
    }

    @Test
    void getStatus_returnsStarting_whenActualStatusIsNull() {
        String json = """
                {"instances": {"actual_status": null, "public_ipaddr": "1.2.3.4"}}
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        );

        StepVerifier.create(adapter.getStatus())
                .expectNext(PodStatus.STARTING)
                .verifyComplete();
    }

    @Test
    void getStatus_throwsProviderException_whenResponseIsEmpty() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("")
        );

        StepVerifier.create(adapter.getStatus())
                .expectError(ProviderException.class)
                .verify();
    }

    @Test
    void getStatus_throwsProviderException_whenVastAiErrors() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(adapter.getStatus())
                .expectError(ProviderException.class)
                .verify();
    }

    @Test
    void getConnectionDetails_returnsCorrectUrl_whenInstanceIsRunning() {
        String json = """
                {"instances": {"actual_status": "running", "public_ipaddr": "1.2.3.4"}}
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        );

        StepVerifier.create(adapter.getConnectionDetails())
                .expectNextMatches(details ->
                        details.ollamaUrl().equals("http://1.2.3.4:11434"))
                .verifyComplete();
    }

    @Test
    void getConnectionDetails_throwsProviderException_whenResponseIsEmpty() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("")
        );

        StepVerifier.create(adapter.getConnectionDetails())
                .expectError(ProviderException.class)
                .verify();
    }

    @Test
    void getConnectionDetails_throwsProviderException_whenVastAiErrors() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(adapter.getConnectionDetails())
                .expectError(ProviderException.class)
                .verify();
    }
}