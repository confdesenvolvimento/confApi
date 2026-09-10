package com.confApi.chatconfianca.configuracao;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConditionalOnProperty(prefix = "chat-confianca.ia-config", name = "shadow-enabled", havingValue = "true")
public class ChatIaConfiguracaoShadowConfig {
    @Bean("chatIaConfiguracaoShadowExecutor")
    public ThreadPoolTaskExecutor executor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1); executor.setMaxPoolSize(1); executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("chat-ia-config-shadow-"); executor.setDaemon(true);
        executor.setWaitForTasksToCompleteOnShutdown(false);
        // Default AbortPolicy: saturation drops diagnostic work, never runs it on the request thread.
        return executor;
    }
    @Bean("chatIaConfiguracaoShadowHttp")
    public RestTemplate http() {
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(4, TimeUnit.SECONDS).callTimeout(4, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false).build();
        return new RestTemplate(new OkHttp3ClientHttpRequestFactory(client));
    }
}
