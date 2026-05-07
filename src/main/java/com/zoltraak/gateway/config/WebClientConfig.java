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
        HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(providerProperties.getTimeoutSeconds()));

        String baseUrl = providerProperties.getActive() == GpuProvider.VASTAI
                ? providerProperties.getVastAi().getBaseUrl()
                : providerProperties.getRunPod().getBaseUrl();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    public WebClient ollamaWebClient() {
        return WebClient.builder().build();
    }
}
