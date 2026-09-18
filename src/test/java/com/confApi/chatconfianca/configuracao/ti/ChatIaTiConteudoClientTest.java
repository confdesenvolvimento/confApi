package com.confApi.chatconfianca.configuracao.ti;

import com.confApi.confApp.*;
import com.confApi.config.UrlConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ChatIaTiConteudoClientTest {
    String original; RestTemplate http; ConfAppService auth; ChatIaTiConteudoClient client; MockRestServiceServer server;
    @BeforeEach void setup(){
        original=UrlConfig.URL_CONFIANCA_MANAGER;UrlConfig.URL_CONFIANCA_MANAGER="http://manager.invalid/";
        http=new RestTemplate();auth=mock(ConfAppService.class);client=new ChatIaTiConteudoClient(http,auth);
        var token=mock(ConfAppResp.class);when(token.getToken()).thenReturn("token-ficticio");when(auth.token()).thenReturn(token);
        server=MockRestServiceServer.bindTo(http).build();
    }
    @AfterEach void cleanup(){UrlConfig.URL_CONFIANCA_MANAGER=original;}
    @Test void consultaFixaAutenticadaSemParametrosDeClienteENaoReaproveitaResposta() throws Exception {
        for(int v=1;v<=2;v++){
            var d=ChatIaTiTestData.documento();d.setPerfilVersao((long)v);
            server.expect(requestTo("http://manager.invalid/chatIa/runtime/ti-resposta"))
                .andExpect(method(HttpMethod.GET)).andExpect(header("Authorization","Bearer token-ficticio"))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(d),MediaType.APPLICATION_JSON));
        }
        assertEquals(1L,client.carregar().getPerfilVersao());assertEquals(2L,client.carregar().getPerfilVersao());server.verify();
    }
    @Test void conteudoNaoElegivelRetornaVazio(){
        server.expect(anything()).andRespond(withNoContent());assertNull(client.carregar());server.verify();
    }
    @Test void erroHttpNaoViraDocumento(){
        server.expect(anything()).andRespond(withServerError());assertThrows(Exception.class,()->client.carregar());server.verify();
    }
    @Test void tokenAusenteNaoChamaEndpoint(){
        when(auth.token()).thenReturn(null);assertThrows(IllegalStateException.class,()->client.carregar());server.verify();
    }
    @Test void urlAusenteNaoChamaAutenticacao(){
        UrlConfig.URL_CONFIANCA_MANAGER=null;assertThrows(IllegalStateException.class,()->client.carregar());verifyNoInteractions(auth);
    }
}
