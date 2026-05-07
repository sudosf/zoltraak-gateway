package com.zoltraak.gateway.adapters.gpu;

import com.zoltraak.gateway.adapters.gpu.model.VastAiInstanceWrapper;
import com.zoltraak.gateway.adapters.gpu.model.VastAiManageRequest;
import com.zoltraak.gateway.annotations.Adapter;
import com.zoltraak.gateway.config.properties.OllamaProperties;
import com.zoltraak.gateway.config.properties.ProviderProperties;
import com.zoltraak.gateway.domain.enums.PodStatus;
import com.zoltraak.gateway.domain.shared.PodConnectionDetails;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.reactive.function.client.WebClient;

@Adapter
@ConditionalOnProperty(name = "zoltraak.provider.active", havingValue = "vastai")
public class VastAiAdapter implements GpuProviderPort {
    private final ProviderProperties providerProperties;
    private final WebClient webClient;
    private final OllamaProperties ollamaProperties;

    public VastAiAdapter(ProviderProperties providerProperties, @Qualifier("providerWebClient") WebClient webClient, OllamaProperties ollamaProperties) {
        this.providerProperties = providerProperties;
        this.webClient = webClient;
        this.ollamaProperties = ollamaProperties;
    }

    @Override
    public void start() {
        changeState("running");
    }

    @Override
    public void stop() {
        changeState("stopped");
    }

    @Override
    public PodStatus getStatus() {
        VastAiInstanceWrapper response = fetchInstance();
        if (response == null) {
            throw new IllegalStateException("Could not retrieve instance status from Vast.ai");
        }

        return switch (response.instances().actualStatus()) {
            case "running" -> PodStatus.READY;
            case "loading" -> PodStatus.WARMING;
            case null -> PodStatus.STARTING;
            default -> PodStatus.STOPPED;
        };
    }

    @Override
    public PodConnectionDetails getConnectionDetails() {
        VastAiInstanceWrapper response = fetchInstance();
        if (response == null) {
            throw new IllegalStateException("Could not retrieve instance details from Vast.ai");
        }

        String ipAddress = response.instances().publicIpaddr();
        String ollamaUrl = "http://%s:%d".formatted(ipAddress, ollamaProperties.getGpuPod().getPort());

        return new PodConnectionDetails(ollamaUrl, null);
    }

    private VastAiInstanceWrapper fetchInstance() {
        return webClient
                .get()
                .uri("/api/v0/instances/%s".formatted(providerProperties.getVastAi().getInstanceId()))
                .header("Authorization", "Bearer %s".formatted(providerProperties.getVastAi().getApiKey()))
                .retrieve()
                .bodyToMono(VastAiInstanceWrapper.class)
                .block();
    }

    private void changeState(String state) {
        webClient
                .put()
                .uri("/api/v0/instances/%s".formatted(providerProperties.getVastAi().getInstanceId()))
                .header("Authorization", "Bearer %s".formatted(providerProperties.getVastAi().getApiKey()))
                .bodyValue(new VastAiManageRequest(state))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}
