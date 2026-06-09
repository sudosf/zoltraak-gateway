package com.zoltraak.gateway.domain.exception;

import com.zoltraak.gateway.domain.enums.GatewayErrorCode;

public class RequestExpiredException extends GatewayServiceException {
    public RequestExpiredException(GatewayErrorCode code, String message) {
        super(code, message);
    }
}
