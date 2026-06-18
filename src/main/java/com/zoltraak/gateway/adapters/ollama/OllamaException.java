package com.zoltraak.gateway.adapters.ollama;

import com.zoltraak.gateway.domain.exception.ZoltraakGatewayException;
import lombok.Getter;

@Getter
public class OllamaException extends ZoltraakGatewayException {
    private final int statusCode;
    private final byte[] body;

    public OllamaException(int statusCode, byte[] body) {
        super("Ollama call failed with status " + statusCode);
        this.statusCode = statusCode;
        this.body = body;
    }
}
