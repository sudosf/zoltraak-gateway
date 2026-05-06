package com.zoltraak.gateway.config;

import com.zoltraak.gateway.config.properties.ProviderProperties;
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
        // TODO revisit hard-coded timeout (magic number)
        HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(30));
        String baseUrl = providerProperties.getActive().equals("vastai")
                ? providerProperties.getRunPod().getBaseUrl()
                : providerProperties.getVastAi().getBaseUrl();

        // TODO revisit creating raw instance of client connector
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
