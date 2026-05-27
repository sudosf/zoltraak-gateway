package com.zoltraak.gateway.features.proxy;

import com.zoltraak.gateway.adapters.ollama.OllamaPort;
import com.zoltraak.gateway.domain.models.ollama.OllamaTagsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class OllamaProxyController {

    private final OllamaPort ollamaPort;

    public OllamaProxyController(OllamaPort ollamaPort) {
        this.ollamaPort = ollamaPort;
    }

    @GetMapping("/tags")
    public Mono<ResponseEntity<OllamaTagsResponse>> getTags() {
        return ollamaPort.getTags()
                .map(ResponseEntity::ok);
    }
}
