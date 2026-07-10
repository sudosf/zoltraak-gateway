package com.zoltraak.gateway.features.proxy;

import com.zoltraak.gateway.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(OllamaProxyController.class)
@Import(SecurityConfig.class)
class OllamaProxyControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OllamaProxyService ollamaProxyService;

    @Test
    void chat_returns200_withEventStreamContentType() {
        byte[] requestBody = "{\"model\":\"llama3\",\"messages\":[],\"stream\":false}".getBytes();
        byte[] responseBody = "{\"model\":\"llama3\",\"done\":true}".getBytes();

        when(ollamaProxyService.forwardChat(any(), any(HttpHeaders.class)))
                .thenReturn(Flux.just(responseBody));

        webTestClient.post()
                .uri(OllamaProxyController.BASE_PATH + OllamaProxyController.CHAT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.valueOf(MediaType.TEXT_EVENT_STREAM_VALUE));

        verify(ollamaProxyService).forwardChat(any(), any(HttpHeaders.class));
    }

    @Test
    void generate_returns200_withNdjsonContentType() {
        byte[] requestBody = "{\"model\":\"llama3\",\"prompt\":\"hello\",\"stream\":false}".getBytes();
        byte[] responseBody = "{\"model\":\"llama3\",\"response\":\"response text\",\"done\":true}".getBytes();

        when(ollamaProxyService.forwardGenerate(any(), any(HttpHeaders.class)))
                .thenReturn(Flux.just(responseBody));

        webTestClient.post()
                .uri(OllamaProxyController.BASE_PATH + OllamaProxyController.GENERATE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.valueOf(MediaType.APPLICATION_NDJSON_VALUE));

        verify(ollamaProxyService).forwardGenerate(any(), any(HttpHeaders.class));
    }

    @Test
    void embed_returns200() {
        byte[] requestBody = "{\"model\":\"embeddinggemma\",\"input\":\"hello\"}".getBytes();
        byte[] responseBody = "{\"model\":\"embeddinggemma\",\"embeddings\":[[0.01,0.02]]}".getBytes();

        when(ollamaProxyService.embed(any(), any(HttpHeaders.class))).thenReturn(Mono.just(responseBody));

        webTestClient.post()
                .uri(OllamaProxyController.BASE_PATH + OllamaProxyController.EMBED)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk();

        verify(ollamaProxyService).embed(any(), any(HttpHeaders.class));
    }

    @Test
    void show_returns200() {
        byte[] requestBody = "{\"model\":\"llama3\"}".getBytes();
        byte[] responseBody = "{\"modelfile\":\"FROM llama3\"}".getBytes();

        when(ollamaProxyService.show(any(), any(HttpHeaders.class))).thenReturn(Mono.just(responseBody));

        webTestClient.post()
                .uri(OllamaProxyController.BASE_PATH + OllamaProxyController.SHOW)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk();

        verify(ollamaProxyService).show(any(), any(HttpHeaders.class));
    }

    @Test
    void getTags_returns200() {
        byte[] response = "{\"models\":[]}".getBytes();

        when(ollamaProxyService.getTags(any(HttpHeaders.class))).thenReturn(Mono.just(response));

        webTestClient.get()
                .uri(OllamaProxyController.BASE_PATH + OllamaProxyController.TAGS)
                .exchange()
                .expectStatus().isOk();

        verify(ollamaProxyService).getTags(any(HttpHeaders.class));
    }

    @Test
    void getVersion_returns200() {
        byte[] response = "{\"version\":\"0.1.0\"}".getBytes();

        when(ollamaProxyService.getVersion(any(HttpHeaders.class))).thenReturn(Mono.just(response));

        webTestClient.get()
                .uri(OllamaProxyController.BASE_PATH + OllamaProxyController.VERSION)
                .exchange()
                .expectStatus().isOk();

        verify(ollamaProxyService).getVersion(any(HttpHeaders.class));
    }

    @Test
    void getPs_returns200() {
        byte[] response = "{\"models\":[]}".getBytes();

        when(ollamaProxyService.getPs(any(HttpHeaders.class))).thenReturn(Mono.just(response));

        webTestClient.get()
                .uri(OllamaProxyController.BASE_PATH + OllamaProxyController.PS)
                .exchange()
                .expectStatus().isOk();

        verify(ollamaProxyService).getPs(any(HttpHeaders.class));
    }
}