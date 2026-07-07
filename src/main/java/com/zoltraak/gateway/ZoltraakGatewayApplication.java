package com.zoltraak.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication(exclude = ReactiveUserDetailsServiceAutoConfiguration.class)
public class ZoltraakGatewayApplication {

    static void main(String[] args) {
        SpringApplication.run(ZoltraakGatewayApplication.class, args);
    }

}
