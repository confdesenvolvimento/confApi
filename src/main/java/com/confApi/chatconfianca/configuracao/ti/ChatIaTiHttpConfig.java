package com.confApi.chatconfianca.configuracao.ti;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConditionalOnProperty(prefix = "chat-confianca.ia-config.ti-resposta", name = "enabled", havingValue = "true")
public class ChatIaTiHttpConfig {
    @Bean("chatIaTiHttp")
    public RestTemplate http() {
        var client = new OkHttpClient.Builder().connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(4, TimeUnit.SECONDS).callTimeout(4, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false).build();
        return new RestTemplate(new OkHttp3ClientHttpRequestFactory(client));
    }
}
