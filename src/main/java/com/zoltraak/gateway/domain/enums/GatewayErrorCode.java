package com.zoltraak.gateway.domain.enums;

public enum GatewayErrorCode {
    INVALID_TOKEN,
    TOKEN_EXPIRED,
    POD_START_FAILED,
    WARMUP_TIMEOUT,
    QUEUE_TIMEOUT,
    STORY_FAILED,
    IMAGE_GEN_FAILED,
    PROVIDER_UNAVAILABLE,
    INTENT_UNRESOLVED,
    INTERNAL_ERROR
}
