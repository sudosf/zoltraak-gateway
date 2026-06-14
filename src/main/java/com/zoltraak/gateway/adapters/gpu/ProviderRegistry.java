package com.zoltraak.gateway.adapters.gpu;

import com.zoltraak.gateway.annotations.Adapter;
import com.zoltraak.gateway.config.properties.ProviderProperties;
import com.zoltraak.gateway.domain.enums.GpuProvider;
import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.domain.models.provider.PodConnectionDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Adapter
@Primary
public class ProviderRegistry implements GpuProviderPort {

    private final ProviderProperties providerProperties;
    private final Map<GpuProvider, GpuProviderPort> providers;

    public ProviderRegistry(ProviderProperties providerProperties, VastAiAdapter vastAiAdapter) {
        this.providerProperties = providerProperties;
        this.providers = Map.of(GpuProvider.VASTAI, vastAiAdapter);
    }

    @Override
    public Mono<Void> start() {
        return activeProvider().start();
    }

    @Override
    public Mono<Void> stop() {
        return activeProvider().stop();
    }

    @Override
    public Mono<PodStatus> getStatus() {
        return activeProvider().getStatus();
    }

    @Override
    public Mono<PodConnectionDetails> getConnectionDetails() {
        return activeProvider().getConnectionDetails();
    }

    private GpuProviderPort activeProvider() {
        GpuProvider activeProvider = providerProperties.getActive();
        GpuProviderPort providerPort = providers.get(activeProvider);

        if (providerPort == null) {
            log.error("No adapter registered for provider {}", activeProvider);
            throw new ProviderException(activeProvider, 404, "No adapter registered for provider");
        }

        log.debug("Using {} provider", activeProvider);
        return providerPort;
    }
}
