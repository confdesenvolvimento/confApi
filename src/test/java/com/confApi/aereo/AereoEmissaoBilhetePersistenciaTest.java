package com.confApi.aereo;

import com.confApi.aereo.dto.ConsultarLocalizadorResponse;
import com.confApi.confApp.*;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.endPoints.usuario.UsuarioApi;
import com.confApi.hub.aereo.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** Fluxo HTTP simulado: nenhuma emissão ou gravação real é realizada. */
class AereoEmissaoBilhetePersistenciaTest {
    private final String hubOriginal = UrlConfig.URL_CONFIANCA_HUB;
    private final String managerOriginal = UrlConfig.URL_CONFIANCA_MANAGER;
    private AereoClientV2 client;
    private MockRestServiceServer server;
    private ReservaAereoModel entrada;
    private static final String BASE = "http://fixture/manager/";
    private static final String BILHETE = "{\"codgBilhete\":91,\"numrBilhete\":\"999-0000000001\",\"status\":1,\"dataCancelamento\":null}";

    @BeforeEach void setup() {
        UrlConfig.URL_CONFIANCA_HUB = "http://fixture/hub/";
        UrlConfig.URL_CONFIANCA_MANAGER = BASE;
        RestTemplate rest = new RestTemplate();
        server = MockRestServiceServer.bindTo(rest).build();
        client = new AereoClientV2(rest);
        ConfAppService auth = mock(ConfAppService.class);
        ConfAppResp token = new ConfAppResp(); token.setToken("fixture");
        when(auth.token()).thenReturn(token);
        ReflectionTestUtils.setField(client,"confAppService",auth);
        UsuarioApi usuarios = mock(UsuarioApi.class);
        Usuario usuario = new Usuario(); usuario.setCodgUsuario(42);
        when(usuarios.consultaUsuarioByLogin(anyString())).thenReturn(usuario);
        ReflectionTestUtils.setField(client,"usuarioApi",usuarios);
        entrada = new ReservaAereoModel(); entrada.setCodgReservaAereoDB(77L);
        entrada.setLocalizador("ABC123"); entrada.setSistema("Wooba"); entrada.setUsuarioCriacao("fixture");
        entrada.setUsuarioCriacao2(usuario);
        entrada.setStatusReserva("Emitida");
        var agencia = new com.confApi.db.confManager.agencia.dto.Agencia(99);
        agencia.setCodgUnidade(new com.confApi.db.confManager.unidade.dto.Unidade(7)); entrada.setAgencia(agencia);
        PassageiroModel pax = new PassageiroModel(); pax.setCodgPassageiroDb(88);
        pax.setNome("PASSAGEIRO"); pax.setSobrenome("TESTE");
        BilheteModel bilhete = new BilheteModel(); bilhete.setNumeroBilhete("999-0000000001");
        bilhete.setStatus(1); bilhete.setDataEmissao(new Date(1794316541000L));
        pax.setBilhetes(List.of(bilhete)); entrada.setPassageiros(List.of(pax));
        AereoService conversor = mock(AereoService.class);
        when(conversor.convertToReservaAereoModel(any(ConsultarLocalizadorResponse.class),eq(entrada),eq(true))).thenReturn(entrada);
        ReflectionTestUtils.setField(client,"aereoService",conversor);
    }

    @AfterEach void restore() { UrlConfig.URL_CONFIANCA_HUB=hubOriginal; UrlConfig.URL_CONFIANCA_MANAGER=managerOriginal; }

    @Test void statusEmitidaPermiteGravarBilheteEConfirmarVinculoAntesDoSucesso() {
        iniciar(3, "[]"); gravar(HttpStatus.CREATED);
        releitura(reservaJson(3,"["+BILHETE+"]")); concluir();
        assertNull(client.emitir(entrada,false).getMsg()); server.verify();
    }

    @ParameterizedTest @ValueSource(ints={1,2})
    void naoGravaBilheteQuandoManagerNaoConfirmouStatusEmitida(int status) {
        iniciar(status,"[]");
        assertTrue(client.emitir(entrada,false).getMsg().contains("estado emitido")); server.verify();
    }

    @Test void falhaAoGravarBilheteNaoViraSucesso() {
        iniciar(3,"[]"); gravar(HttpStatus.INTERNAL_SERVER_ERROR);
        assertTrue(client.emitir(entrada,false).getMsg().contains("gravação dos bilhetes")); server.verify();
    }

    @Test void resposta201SemBilheteNaReleituraNaoViraSucesso() {
        iniciar(3,"[]"); gravar(HttpStatus.CREATED); releitura(reservaJson(3,"[]"));
        assertTrue(client.emitir(entrada,false).getMsg().contains("gravação dos bilhetes")); server.verify();
    }

    @ParameterizedTest @ValueSource(strings={"cancelado","outraReserva","outroPassageiro","outroBilhete"})
    void releituraExigeBilheteAtivoNaMesmaReservaEPassageiro(String caso) {
        iniciar(3,"[]"); gravar(HttpStatus.CREATED);
        String confirmada=reservaJson(3,"["+BILHETE+"]");
        confirmada=switch(caso) {
            case "cancelado" -> confirmada.replace("\"dataCancelamento\":null","\"dataCancelamento\":1794316541000");
            case "outraReserva" -> confirmada.replace("\"codgReservaAereo\":77","\"codgReservaAereo\":78");
            case "outroPassageiro" -> confirmada.replace("\"codgPassageiro\":88","\"codgPassageiro\":89").replace("PASSAGEIRO","OUTRO");
            default -> confirmada.replace("999-0000000001","999-0000000002");
        };
        releitura(confirmada);
        assertTrue(client.emitir(entrada,false).getMsg().contains("gravação dos bilhetes")); server.verify();
    }

    @Test void bilheteJaPersistidoNaoEhInseridoOutraVez() {
        iniciar(3,"["+BILHETE+"]"); releitura(reservaJson(3,"["+BILHETE+"]")); concluir();
        assertNull(client.emitir(entrada,false).getMsg()); server.verify();
    }

    @Test void passageiroSemVinculoNaoEhIgnorado() {
        iniciar(3,"[]"); entrada.getPassageiros().get(0).setCodgPassageiroDb(89);
        entrada.getPassageiros().get(0).setNome("OUTRO");
        assertTrue(client.emitir(entrada,false).getMsg().contains("passageiro")); server.verify();
    }

    private void iniciar(int status,String bilhetes) {
        server.expect(requestTo("http://fixture/hub/api/aereo/emitir"))
            .andExpect(method(HttpMethod.POST)).andRespond(withSuccess("{}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://fixture/hub/api/aereo/consultar"))
            .andRespond(withSuccess("{\"reservas\":[]}",MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE+"reservaAereo/emitir/77"))
            .andExpect(method(HttpMethod.PUT)).andRespond(withSuccess());
        releitura(reservaJson(status,bilhetes));
    }
    private String reservaJson(int status,String bilhetes) {
        return "{\"codgReservaAereo\":77,\"localizador\":\"ABC123\",\"status\":"+status
            +",\"passageiros\":[{\"codgPassageiro\":88,\"nomePassageiro\":\"PASSAGEIRO\",\"sobrenomePassageiro\":\"TESTE\",\"bilhetes\":"+bilhetes+"}]}";
    }
    private void gravar(HttpStatus status) {
        server.expect(requestTo(BASE+"bilheteAereo")).andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.codgPassageiro.codgPassageiro").value(88))
            .andExpect(jsonPath("$.numrBilhete").value("999-0000000001"))
            .andExpect(jsonPath("$.status").value(1)).andRespond(withStatus(status));
    }
    private void releitura(String json) {
        server.expect(requestTo(BASE+"reservaAereo/localizador/ABC123"))
            .andExpect(method(HttpMethod.GET)).andRespond(withSuccess(json,MediaType.APPLICATION_JSON));
    }
    private void concluir() {
        server.expect(requestTo(BASE+"notificacao?tipo=Exclusivo")).andRespond(withSuccess("[]",MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE+"recebimento/Wooba?autorizacao=false")).andRespond(withSuccess());
    }
}
