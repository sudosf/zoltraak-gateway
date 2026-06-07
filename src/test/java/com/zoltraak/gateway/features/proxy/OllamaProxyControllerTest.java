package com.zoltraak.gateway.features.proxy;

import com.zoltraak.gateway.config.SecurityConfig;
import com.zoltraak.gateway.domain.models.ollama.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

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
    void chat_returns200_withNdjsonContentType() {
        OllamaChatRequest request = new OllamaChatRequest(
                "llama3", List.of(), false, false
        );

        OllamaChatResponse response = new OllamaChatResponse(
                "llama3", null, null,
                true, null, null,
                null, null, null,
                null, null
        );

        when(ollamaProxyService.forwardChat(request)).thenReturn(Flux.just(response));

        webTestClient.post()
                .uri(OllamaProxyController.BASE_PATH + OllamaProxyController.CHAT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.valueOf("application/x-ndjson"));

        verify(ollamaProxyService).forwardChat(any(OllamaChatRequest.class));
    }

    @Test
    void generate_returns200_withNdjsonContentType() {
        OllamaGenerateRequest request = new OllamaGenerateRequest(
                "llama3", "hello", List.of(), false, false, null
        );

        OllamaGenerateResponse response = new OllamaGenerateResponse(
                "llama3", null, "response text",
                null, true, null
        );

        when(ollamaProxyService.forwardGenerate(request)).thenReturn(Flux.just(response));

        webTestClient.post()
                .uri(OllamaProxyController.BASE_PATH + OllamaProxyController.GENERATE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.valueOf("application/x-ndjson"));

        verify(ollamaProxyService).forwardGenerate(any(OllamaGenerateRequest.class));
    }

    @Test
    void getTags_returns200() {
        OllamaModelsResponse response = new OllamaModelsResponse(List.of());

        when(ollamaProxyService.getTags()).thenReturn(Mono.just(response));

        webTestClient.get()
                .uri(OllamaProxyController.BASE_PATH + OllamaProxyController.TAGS)
                .exchange()
                .expectStatus().isOk();

        verify(ollamaProxyService).getTags();
    }

    @Test
    void getVersion_returns200() {
        OllamaVersionResponse response = new OllamaVersionResponse("0.1.0");

        when(ollamaProxyService.getVersion()).thenReturn(Mono.just(response));

        webTestClient.get()
                .uri(OllamaProxyController.BASE_PATH + OllamaProxyController.VERSION)
                .exchange()
                .expectStatus().isOk();

        verify(ollamaProxyService).getVersion();
    }

    @Test
    void getPs_returns200() {
        OllamaModelsResponse response = new OllamaModelsResponse(List.of());

        when(ollamaProxyService.getPs()).thenReturn(Mono.just(response));

        webTestClient.get()
                .uri(OllamaProxyController.BASE_PATH + OllamaProxyController.PS)
                .exchange()
                .expectStatus().isOk();

        verify(ollamaProxyService).getPs();
    }
}
