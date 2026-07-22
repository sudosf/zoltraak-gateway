package com.zoltraak.gateway.features.proxy;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping({OllamaProxyController.NATIVE_BASE_PATH, OllamaProxyController.OPENAI_BASE_PATH})
public class OllamaProxyController {

    public static final String NATIVE_BASE_PATH = "/api";
    public static final String OPENAI_BASE_PATH = "/v1";
    public static final String CHAT = "/chat";
    public static final String CHAT_COMPLETIONS = "/chat/completions";
    public static final String GENERATE = "/generate";
    public static final String EMBED = "/embed";
    public static final String SHOW = "/show";
    public static final String VERSION = "/version";
    public static final String PS = "/ps";
    public static final String TAGS = "/tags";

    private final OllamaProxyService ollamaProxyService;

    public OllamaProxyController(OllamaProxyService ollamaProxyService) {
        this.ollamaProxyService = ollamaProxyService;
    }

    @PostMapping(value = CHAT, produces = {MediaType.TEXT_EVENT_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public Flux<byte[]> chat(@RequestBody Flux<byte[]> request, HttpHeaders headers) {
        return ollamaProxyService.forwardChat(request, headers);
    }

    @PostMapping(value = CHAT_COMPLETIONS, produces = {MediaType.TEXT_EVENT_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public Flux<byte[]> chatCompletions(@RequestBody Flux<byte[]> request, HttpHeaders headers) {
        return ollamaProxyService.forwardChat(request, headers);
    }

    @PostMapping(value = GENERATE, produces = {MediaType.APPLICATION_NDJSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public Flux<byte[]> generate(@RequestBody Flux<byte[]> request, HttpHeaders headers) {
        return ollamaProxyService.forwardGenerate(request, headers);
    }

    @PostMapping(EMBED)
    public Mono<byte[]> embed(@RequestBody Flux<byte[]> request, @RequestHeader HttpHeaders headers) {
        return ollamaProxyService.embed(request, headers);
    }

    @PostMapping(SHOW)
    public Mono<byte[]> show(@RequestBody Flux<byte[]> request, @RequestHeader HttpHeaders headers) {
        return ollamaProxyService.show(request, headers);
    }

    @GetMapping(TAGS)
    public Mono<byte[]> getTags(HttpHeaders headers) {
        return ollamaProxyService.getTags(headers);
    }

    @GetMapping(VERSION)
    public Mono<byte[]> getVersion(HttpHeaders headers) {
        return ollamaProxyService.getVersion(headers);
    }

    @GetMapping(PS)
    public Mono<byte[]> getPs(HttpHeaders headers) {
        return ollamaProxyService.getPs(headers);
    }
}
