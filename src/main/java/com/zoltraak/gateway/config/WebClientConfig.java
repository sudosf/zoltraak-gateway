package com.zoltraak.gateway.config;

import com.zoltraak.gateway.config.properties.ProviderProperties;
import com.zoltraak.gateway.domain.enums.GpuProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    // TODO create separate webclients for vast and runpod
    // TODO {NOT} remove base url and api initialization

    @Bean
    public WebClient providerWebClient(ProviderProperties providerProperties) {
        ConnectionProvider connProvider = ConnectionProvider.builder("gpuProviderConnProvider")
                .maxIdleTime(Duration.ofSeconds(providerProperties.getResponseTimeoutSeconds()))
                .build();


        HttpClient httpClient = HttpClient.create(connProvider)
                .followRedirect(true)
                .responseTimeout(Duration.ofSeconds(providerProperties.getResponseTimeoutSeconds()));

        String baseUrl = activeBaseUrl(providerProperties);
        String apiKey = activeApiKey(providerProperties);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer %s".formatted(apiKey))
                .build();
    }

    @Bean
    public WebClient ollamaWebClient() {
        return WebClient.builder().build();
    }

    private String activeBaseUrl(ProviderProperties providerProperties) {
        return providerProperties.getActive() == GpuProvider.VASTAI
                ? providerProperties.getVastAi().getBaseUrl()
                : providerProperties.getRunPod().getBaseUrl();
    }

    private String activeApiKey(ProviderProperties providerProperties) {
        return providerProperties.getActive() == GpuProvider.VASTAI
                ? providerProperties.getVastAi().getApiKey()
                : providerProperties.getRunPod().getApiKey();
    }
}