package com.zoltraak.gateway.features.proxy;

import com.zoltraak.gateway.domain.models.ollama.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(OllamaProxyController.BASE_PATH)
public class OllamaProxyController {

    public static final String BASE_PATH = "/api";
    public static final String CHAT = "/chat";
    public static final String GENERATE = "/generate";
    public static final String VERSION = "/version";
    public static final String PS = "/ps";
    public static final String TAGS = "/tags";

    private final OllamaProxyService ollamaProxyService;

    public OllamaProxyController(OllamaProxyService ollamaProxyService) {
        this.ollamaProxyService = ollamaProxyService;
    }

    @PostMapping(value = CHAT, produces = "application/x-ndjson")
    public Flux<OllamaChatResponse> chat(@RequestBody OllamaChatRequest request) {
        return ollamaProxyService.forwardChat(request);
    }

    @PostMapping(value = GENERATE, produces = "application/x-ndjson")
    public Flux<OllamaGenerateResponse> generate(@RequestBody OllamaGenerateRequest request) {
        return ollamaProxyService.forwardGenerate(request);
    }

    @GetMapping(TAGS)
    public Mono<ResponseEntity<OllamaModelsResponse>> getTags() {
        return ollamaProxyService.getTags().map(ResponseEntity::ok);
    }

    @GetMapping(VERSION)
    public Mono<ResponseEntity<OllamaVersionResponse>> getVersion() {
        return ollamaProxyService.getVersion().map(ResponseEntity::ok);
    }

    @GetMapping(PS)
    public Mono<ResponseEntity<OllamaModelsResponse>> getPs() {
        return ollamaProxyService.getPs().map(ResponseEntity::ok);
    }
}
