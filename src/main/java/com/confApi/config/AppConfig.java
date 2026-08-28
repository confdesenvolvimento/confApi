package com.confApi.config;


import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean("chatConfiancaRestTemplate")
    public RestTemplate chatConfiancaRestTemplate(
            RestTemplateBuilder builder,
            @Value("${chat-confianca.http.connect-timeout-ms:10000}") long connectTimeoutMs,
            @Value("${chat-confianca.http.request-timeout-ms:60000}") long requestTimeoutMs,
            @Value("${chat-confianca.http.max-idle-connections:20}") int maxIdleConnections,
            @Value("${chat-confianca.http.keep-alive-ms:300000}") long keepAliveMs) {
        return pooledRestTemplate(
                builder,
                connectTimeoutMs,
                requestTimeoutMs,
                maxIdleConnections,
                keepAliveMs,
                10000,
                60000);
    }

    @Bean("chatConfiancaAuthRestTemplate")
    public RestTemplate chatConfiancaAuthRestTemplate(
            RestTemplateBuilder builder,
            @Value("${chat-confianca.auth.http.connect-timeout-ms:5000}")
            long connectTimeoutMs,
            @Value("${chat-confianca.auth.http.request-timeout-ms:15000}")
            long requestTimeoutMs) {
        return pooledRestTemplate(
                builder,
                connectTimeoutMs,
                requestTimeoutMs,
                2,
                300000,
                5000,
                15000);
    }

    @Bean("chatIntencaoAuditExecutor")
    public ThreadPoolTaskExecutor chatIntencaoAuditExecutor(
            @Value("${chat-confianca.intencao-v1.audit-core-pool-size:1}") int corePoolSize,
            @Value("${chat-confianca.intencao-v1.audit-max-pool-size:2}") int maxPoolSize,
            @Value("${chat-confianca.intencao-v1.audit-queue-capacity:500}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Math.max(1, corePoolSize));
        executor.setMaxPoolSize(Math.max(Math.max(1, corePoolSize), maxPoolSize));
        executor.setQueueCapacity(Math.max(1, queueCapacity));
        executor.setThreadNamePrefix("chat-intencao-audit-");
        executor.setDaemon(true);
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }

    private RestTemplate pooledRestTemplate(
            RestTemplateBuilder builder,
            long connectTimeoutMs,
            long requestTimeoutMs,
            int maxIdleConnections,
            long keepAliveMs,
            long defaultConnectTimeoutMs,
            long defaultRequestTimeoutMs) {
        long connectTimeout = positivo(connectTimeoutMs, defaultConnectTimeoutMs);
        long requestTimeout = positivo(requestTimeoutMs, defaultRequestTimeoutMs);
        int maxIdle = Math.max(1, maxIdleConnections);
        long keepAlive = positivo(keepAliveMs, 300000);

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(maxIdle, keepAlive, TimeUnit.MILLISECONDS))
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .readTimeout(Duration.ofMillis(requestTimeout))
                .writeTimeout(Duration.ofMillis(requestTimeout))
                .callTimeout(Duration.ofMillis(requestTimeout))
                .retryOnConnectionFailure(false)
                .build();

        return builder
                .requestFactory(() -> new OkHttp3ClientHttpRequestFactory(httpClient))
                .build();
    }

    private long positivo(long valor, long padrao) {
        return valor > 0 ? valor : padrao;
    }
}
