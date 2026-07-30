package com.confApi.config;

import com.confApi.confApp.ConfAppService;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class AppConfigTest {

    private final AppConfig config = new AppConfig();
    private final RestTemplateBuilder builder = new RestTemplateBuilder();

    @Test
    void deveConfigurarTimeoutsDoManagerSemRetryDeTransporte() {
        OkHttpClient client = httpClient(config.chatConfiancaRestTemplate(
                builder,
                1_234L,
                5_678L,
                20,
                300_000L));

        assertEquals(1_234, client.connectTimeoutMillis());
        assertEquals(5_678, client.readTimeoutMillis());
        assertEquals(5_678, client.writeTimeoutMillis());
        assertEquals(5_678, client.callTimeoutMillis());
        assertFalse(client.retryOnConnectionFailure());
    }

    @Test
    void deveLimitarAutenticacaoEUsarDefaultsParaValoresInvalidos() {
        OkHttpClient client = httpClient(config.chatConfiancaAuthRestTemplate(
                builder,
                0L,
                -1L));

        assertEquals(5_000, client.connectTimeoutMillis());
        assertEquals(15_000, client.callTimeoutMillis());
        assertFalse(client.retryOnConnectionFailure());
    }

    @Test
    void deveInjetarClienteComTimeoutNoServicoDeAutenticacao() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext()) {
            Supplier<RestTemplateBuilder> builderSupplier = RestTemplateBuilder::new;
            context.registerBean(RestTemplateBuilder.class, builderSupplier);
            context.register(AppConfig.class, ConfAppService.class);
            context.refresh();

            ConfAppService service = context.getBean(ConfAppService.class);
            RestTemplate authClient = context.getBean(
                    "chatConfiancaAuthRestTemplate",
                    RestTemplate.class);
            assertSame(
                    authClient,
                    ReflectionTestUtils.getField(service, "restTemplate"));
        }
    }

    private OkHttpClient httpClient(RestTemplate restTemplate) {
        OkHttp3ClientHttpRequestFactory factory = assertInstanceOf(
                OkHttp3ClientHttpRequestFactory.class,
                restTemplate.getRequestFactory());
        return assertInstanceOf(
                OkHttpClient.class,
                ReflectionTestUtils.getField(factory, "client"));
    }
}
