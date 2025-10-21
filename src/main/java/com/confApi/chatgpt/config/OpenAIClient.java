package com.confApi.chatgpt.config;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(OpenAIProperties.class)
public class OpenAIClient {
    @Bean("baseHttpClient")
    public OkHttpClient httpClient() {
        return new OkHttpClient.Builder()
                .readTimeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public Interceptor authInterceptor(OpenAIProperties p) {
        return chain -> {
            Request.Builder b = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer " + p.getApiKey());
            if (p.getOrganization() != null && !p.getOrganization().isBlank()) {
                b.addHeader("OpenAI-Organization", p.getOrganization());
            }
            if (p.getProject() != null && !p.getProject().isBlank()) {
                b.addHeader("OpenAI-Project", p.getProject());
            }
            return chain.proceed(b.build());
        };
    }

    @Bean("openAi")
    @Primary // <- este passa a ser o padrão quando alguém pedir OkHttpClient
    public OkHttpClient openAi(
            @Qualifier("baseHttpClient") OkHttpClient base,
            Interceptor authInterceptor) {
        return base.newBuilder().addInterceptor(authInterceptor).build();
    }
}