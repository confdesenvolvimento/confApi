package com.confApi.aereo;

import com.confApi.aereo.dto.*;
import com.confApi.confApp.*;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.endPoints.usuario.UsuarioApi;
import com.confApi.hub.aereo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** Valida o transporte real com respostas simuladas, sem subir serviços ou acessar banco. */
class AereoClientV2MigrationTest {
    private final String hubOriginal = UrlConfig.URL_CONFIANCA_HUB;
    private final String managerOriginal = UrlConfig.URL_CONFIANCA_MANAGER;
    private AereoClientV2 client;
    private AereoService conversor;
    private MockRestServiceServer server;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach void setup() {
        UrlConfig.URL_CONFIANCA_HUB = "http://fixture/hub/";
        UrlConfig.URL_CONFIANCA_MANAGER = "http://fixture/manager/";
        RestTemplate rest = new RestTemplate();
        server = MockRestServiceServer.bindTo(rest).build();
        client = new AereoClientV2(rest);
        ConfAppService auth = mock(ConfAppService.class);
        ConfAppResp token = new ConfAppResp(); token.setToken("fixture");
        when(auth.token()).thenReturn(token);
        ReflectionTestUtils.setField(client, "confAppService", auth);
        conversor = mock(AereoService.class);
        ReflectionTestUtils.setField(client, "aereoService", conversor);
        UsuarioApi usuarios = mock(UsuarioApi.class);
        Usuario usuario = new Usuario(); usuario.setCodgUsuario(42);
        when(usuarios.consultaUsuarioByLogin(anyString())).thenReturn(usuario);
        ReflectionTestUtils.setField(client, "usuarioApi", usuarios);
    }

    @AfterEach void restore() {
        UrlConfig.URL_CONFIANCA_HUB = hubOriginal;
        UrlConfig.URL_CONFIANCA_MANAGER = managerOriginal;
    }

    @Test void recebimentoAceitaNomeAtualELegadoSemPerderIdentificador() throws Exception {
        for (String campo : List.of("codgRecebimento", "codgCodgRecebimento")) {
            RecebimentoModel model=json.readValue("{\""+campo+"\":123}",RecebimentoModel.class);
            assertEquals(123,model.getCodgRecebimento());
            var serialized=json.valueToTree(model);
            assertEquals(123,serialized.path("codgRecebimento").asInt());
            assertEquals(123,serialized.path("codgCodgRecebimento").asInt());
        }
    }

    @ParameterizedTest @ValueSource(strings={"http://fixture/hub", "http://fixture/hub/"})
    void tarifaUsaUrlNormalizadaEConservaClassesEQuantidade(String base) throws Exception {
        UrlConfig.URL_CONFIANCA_HUB = base;
        PreReserva entrada = json.readValue("""
            {"tipoTrecho":0,"tipoVooPesquisa":1,"qtdAdt":2,"qtdChd":1,"sistema":"Wooba",
             "trechos":[{"sistema":"Wooba","identificacaoDaViagem":"IDA",
               "familiaSelecionada":{"classe":"Y","baseTarifaria":"YTEST","familia":{"codgFamilia":"LIGHT"}},
               "voos":[{"numeroVoo":"1001","identificacaoDoVoo":"VOO1"}]}]}
            """, PreReserva.class);
        server.expect(requestTo("http://fixture/hub/api/aereo/tarifar"))
            .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization", "Bearer fixture"))
            .andExpect(jsonPath("$.identificacaoViagem").value("IDA"))
            .andExpect(jsonPath("$.classes[0].identificacaoDeVoo").value("VOO1"))
            .andExpect(jsonPath("$.classes[0].classe").value("Y"))
            .andExpect(jsonPath("$.passageiroTipoQtds[0].quantidade").value(2))
            .andRespond(withSuccess("{\"preco\":{\"total\":100}}", MediaType.APPLICATION_JSON));
        PreReserva saida = new PreReserva(); saida.setValorTotalGeral(100.0);
        when(conversor.montarPreReservaTarifada(eq(entrada), any())).thenReturn(saida);
        assertEquals(100.0, client.tarifar(entrada).getValorTotalGeral());
        server.verify();
    }

    @ParameterizedTest @ValueSource(strings={"2002-10-15", "15/10/2002"})
    void reservaTraduzAgenciaUnidadeENascimentoDoApp(String nascimento) throws Exception {
        PreReserva entrada = json.readValue("""
            {"tipoTrecho":0,"tipoVooPesquisa":1,"sistema":"Wooba",
             "usuario":{"id":42,"agencia":{"codgAgencia":99,"nomeAgencia":"Agência Teste","codgUnidade":{"codgUnidade":7}}},
             "contatos":[{"nome":"Teste","email":"teste@example.invalid"}],
             "passageiros":[{"nome":"PASSAGEIRO","sobrenome":"TESTE","nascimento":"%s"}],
             "trechos":[{"sistema":"Wooba","identificacaoDaViagem":"IDA",
               "familiaSelecionada":{"classe":"Y","baseTarifaria":"YTEST","familia":{"codgFamilia":"LIGHT"}},
               "voos":[{"numeroVoo":"1001","identificacaoDoVoo":"VOO1"}]}]}
            """.formatted(nascimento), PreReserva.class);
        server.expect(requestTo("http://fixture/hub/api/aereo/reservar"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.agencia.nome").value("99"))
            .andExpect(jsonPath("$.agencia.unidade").value("7"))
            .andExpect(jsonPath("$.classesSelecionadas[0].numero").value("1001"))
            .andExpect(jsonPath("$.identificacaoDaViagem").value("IDA"))
            .andExpect(jsonPath("$.passageiros[0].nascimento").value("2002-10-15T12:00:00"))
            .andRespond(withSuccess("{\"reservas\":[]}",MediaType.APPLICATION_JSON));
        assertTrue(client.reserva(entrada).getReservas().isEmpty());
        server.verify();
    }

    @Test void falhaNoManagerImpedeConfirmarCancelamento() {
        server.expect(requestTo("http://fixture/hub/api/aereo/cancelar"))
            .andRespond(withSuccess("{\"status\":\"Cancelado\"}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://fixture/manager/reservaAereo/cancelar/77"))
            .andExpect(method(HttpMethod.PUT)).andRespond(withServerError());
        assertFalse(client.cancelarReserva(reserva()));
        server.verify();
    }

    @ParameterizedTest @CsvSource({"77,2,ABC123,true", "77,0,ABC123,false", "78,2,ABC123,false", "77,2,OUTRA,false"})
    void cancelamentoExigeReleituraDaMesmaReserva(int id,int status,String pnr,boolean confirmado) {
        server.expect(requestTo("http://fixture/hub/api/aereo/cancelar"))
            .andRespond(withSuccess("{\"status\":\"Cancelado\"}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://fixture/manager/reservaAereo/cancelar/77"))
            .andExpect(method(HttpMethod.PUT)).andRespond(withSuccess());
        server.expect(requestTo("http://fixture/manager/reservaAereo/localizador/ABC123"))
            .andRespond(withSuccess("{\"codgReservaAereo\":"+id+",\"status\":"+status+",\"localizador\":\""+pnr+"\"}",MediaType.APPLICATION_JSON));
        if(confirmado) server.expect(requestTo("http://fixture/manager/notificacao?tipo=Exclusivo"))
            .andRespond(withSuccess("[]",MediaType.APPLICATION_JSON));
        assertEquals(confirmado,client.cancelarReserva(reserva()));
        server.verify();
    }

    @Test void emissaoPreservaAntifraudeEParaQuandoManagerNaoConfirma() {
        ReservaAereoModel entrada=reserva();
        server.expect(requestTo("http://fixture/hub/api/aereo/emitir"))
            .andExpect(header("X-Antifraude-Operacao","fixture-operation"))
            .andExpect(header("Authorization","Bearer fixture"))
            .andRespond(withSuccess("{}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://fixture/hub/api/aereo/consultar"))
            .andRespond(withSuccess("{\"reservas\":[]}",MediaType.APPLICATION_JSON));
        entrada.setStatusReserva("Emitida");
        PassageiroModel passageiro=new PassageiroModel();
        BilheteModel bilhete=new BilheteModel(); bilhete.setNumeroBilhete("9990000000001");
        passageiro.setBilhetes(List.of(bilhete)); entrada.setPassageiros(List.of(passageiro));
        when(conversor.convertToReservaAereoModel(any(),eq(entrada),eq(true))).thenReturn(entrada);
        server.expect(requestTo("http://fixture/manager/reservaAereo/emitir/77"))
            .andExpect(method(HttpMethod.PUT)).andRespond(withServerError());
        HttpHeaders headers=new HttpHeaders();headers.set("X-Antifraude-Operacao","fixture-operation");
        assertTrue(client.emitir(entrada,false,headers).getMsg().contains("Manager não confirmou"));
        server.verify();
    }

    private ReservaAereoModel reserva() {
        ReservaAereoModel r=new ReservaAereoModel();r.setCodgReservaAereoDB(77L);r.setLocalizador("ABC123");
        r.setSistema("Wooba");r.setUsuarioCriacao("fixture");r.setUsuarioCancelamento("fixture");
        r.setMotivoCancelamento("Desistencia");r.setIsCancelarTktsAtivos(false);
        var agencia=new com.confApi.db.confManager.agencia.dto.Agencia(99);
        agencia.setCodgUnidade(new com.confApi.db.confManager.unidade.dto.Unidade(7));r.setAgencia(agencia);
        return r;
    }
}
