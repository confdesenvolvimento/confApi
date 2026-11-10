package com.confApi.geradorPdf;
import com.confApi.confApp.*;
import com.confApi.config.UrlConfig;
import com.confApi.geradorPdf.aereo.EnvioReservaAereoPDF;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
class GeradorPDFApiTest {
    String original;MockRestServiceServer server;GeradorPDFApi api;
    @BeforeEach void setup(){
        original=UrlConfig.URL_CONFIANCA_EMAIL;UrlConfig.URL_CONFIANCA_EMAIL="https://email.example.test/";
        var http=new RestTemplate();server=MockRestServiceServer.createServer(http);
        var auth=mock(ConfAppService.class);var token=new ConfAppResp();token.setToken("token-teste");when(auth.token()).thenReturn(token);
        api=new GeradorPDFApi(http,auth);
    }
    @AfterEach void cleanup(){UrlConfig.URL_CONFIANCA_EMAIL=original;}
    @Test void enviaSomenteTokenEExigeConfirmacao(){
        server.expect(requestTo("https://email.example.test/mail/sendEmailPdf")).andExpect(header("Authorization","Bearer token-teste"))
            .andRespond(withSuccess("[{\"statusEnvio\":\"Email enviado com sucesso\"}]",MediaType.APPLICATION_JSON));
        api.envioAereoPDF(null);server.verify();
    }
    @Test void erroDoServicoNaoEhIgnorado(){
        server.expect(anything()).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertThrows(RuntimeException.class,()->api.envioAereoPDF(null));server.verify();
    }
    @Test void status200ComFalhaDeEnvioNaoEhSucesso(){
        server.expect(anything()).andRespond(withSuccess("[{\"statusEnvio\":\"Email não enviado\"}]",MediaType.APPLICATION_JSON));
        assertThrows(IllegalStateException.class,()->api.envioAereoPDF(null));server.verify();
    }
    @Test void listaVaziaNaoConfirmaEnvio(){
        server.expect(anything()).andRespond(withSuccess("[]",MediaType.APPLICATION_JSON));
        assertThrows(IllegalStateException.class,()->api.envioAereoPDF(null));server.verify();
    }
}
