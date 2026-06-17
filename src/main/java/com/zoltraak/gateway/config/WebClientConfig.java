package com.zoltraak.gateway.config;

import com.zoltraak.gateway.config.properties.ProviderProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    private final ProviderProperties providerProperties;

    public WebClientConfig(ProviderProperties providerProperties) {
        this.providerProperties = providerProperties;
    }

    @Bean
    public WebClient runpodWebClient() {
        return buildGpuProviderWebClient(
                "runpodConnProvider",
                new ProviderConfig(
                        providerProperties.getRunpod().getBaseUrl(), providerProperties.getRunpod().getApiKey())
        );
    }

    @Bean
    public WebClient vastaiWebClient() {
        return buildGpuProviderWebClient(
                "vastaiConnProvider",
                new ProviderConfig(
                        providerProperties.getVastAi().getBaseUrl(), providerProperties.getVastAi().getApiKey())
        );
    }

    @Bean
    public WebClient ollamaWebClient() {
        ConnectionProvider connProvider = buildConnectionProvider("ollamaConnProvider");
        HttpClient httpClient = createHttpClient(connProvider);

        return WebClient
                .builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private WebClient buildGpuProviderWebClient(String connProviderName, ProviderConfig providerConfig) {
        ConnectionProvider connProvider = buildConnectionProvider(connProviderName);
        HttpClient httpClient = createHttpClient(connProvider);

        return buildWebClient(httpClient, providerConfig);
    }

    private WebClient buildWebClient(HttpClient httpClient, ProviderConfig providerConfig) {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(providerConfig.baseUrl)
                .defaultHeader("Authorization", "Bearer %s".formatted(providerConfig.apiKey))
                .build();
    }

    private HttpClient createHttpClient(ConnectionProvider connProvider) {
        return HttpClient.create(connProvider)
                .followRedirect(true)
                .responseTimeout(Duration.ofSeconds(providerProperties.getResponseTimeoutSeconds()));
    }

    private ConnectionProvider buildConnectionProvider(String name) {
        return ConnectionProvider.builder(name)
                .maxIdleTime(Duration.ofSeconds(providerProperties.getResponseTimeoutSeconds()))
                .build();
    }

    private record ProviderConfig(String baseUrl, String apiKey) {
    }
}
