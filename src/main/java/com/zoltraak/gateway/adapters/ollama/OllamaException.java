package com.zoltraak.gateway.adapters.ollama;

import com.zoltraak.gateway.domain.shared.ZoltraakGatewayException;
import lombok.Getter;

@Getter
public class OllamaException extends ZoltraakGatewayException {
    private final String modelName;
    private final String reason;

    public OllamaException(String modelName, String reason, String message) {
        super(message);
        this.modelName = modelName;
        this.reason = reason;
    }
}
