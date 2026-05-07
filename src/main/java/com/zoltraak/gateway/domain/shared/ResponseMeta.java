package com.zoltraak.gateway.domain.shared;

import java.time.LocalDateTime;

public record ResponseMeta(
        String requestId,
        LocalDateTime timestamp,
        Integer estimatedWaitSeconds
) {
}
