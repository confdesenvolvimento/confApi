package com.confApi.endPoints.reservaAereo;

import com.confApi.db.confManager.companhiaAerea.CompanhiaAerea;
import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ReservaAereoApiTest {
    private String originalManagerUrl;

    @BeforeEach
    void configureMockUrl() {
        originalManagerUrl = UrlConfig.URL_CONFIANCA_MANAGER;
        UrlConfig.URL_CONFIANCA_MANAGER = "http://localhost/manager";
    }

    @AfterEach
    void restoreUrl() {
        UrlConfig.URL_CONFIANCA_MANAGER = originalManagerUrl;
    }

    @Test
    void deveConsiderarLaEJjComoMesmaCompanhia() {
        ReservaAereoApi api = new ReservaAereoApi(null);
        CompanhiaAerea latamLa = new CompanhiaAerea(1, "LA");
        CompanhiaAerea latamJj = new CompanhiaAerea(2, "JJ");

        Boolean mesmaCompanhia = ReflectionTestUtils.invokeMethod(api, "mesmaCompanhia", latamLa, latamJj);

        assertTrue(Boolean.TRUE.equals(mesmaCompanhia));
    }

    @Test
    void consultaDeSincronizacaoNaoDeveTratarErroComoReservaAusente() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rest);
        ReservaAereoApi api = authenticatedApi(rest);
        server.expect(method(HttpMethod.GET)).andRespond(withServerError());
        assertThrows(HttpServerErrorException.class,
                () -> api.findByLocalizadorCompanhiaParaSincronizacao("ATPXGW", new CompanhiaAerea(1, "G3")));
        server.verify();
    }

    @Test
    void consultaDeSincronizacaoDeveAceitarListaVaziaMasRejeitarAmbiguidade() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rest);
        ReservaAereoApi api = authenticatedApi(rest);
        server.expect(method(HttpMethod.GET)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(method(HttpMethod.GET)).andRespond(withSuccess("""
                [{"codgReservaAereo":1,"localizador":"ATPXGW","codgCompanhiaAerea":{"codgCompanhiaAerea":1,"iataCia":"G3"}},
                 {"codgReservaAereo":2,"localizador":"ATPXGW","codgCompanhiaAerea":{"codgCompanhiaAerea":1,"iataCia":"G3"}}]
                """, MediaType.APPLICATION_JSON));
        assertNull(api.findByLocalizadorCompanhiaParaSincronizacao("ATPXGW", new CompanhiaAerea(1, "G3")));
        assertThrows(IllegalStateException.class,
                () -> api.findByLocalizadorCompanhiaParaSincronizacao("ATPXGW", new CompanhiaAerea(1, "G3")));
        server.verify();
    }

    @Test
    void divisaoDeveUsarEndpointIsoladoEExigirConfirmacao() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(rest);
        ReservaAereoApi api = authenticatedApi(rest);
        var destination = new com.confApi.db.confManager.reservaAereo.ReservaAereo();
        destination.setLocalizador("GKPXNT");
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                        "http://localhost/manager/reservaAereo/wooba/divisoes"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath("$.codgReservaOrigem").value(256643))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath("$.destino.localizador").value("GKPXNT"))
                .andRespond(withSuccess("999", MediaType.APPLICATION_JSON));
        server.expect(method(HttpMethod.POST)).andRespond(withNoContent());
        org.junit.jupiter.api.Assertions.assertEquals(999, api.reconciliarDivisaoWooba(256643, destination));
        assertThrows(IllegalStateException.class, () -> api.reconciliarDivisaoWooba(256643, destination));
        server.verify();
    }

    private ReservaAereoApi authenticatedApi(RestTemplate rest) {
        ReservaAereoApi api = new ReservaAereoApi(rest);
        ConfAppService auth = mock(ConfAppService.class);
        ConfAppResp token = mock(ConfAppResp.class);
        when(auth.token()).thenReturn(token);
        when(token.getToken()).thenReturn("test-token");
        ReflectionTestUtils.setField(api, "confAppService", auth);
        return api;
    }
}
