package com.confApi.hub.limites;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.hub.limites.dto.Disponibilidade;
import com.confApi.hub.limites.dto.LimiteCreditoRQ;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LimitesServiceTest {

    @Test
    void deveSinalizarFalhaTecnicaSemInventarAusenciaDeLimite() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ConfAppService confAppService = mock(ConfAppService.class);
        when(confAppService.token()).thenThrow(new RuntimeException("Hub indisponivel"));
        LimitesService service = new LimitesService(restTemplate);
        ReflectionTestUtils.setField(service, "confAppService", confAppService);

        Disponibilidade response =
                service.consultaLimiteApi(new LimiteCreditoRQ("987"));

        assertFalse(Boolean.TRUE.equals(response.getConsultaConfirmada()));
        assertNotNull(response.getMensagemConsulta());
        assertTrue(response.getLimiteCredito().isEmpty());
    }

    @Test
    void deveMarcarComoConfirmadaRespostaValidaSemLimites() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ConfAppService confAppService = mock(ConfAppService.class);
        ConfAppResp token = new ConfAppResp();
        token.setToken("token");
        when(confAppService.token()).thenReturn(token);
        Disponibilidade body = new Disponibilidade();
        body.setLimiteCredito(List.of());
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Disponibilidade.class)))
                .thenReturn(ResponseEntity.ok(body));

        LimitesService service = new LimitesService(restTemplate);
        ReflectionTestUtils.setField(service, "confAppService", confAppService);
        String urlAnterior = UrlConfig.URL_CONFIANCA_HUB;
        UrlConfig.URL_CONFIANCA_HUB = "http://localhost/";
        try {
            Disponibilidade response =
                    service.consultaLimiteApi(new LimiteCreditoRQ("987"));

            assertTrue(Boolean.TRUE.equals(response.getConsultaConfirmada()));
            assertTrue(response.getLimiteCredito().isEmpty());
        } finally {
            UrlConfig.URL_CONFIANCA_HUB = urlAnterior;
        }
    }
}
