package com.zoltraak.gateway.domain.shared;

import com.zoltraak.gateway.domain.enums.GatewayErrorCode;
import lombok.Getter;

@Getter
public class GatewayServiceException extends ZoltraakGatewayException {

    private final GatewayErrorCode code;

    public GatewayServiceException(GatewayErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
