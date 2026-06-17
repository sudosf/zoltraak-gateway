package com.zoltraak.gateway.features.gpu.model;

import com.zoltraak.gateway.domain.enums.GpuProviderType;
import jakarta.validation.constraints.NotNull;

public record ProviderRequest(
        @NotNull(message = "Missing required field")
        GpuProviderType provider
) {
}
