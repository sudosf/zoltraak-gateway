package com.zoltraak.gateway.config;

import com.zoltraak.gateway.config.properties.ProviderProperties;
import com.zoltraak.gateway.domain.enums.GpuProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient providerWebClient(ProviderProperties providerProperties) {
        HttpClient httpClient = HttpClient.create()
                .followRedirect(true)
                .responseTimeout(Duration.ofSeconds(providerProperties.getTimeoutSeconds()));

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