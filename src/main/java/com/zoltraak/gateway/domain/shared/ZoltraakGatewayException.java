package com.zoltraak.gateway.domain.shared;

public class ZoltraakGatewayException extends RuntimeException {

    public ZoltraakGatewayException(String message) {
        super(message);
    }

    public ZoltraakGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
