package com.zoltraak.gateway.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

// TODO review class after connecting to cloud instance and remove if not needed
@ConfigurationProperties(prefix = "zoltraak")
@Data
public class ZoltraakProperties {
    private ProviderProperties provider;
    private GpuProperties gpu;
    private OllamaProperties ollama;
    private QueueProperties queue;
}
