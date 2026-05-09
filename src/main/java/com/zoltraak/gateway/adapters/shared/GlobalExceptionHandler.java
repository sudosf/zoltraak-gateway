package com.zoltraak.gateway.adapters.shared;

import com.zoltraak.gateway.adapters.gpu.ProviderException;
import com.zoltraak.gateway.domain.enums.GatewayErrorCode;
import com.zoltraak.gateway.domain.shared.GatewayResponse;
import com.zoltraak.gateway.domain.shared.GatewayServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProviderException.class)
    public ResponseEntity<GatewayResponse<Void>> handleProviderException(ProviderException ex) {
        ex.printStackTrace(); // TODO: remove in production
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(GatewayResponse.error(
                        GatewayErrorCode.PROVIDER_UNAVAILABLE,
                        "GPU provider is currently unavailable"));
    }

    @ExceptionHandler(GatewayServiceException.class)
    public ResponseEntity<GatewayResponse<Void>> handleServiceException(GatewayServiceException ex) {
        ex.printStackTrace(); // TODO: remove in production
        return ResponseEntity
                .status(mapToHttpStatus(ex.getCode()))
                .body(GatewayResponse.error(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<GatewayResponse<Void>> handleThrowable(Throwable ex) {
        ex.printStackTrace(); // TODO: remove in production
        return ResponseEntity
                .internalServerError()
                .body(GatewayResponse.error(
                        GatewayErrorCode.INTERNAL_ERROR,
                        "An unexpected error occurred"));
    }

    private HttpStatus mapToHttpStatus(GatewayErrorCode code) {
        return switch (code) {
            case INVALID_TOKEN, TOKEN_EXPIRED -> HttpStatus.UNAUTHORIZED;
            case POD_START_FAILED, WARMUP_TIMEOUT,
                 QUEUE_TIMEOUT, PROVIDER_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}