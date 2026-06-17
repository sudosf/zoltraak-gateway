package com.zoltraak.gateway.adapters.gpu;

import com.zoltraak.gateway.domain.enums.GpuProviderType;
import com.zoltraak.gateway.domain.exception.ZoltraakGatewayException;
import lombok.Getter;

@Getter
public class ProviderException extends ZoltraakGatewayException {

    private final GpuProviderType provider;
    private final int httpStatusCode;

    public ProviderException(GpuProviderType provider, int httpStatusCode, String rawError) {
        super(rawError);
        this.provider = provider;
        this.httpStatusCode = httpStatusCode;
    }
}
