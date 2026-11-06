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

    /** O RT OPER pode revalidar a seleção antes de reservar; não altera o timeout de CONS. */
    @Bean
    public RestTemplate hubOperadoraRestTemplate(RestTemplateBuilder builder) {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory() {
            @Override protected void prepareConnection(java.net.HttpURLConnection c, String method) throws java.io.IOException {
                super.prepareConnection(c, method); c.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(8000); factory.setReadTimeout(300000);
        return builder.requestFactory(() -> factory).build();
    }
}