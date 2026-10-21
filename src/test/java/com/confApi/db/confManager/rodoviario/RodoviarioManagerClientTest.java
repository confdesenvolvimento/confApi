package com.confApi.db.confManager.rodoviario;
import com.confApi.confApp.*;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.rodoviario.dto.*;
import org.junit.jupiter.api.*;
import org.springframework.web.client.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
class RodoviarioManagerClientTest {
    String urlOriginal;
    @AfterEach void restaurar(){UrlConfig.URL_CONFIANCA_MANAGER=urlOriginal;}
    RestTemplate rest;ConfAppService auth;RodoviarioManagerClient client;MockRestServiceServer server;
    @BeforeEach void preparar(){urlOriginal=UrlConfig.URL_CONFIANCA_MANAGER;UrlConfig.URL_CONFIANCA_MANAGER="http://manager.test/";rest=new RestTemplate();auth=mock(ConfAppService.class);var t=new ConfAppResp();t.setToken("APP_TEST");when(auth.token()).thenReturn(t);client=new RodoviarioManagerClient(rest,auth);server=MockRestServiceServer.bindTo(rest).build();}
    @Test void encaminhaTokenIndividualEChaveIdempotencia(){
        server.expect(requestTo(UrlConfig.URL_CONFIANCA_MANAGER+"reservaRodoviario/salvarCompleta")).andExpect(method(HttpMethod.POST)).andExpect(header("Authorization","Bearer APP_TEST")).andExpect(header("X-Usuario-Token","USUARIO_TEST")).andExpect(jsonPath("$.chaveIdempotencia").value("chave-teste-001")).andRespond(withSuccess("{\"reserva\":{\"codgReservaRodoviario\":10}}",MediaType.APPLICATION_JSON));
        var r=new SalvarReservaRodoviarioRequest();r.setChaveIdempotencia("chave-teste-001");assertEquals(10,client.salvarCompleta(r,"USUARIO_TEST").getReserva().getCodgReservaRodoviario());server.verify();
    }
    @Test void naoUsaTokenDeAplicacaoComoIdentidade(){
        assertEquals(401,assertThrows(ResponseStatusException.class,()->client.detalhe(10,null)).getRawStatusCode());server.verify();
    }
    @Test void preservaErroDeEscopoDoManager(){
        server.expect(requestTo(UrlConfig.URL_CONFIANCA_MANAGER+"reservaRodoviario/detalhe/10")).andRespond(withStatus(HttpStatus.FORBIDDEN));
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(HttpClientErrorException.class,()->client.detalhe(10,"USUARIO_TEST")).getStatusCode());server.verify();
    }
}
