package com.zoltraak.gateway.domain.response;

import com.zoltraak.gateway.domain.enums.GatewayErrorCode;
import com.zoltraak.gateway.domain.error.GatewayError;

public record GatewayResponse<T>(
        T data,
        GatewayError error,
        ResponseMeta meta
) {
    public static <T> GatewayResponse<T> success(T data) {
        return new GatewayResponse<>(data, null, ResponseMeta.now());
    }

    public static <T> GatewayResponse<T> error(GatewayErrorCode code, String message) {
        return new GatewayResponse<>(null, new GatewayError(code, message), ResponseMeta.now());
    }
}