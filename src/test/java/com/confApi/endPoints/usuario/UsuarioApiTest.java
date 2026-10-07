package com.confApi.endPoints.usuario;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class UsuarioApiTest {
    private String originalUrl;
    private UsuarioApi api;
    private MockRestServiceServer server;

    @BeforeEach
    void setup() {
        originalUrl = UrlConfig.URL_CONFIANCA_MANAGER;
        UrlConfig.URL_CONFIANCA_MANAGER = "https://manager.example/confiancamanger2";
        RestTemplate http = new RestTemplate();
        server = MockRestServiceServer.bindTo(http).build();
        api = new UsuarioApi(http);
        ConfAppService auth = mock(ConfAppService.class);
        ConfAppResp token = new ConfAppResp();
        token.setToken("test-token");
        when(auth.token()).thenReturn(token);
        ReflectionTestUtils.setField(api, "confAppService", auth);
    }

    @AfterEach
    void cleanup() {
        UrlConfig.URL_CONFIANCA_MANAGER = originalUrl;
    }

    @Test
    void consultaParaImportacaoDeveDistinguir404DeIndisponibilidade() {
        server.expect(requestTo(UrlConfig.URL_CONFIANCA_MANAGER + "/usuario/findByLogin/ausente"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));
        server.expect(requestTo(UrlConfig.URL_CONFIANCA_MANAGER + "/usuario/findByLogin/offline"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));
        assertNull(api.consultaUsuarioByLoginParaImportacao("ausente"));
        assertThrows(org.springframework.web.client.HttpServerErrorException.class,
                () -> api.consultaUsuarioByLoginParaImportacao("offline"));
        server.verify();
    }

    @Test
    void consultaParaImportacaoDeveCodificarLogin() {
        server.expect(requestTo(UrlConfig.URL_CONFIANCA_MANAGER + "/usuario/findByLogin/ana%2Bteste"))
                .andRespond(withSuccess("{\"codgUsuario\":123,\"loginUsuario\":\"ana+teste\"}", MediaType.APPLICATION_JSON));
        assertNotNull(api.consultaUsuarioByLoginParaImportacao(" ana+teste "));
        server.verify();
    }

    @ParameterizedTest
    @CsvSource(value = {"' 12345678901 ',12345678901", "'ana maria',ana%20maria", "'ana/+?#%',ana%2F%2B%3F%23%25"})
    void consultaManagerNormalizaECodificaUmaVez(String login, String encoded) {
        server.expect(requestTo(UrlConfig.URL_CONFIANCA_MANAGER + "/usuario/findByLogin/" + encoded))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertNotNull(api.consultaUsuarioByLogin(login));
        server.verify();
    }

    @ParameterizedTest
    @CsvSource(value = {"' 12345678901 ',12345678901", "'ana maria',ana%20maria", "'ana/+?#%',ana%2F%2B%3F%23%25"})
    void consultaWoobaNormalizaECodificaUmaVez(String login, String encoded) {
        server.expect(requestTo(UrlConfig.URL_CONFIANCA_MANAGER + "/wooba/turUsuarios/loginDB/" + encoded))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertNotNull(api.consultaUsuarioByLoginWooba(login));
        server.verify();
    }
}
