package com.confApi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class HubRestTemplateConfig {

    @Bean
    public RestTemplate hubRestTemplate(
            RestTemplateBuilder builder,
            @Value("${hub.connect-timeout-ms:8000}") long connectTimeout,
            @Value("${hub.read-timeout-ms:30000}") long readTimeout
    ) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeout))
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .build();
    }
}