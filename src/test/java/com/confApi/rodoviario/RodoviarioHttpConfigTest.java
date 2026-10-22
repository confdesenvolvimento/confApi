package com.confApi.rodoviario;

import com.confApi.confApp.ConfAppService;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RodoviarioHttpConfigTest {
    @Test void transportesPossuemTimeoutENaoRepetemOuRedirecionamCheckout() {
        var rest = new RodoviarioHttpConfig().rodoviarioRestTemplate(new RestTemplateBuilder(), 1234, 5678);
        var factory = assertInstanceOf(OkHttp3ClientHttpRequestFactory.class, rest.getRequestFactory());
        var client = (OkHttpClient) ReflectionTestUtils.getField(factory, "client");
        assertNotNull(client); assertEquals(1234, client.connectTimeoutMillis()); assertEquals(5678, client.callTimeoutMillis());
        assertFalse(client.retryOnConnectionFailure()); assertFalse(client.followRedirects()); assertFalse(client.followSslRedirects());
    }
    @Test void clienteRecebeOBeanRodoviarioMesmoComOutrosRestTemplates() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(RestTemplateBuilder.class, () -> new RestTemplateBuilder());
            context.registerBean("restTemplate", RestTemplate.class, () -> new RestTemplate());
            context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
            context.registerBean(ConfAppService.class, () -> mock(ConfAppService.class));
            context.register(RodoviarioHttpConfig.class, RodoviarioHttpClient.class); context.refresh();
            assertSame(context.getBean("rodoviarioRestTemplate"), ReflectionTestUtils.getField(context.getBean(RodoviarioHttpClient.class), "rest"));
        }
    }
}