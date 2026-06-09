package com.zoltraak.gateway.exception;

import com.zoltraak.gateway.adapters.gpu.ProviderException;
import com.zoltraak.gateway.domain.enums.GatewayErrorCode;
import com.zoltraak.gateway.domain.exception.GatewayServiceException;
import com.zoltraak.gateway.domain.exception.PodNotReadyException;
import com.zoltraak.gateway.domain.response.GatewayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProviderException.class)
    public ResponseEntity<GatewayResponse<Void>> handleProviderException(ProviderException ex) {
        log.error("Provider error code={} message={}", ex.getHttpStatusCode(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(GatewayResponse.error(
                        GatewayErrorCode.PROVIDER_UNAVAILABLE,
                        "GPU provider is currently unavailable"));
    }

    @ExceptionHandler(GatewayServiceException.class)
    public ResponseEntity<GatewayResponse<Void>> handleServiceException(GatewayServiceException ex) {
        log.error("Service error code={} message={}", ex.getGatewayErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(mapToHttpStatus(ex.getGatewayErrorCode()))
                .body(GatewayResponse.error(ex.getGatewayErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(PodNotReadyException.class)
    public ResponseEntity<GatewayResponse<Void>> handlePodNotReadyException(PodNotReadyException ex) {
        log.error("Pod not ready error  code={} status={} message={}", ex.getGatewayErrorCode(), ex.getStatus(), ex.getMessage());
        return ResponseEntity
                .status(mapToHttpStatus(ex.getGatewayErrorCode()))
                .body(GatewayResponse.error(ex.getGatewayErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<GatewayResponse<Void>> handleThrowable(Throwable ex) {
        log.error("Unhandled exception", ex);
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