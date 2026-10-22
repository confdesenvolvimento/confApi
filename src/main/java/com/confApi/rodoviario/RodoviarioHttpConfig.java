package com.confApi.rodoviario;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

@Configuration
public class RodoviarioHttpConfig {
    @Bean("rodoviarioRestTemplate")
    public RestTemplate rodoviarioRestTemplate(RestTemplateBuilder builder,
            @Value("${rodoviario.http.connect-timeout-ms:8000}") long connect,
            @Value("${rodoviario.http.request-timeout-ms:180000}") long request) {
        var client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(connect > 0 ? connect : 8000))
                .readTimeout(Duration.ofMillis(request > 0 ? request : 180000))
                .writeTimeout(Duration.ofMillis(request > 0 ? request : 180000))
                .callTimeout(Duration.ofMillis(request > 0 ? request : 180000))
                .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build();
        return builder.requestFactory(() -> new OkHttp3ClientHttpRequestFactory(client)).build();
    }
}