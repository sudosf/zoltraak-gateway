package com.zoltraak.gateway.domain.shared;

public record GatewayResponse<T>(
        T data,
        GatewayError error,
        ResponseMeta meta
) {
}
