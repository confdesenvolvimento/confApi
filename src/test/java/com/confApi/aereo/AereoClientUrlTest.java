package com.confApi.aereo;

import com.confApi.config.UrlConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class AereoClientUrlTest {

    private final String urlOriginal = UrlConfig.URL_CONFIANCA_HUB;
    private final AereoClient client = new AereoClient(mock(RestTemplate.class));

    @AfterEach
    void restaurarUrlOriginal() {
        UrlConfig.URL_CONFIANCA_HUB = urlOriginal;
    }

    @Test
    void deveRemoverBarraDuplicada() {
        assertUrl("http://localhost:8081/", "/api/aereo/tarifar");
    }

    @Test
    void deveAdicionarBarraQuandoAusente() {
        assertUrl("http://localhost:8081", "api/aereo/tarifar");
    }

    @Test
    void devePreservarUrlComUmaBarra() {
        assertUrl("http://localhost:8081", "/api/aereo/tarifar");
        assertUrl("http://localhost:8081/", "api/aereo/tarifar");
    }

    private void assertUrl(String baseUrl, String endpoint) {
        UrlConfig.URL_CONFIANCA_HUB = baseUrl;

        String url = ReflectionTestUtils.invokeMethod(client, "montarUrl", endpoint);

        assertEquals("http://localhost:8081/api/aereo/tarifar", url);
    }
}
