package com.zoltraak.gateway.adapters.gpu;

import com.zoltraak.gateway.domain.enums.GpuProvider;
import com.zoltraak.gateway.domain.shared.ZoltraakGatewayException;
import lombok.Getter;

@Getter
public class ProviderException extends ZoltraakGatewayException {

    private final GpuProvider provider;
    private final int httpStatusCode;

    public ProviderException(GpuProvider provider, int httpStatusCode, String rawError) {
        super(rawError);
        this.provider = provider;
        this.httpStatusCode = httpStatusCode;
    }
}
