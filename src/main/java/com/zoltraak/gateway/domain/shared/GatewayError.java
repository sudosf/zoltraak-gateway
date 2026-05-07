package com.zoltraak.gateway.domain.shared;

import com.zoltraak.gateway.domain.enums.GatewayErrorCode;

public record GatewayError(
        GatewayErrorCode code,
        String message
) {
}
