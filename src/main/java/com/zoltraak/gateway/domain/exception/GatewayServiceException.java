package com.zoltraak.gateway.domain.exception;

import com.zoltraak.gateway.domain.enums.GatewayErrorCode;
import lombok.Getter;

@Getter
public class GatewayServiceException extends ZoltraakGatewayException {

    private final GatewayErrorCode gatewayErrorCode;

    public GatewayServiceException(GatewayErrorCode code, String message) {
        super(message);
        this.gatewayErrorCode = code;
    }
}
