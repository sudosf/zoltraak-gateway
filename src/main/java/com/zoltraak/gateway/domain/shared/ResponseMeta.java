package com.zoltraak.gateway.domain.shared;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResponseMeta(
        String requestId,
        LocalDateTime timestamp,
        Integer estimatedWaitSeconds
) {
    public static ResponseMeta now() {
        return new ResponseMeta(UUID.randomUUID().toString(), LocalDateTime.now(), null);
    }
}