package com.confApi.clientapp.config;

import com.confApi.clientapp.integration.MViagensBackendClient;
import com.confApi.clientapp.integration.enrollment.ManagerPassengerDiscoveryClient;
import com.confApi.clientapp.integration.enrollment.MViagensEnrollmentClient;
import com.confApi.confApp.ConfAppService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(prefix = "mviagens.backend", name = "enabled", havingValue = "true")
public class MViagensBackendHttpConfig {

    @Bean("mviagensRestTemplate")
    RestTemplate mviagensRestTemplate(RestTemplateBuilder builder, MViagensBackendProperties properties) {
        return builder
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                .build();
    }

    @Bean
    MViagensBackendClient mViagensBackendClient(
            @Qualifier("mviagensRestTemplate") RestTemplate restTemplate,
            MViagensBackendProperties properties
    ) {
        return new MViagensBackendClient(restTemplate, properties);
    }

    @Bean
    MViagensEnrollmentClient mViagensEnrollmentClient(
            @Qualifier("mviagensRestTemplate") RestTemplate restTemplate,
            MViagensBackendProperties properties,
            ObjectMapper objectMapper
    ) {
        return new MViagensEnrollmentClient(restTemplate, properties, objectMapper);
    }

    @Bean
    ManagerPassengerDiscoveryClient managerPassengerDiscoveryClient(
            @Qualifier("mviagensRestTemplate") RestTemplate restTemplate,
            ClientAppEnrollmentProperties enrollmentProperties,
            ConfAppService confAppService,
            ObjectMapper objectMapper
    ) {
        return new ManagerPassengerDiscoveryClient(restTemplate, enrollmentProperties, confAppService, objectMapper);
    }
}
