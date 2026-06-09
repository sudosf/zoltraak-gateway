package com.zoltraak.gateway.domain.exception;

import com.zoltraak.gateway.domain.enums.GatewayErrorCode;
import com.zoltraak.gateway.domain.enums.PodStatus;
import lombok.Getter;

@Getter
public class PodNotReadyException extends GatewayServiceException {

    private final PodStatus status;

    public PodNotReadyException(PodStatus status, GatewayErrorCode code, String message) {
        super(code, message);
        this.status = status;
    }
}
