package com.confApi.carros;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.carro.CarroReservaService;
import com.confApi.hub.carro.HubCarroClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import static org.mockito.Mockito.*;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

class CarroCancelamentoConfirmacaoTest {
    private MockRestServiceServer hubServer, managerServer;
    private MockMvc mvc;
    private String oldHub, oldManager;
    private static final String REQUEST = "{\"cancelarReservaCarroRequestDTO\":{\"localizador\":\"CAR123\",\"token\":\"selecao-teste\",\"sistema\":{\"nome\":\"MB\"}},\"usuario\":{\"codgUsuario\":42},\"descricaoMotivoCancelamento\":\"Teste de cancelamento\"}";

    @BeforeEach void setup() {
        oldHub = UrlConfig.URL_CONFIANCA_HUB; oldManager = UrlConfig.URL_CONFIANCA_MANAGER;
        UrlConfig.URL_CONFIANCA_HUB = "http://hub.test/"; UrlConfig.URL_CONFIANCA_MANAGER = "http://manager.test/";
        var hubRest = new RestTemplate(); var managerRest = new RestTemplate();
        hubServer = MockRestServiceServer.bindTo(hubRest).build(); managerServer = MockRestServiceServer.bindTo(managerRest).build();
        var auth = mock(ConfAppService.class); var token = new ConfAppResp(); token.setToken("teste");
        when(auth.token()).thenReturn(token);
        var hub = new HubCarroClient(hubRest); ReflectionTestUtils.setField(hub, "confAppService", auth);
        var service = new CarroReservaService(managerRest);
        ReflectionTestUtils.setField(service, "confAppService", auth); ReflectionTestUtils.setField(service, "hubCarroClient", hub);
        var controller = new CarrosController(mock(CarrosService.class));
        ReflectionTestUtils.setField(controller, "carroReservaService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach void cleanup() {
        UrlConfig.URL_CONFIANCA_HUB = oldHub; UrlConfig.URL_CONFIANCA_MANAGER = oldManager;
    }

    private void hubReturns(String response, String expectedIp) {
        hubServer.expect(requestTo("http://hub.test/api/carro/cancelarReserva")).andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath("$.localizador").value("CAR123"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.ip").value(expectedIp))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    private ResultActions cancel(String body, String remoteIp) throws Exception {
        return mvc.perform(post("/v1/carro/cancelarReserva").contentType(MediaType.APPLICATION_JSON).content(body)
                .header("X-Forwarded-For", "198.51.100.20")
                .with(req -> { req.setRemoteAddr(remoteIp); return req; }));
    }

    @ParameterizedTest @ValueSource(strings = {
        "[]", "[null]", "[{}]", "[{\"success\":null}]", "[{\"success\":false}]",
        "[{\"success\":true,\"error\":[{\"error_description\":\"Recusado\"}]}]",
        "[{\"success\":true},{\"success\":false,\"message\":\"Recusado\"}]"
    })
    void naoAtualizaManagerSemConfirmacaoDoFornecedor(String response) throws Exception {
        hubReturns(response, "192.0.2.10");
        cancel(REQUEST, "192.0.2.10").andExpect(status().isOk()).andExpect(jsonPath("$[0].success").value(false));
        hubServer.verify(); managerServer.verify();
    }

    @Test void preservaRecusaDoIpSemCancelarNoManager() throws Exception {
        hubReturns("[{\"success\":false,\"error\":[{\"error_description\":\"It is mandatory to send the IP.\"}]}]", "192.0.2.10");
        cancel(REQUEST, "192.0.2.10").andExpect(status().isOk()).andExpect(jsonPath("$[0].success").value(false))
                .andExpect(jsonPath("$[0].message").value("It is mandatory to send the IP."));
        hubServer.verify(); managerServer.verify();
    }

    private void expectManagerSuccess() {
        managerServer.expect(requestTo("http://manager.test/carroReserva/cancelar")).andExpect(method(HttpMethod.PUT))
                .andExpect(MockRestRequestMatchers.jsonPath("$.localizador").value("CAR123"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.codgUsuarioCancelamento").value(42))
                .andExpect(MockRestRequestMatchers.jsonPath("$.descricaoMotivoCancelamento").value("Teste de cancelamento"))
                .andRespond(withNoContent());
    }

    @Test void completaIpAntesDoHubEAtualizaManagerAposConfirmacao() throws Exception {
        hubReturns("[{\"success\":true,\"error\":[]}]", "192.0.2.10"); expectManagerSuccess();
        cancel(REQUEST, "192.0.2.10").andExpect(status().isOk()).andExpect(jsonPath("$[0].success").value(true));
        hubServer.verify(); managerServer.verify();
    }

    @Test void preservaOrigemPreenchidaPeloGateway() throws Exception {
        hubReturns("[{\"success\":true}]", "2001:db8::10"); expectManagerSuccess();
        String body = REQUEST.replace("\"localizador\":", "\"ip\":\"2001:db8::10\",\"localizador\":");
        cancel(body, "127.0.0.1").andExpect(status().isOk()).andExpect(jsonPath("$[0].success").value(true));
        hubServer.verify(); managerServer.verify();
    }

    @Test void falhaDePersistenciaInformaQueFornecedorJaCancelou() throws Exception {
        hubReturns("[{\"success\":true}]", "192.0.2.10");
        managerServer.expect(requestTo("http://manager.test/carroReserva/cancelar")).andRespond(withServerError());
        cancel(REQUEST, "192.0.2.10").andExpect(status().isOk()).andExpect(jsonPath("$[0].success").value(false))
                .andExpect(jsonPath("$[0].message", containsString("O fornecedor confirmou o cancelamento")));
        hubServer.verify(); managerServer.verify();
    }

    @Test void reservaSemLocalizadorNaoAcionaFornecedorOuManager() throws Exception {
        cancel("{\"cancelarReservaCarroRequestDTO\":{}}", "192.0.2.10")
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].success").value(false));
        hubServer.verify(); managerServer.verify();
    }
}
