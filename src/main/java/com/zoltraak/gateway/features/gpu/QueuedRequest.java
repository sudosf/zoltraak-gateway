package com.zoltraak.gateway.features.gpu;

import java.time.LocalDateTime;
import java.util.function.Consumer;

public record QueuedRequest(
        String requestId,
        LocalDateTime enqueuedAt,
        Runnable task,
        Consumer<Throwable> onFailure
) {
}
