package com.zoltraak.gateway.domain.error;

import com.zoltraak.gateway.domain.enums.GatewayErrorCode;

public record GatewayError(
        GatewayErrorCode code,
        String message
) {
}
