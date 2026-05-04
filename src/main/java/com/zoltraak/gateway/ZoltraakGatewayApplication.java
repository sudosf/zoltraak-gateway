package com.zoltraak.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ZoltraakGatewayApplication {

    static void main(String[] args) {
        SpringApplication.run(ZoltraakGatewayApplication.class, args);
    }

}
