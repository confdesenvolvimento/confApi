package com.confApi.aereo;

import com.confApi.aereo.dto.*;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.hub.aereo.ReservaAereoModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AereoControllerV2CompatibilidadeTest {
    private final AereoClient atual = mock(AereoClient.class);
    private final AereoClientV2 v2 = mock(AereoClientV2.class);
    private MockMvc mvc;

    @BeforeEach void configurar() {
        AereoControllerV2 controller = new AereoControllerV2();
        ReflectionTestUtils.setField(controller, "aereoClient", atual);
        ReflectionTestUtils.setField(controller, "aereoClientV2", v2);
        ReflectionTestUtils.setField(controller, "regrasReservaService", mock(AereoRegrasReservaService.class));
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test void pesquisaAtualRetornaObjetoEVersaoAppRetornaLista() throws Exception {
        when(v2.pesquisarDisponibilidade(any())).thenReturn(new PesquisaResponse());
        when(atual.pesquisarDisponibilidadeV2(any())).thenReturn(List.of(new PesquisaResponse()));
        String objeto = enviar("/pesquisar", "{\"tipoPesquisa\":\"ROUNDTRIP\"}");
        String lista = enviar("/pesquisarApp", "{\"tipoPesquisa\":\"ROUNDTRIP\"}");
        assertTrue(objeto.startsWith("{"));
        assertTrue(lista.startsWith("["));
        verify(v2).pesquisarDisponibilidade(any());
        verify(atual).pesquisarDisponibilidadeV2(any());
    }

    @Test void carregarReservaModelPreservaLocalizadorEIdentidadeDoManager() throws Exception {
        ReservaAereoModel resultado = new ReservaAereoModel();
        resultado.setLocalizador("TESTE1");
        when(v2.carregarReservaAerea(any())).thenReturn(resultado);
        mvc.perform(post("/v2/aereo/carregarReservaModel").contentType(MediaType.APPLICATION_JSON)
                .content("{\"codgReservaAereo\":123,\"localizador\":\"TESTE1\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.localizador").value("TESTE1"));
        ArgumentCaptor<ReservaAereo> pedido = ArgumentCaptor.forClass(ReservaAereo.class);
        verify(v2).carregarReservaAerea(pedido.capture());
        assertEquals(123, pedido.getValue().getCodgReservaAereo());
        assertEquals("TESTE1", pedido.getValue().getLocalizador());
        verifyNoInteractions(atual);
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void emissaoAtualPreservaOpcaoDeLink(boolean link) throws Exception {
        ReservaAereoModel resultado = new ReservaAereoModel();
        resultado.setLocalizador("TESTE1");
        when(v2.emitir(any(), eq(link), any())).thenReturn(resultado);
        enviar("/emitir/" + link, "{\"localizador\":\"TESTE1\"}");
        ArgumentCaptor<ReservaAereoModel> pedido = ArgumentCaptor.forClass(ReservaAereoModel.class);
        verify(v2).emitir(pedido.capture(), eq(link), any());
        assertEquals("TESTE1", pedido.getValue().getLocalizador());
        verifyNoInteractions(atual);
    }

    @Test void tarifacaoEReservaPreservamContratoPreReserva() throws Exception {
        PreReserva resultado = new PreReserva();
        resultado.setValorTotalGeral(100.0);
        when(v2.tarifar(any())).thenReturn(resultado);
        when(v2.reserva(any())).thenReturn(new ReservarResponse());
        String json = "{\"sistema\":\"Wooba\",\"codgPacote\":321,\"identificacaoViagemMultipla\":[\"IDA\",\"VOLTA\"],\"trechos\":[],\"passageiros\":[]}";
        enviar("/tarifar", json);
        enviar("/reservar", json);
        ArgumentCaptor<PreReserva> tarifa = ArgumentCaptor.forClass(PreReserva.class);
        ArgumentCaptor<PreReserva> reserva = ArgumentCaptor.forClass(PreReserva.class);
        verify(v2).tarifar(tarifa.capture());
        verify(v2).reserva(reserva.capture());
        for (PreReserva pedido : List.of(tarifa.getValue(), reserva.getValue())) {
            assertEquals(321, pedido.getCodgPacote());
            assertEquals("Wooba", pedido.getSistema());
            assertEquals(List.of("IDA","VOLTA"), pedido.getIdentificacaoViagemMultipla());
        }
        verifyNoInteractions(atual);
    }

    @Test void novasRotasDeEmissaoCoexistemComContratoAtual() throws Exception {
        when(atual.iniciarEmissao(any())).thenReturn(Map.of("localizador","TESTE1"));
        when(atual.emitir(any(), any())).thenReturn(new EmitirResponse());
        enviar("/iniciarEmissao", "{\"localizador\":\"TESTE1\",\"sistema\":\"Wooba\"}");
        enviar("/emitir", "{\"localizador\":\"TESTE1\",\"sistema\":\"Wooba\",\"formaDePagamento\":70}");
        ArgumentCaptor<EmitirRequest> pedido = ArgumentCaptor.forClass(EmitirRequest.class);
        verify(atual).iniciarEmissao(any());
        verify(atual).emitir(pedido.capture(), any());
        assertEquals("TESTE1", pedido.getValue().getLocalizador());
        assertEquals(70, pedido.getValue().getFormaDePagamento());
        verifyNoInteractions(v2);
    }

    @Test void cancelarReservaPreservaContratoModelERetornoBooleano() throws Exception {
        when(v2.cancelarReserva(any())).thenReturn(true);
        assertEquals("true", enviar("/cancelarReserva",
                "{\"localizador\":\"TESTE1\",\"codgReservaAereoDB\":123}"));
        ArgumentCaptor<ReservaAereoModel> pedido = ArgumentCaptor.forClass(ReservaAereoModel.class);
        verify(v2).cancelarReserva(pedido.capture());
        assertEquals("TESTE1", pedido.getValue().getLocalizador());
        assertEquals(123L, pedido.getValue().getCodgReservaAereoDB());
        verifyNoInteractions(atual);
    }

    @Test void cancelarReservaRequestUsaRotaDistintaERetornaRespostaDoHub() throws Exception {
        CancelarReservaResponse resultado = new CancelarReservaResponse();
        resultado.setStatus("Cancelado");
        when(atual.cancelarReserva(any())).thenReturn(resultado);
        mvc.perform(post("/v2/aereo/cancelarReservaRequest").contentType(MediaType.APPLICATION_JSON)
                .content("{\"localizador\":\"TESTE1\",\"motivo\":\"Desistencia\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("Cancelado"));
        ArgumentCaptor<CancelarReservaRequest> pedido = ArgumentCaptor.forClass(CancelarReservaRequest.class);
        verify(atual).cancelarReserva(pedido.capture());
        assertEquals("TESTE1", pedido.getValue().getLocalizador());
        assertEquals("Desistencia", pedido.getValue().getMotivo());
        verifyNoInteractions(v2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/tarifar", "/reservar", "/cancelarReserva", "/cancelarReservaRequest", "/cancelarBilhete"})
    void operadoraPreservaDtoETokenMesmoComCamposDoModelo(String rota) throws Exception {
        String json = """
                {"tipoTarifa":"OPER","codgReservaAereo":123,"localizador":"TESTE1",
                 "criteriosOperadora":{"origem":"CGB"},"chaveSelecao":"selecao-teste",
                 "codgPacote":321,"codgReservaHotel":456,"precoEsperadoOperadora":123.45,
                 "trechos":[],"passageiros":[],"codgReservaAereoDB":123}
                """;
        mvc.perform(post("/v2/aereo" + rota).contentType(MediaType.APPLICATION_JSON)
                        .header("X-Usuario-Token", "token-usuario-teste").content(json))
                .andExpect(status().isOk());
        switch (rota) {
            case "/tarifar" -> verify(atual).tarifar(argThat(req ->
                    "OPER".equals(req.getTipoTarifa()) && req.getCodgPacote() == 321
                            && req.getCodgReservaHotel() == 456L
                            && "CGB".equals(req.getCriteriosOperadora().path("origem").asText())
                            && "selecao-teste".equals(req.getChaveSelecao())
                            && req.getPrecoEsperadoOperadora().toPlainString().equals("123.45")),
                    eq("token-usuario-teste"));
            case "/reservar" -> verify(atual).reserva(argThat(req ->
                    "OPER".equals(req.getTipoTarifa()) && req.getCodgPacote() == 321
                            && "selecao-teste".equals(req.getChaveSelecao())), eq("token-usuario-teste"));
            case "/cancelarReserva", "/cancelarReservaRequest" -> verify(atual).cancelarReserva(argThat(req ->
                    "OPER".equals(req.getTipoTarifa()) && req.getCodgReservaAereo() == 123
                            && "TESTE1".equals(req.getLocalizador())), eq("token-usuario-teste"));
            case "/cancelarBilhete" -> verify(atual).cancelarBilhete(argThat(req ->
                    "OPER".equals(req.getTipoTarifa()) && req.getCodgReservaAereo() == 123
                            && "TESTE1".equals(req.getLocalizador())), eq("token-usuario-teste"));
            default -> fail("Rota inesperada: " + rota);
        }
        verifyNoInteractions(v2);
    }

    @Test void contratosDiretosConsContinuamNasMesmasRotas() throws Exception {
        when(atual.tarifar(any())).thenReturn(new TarifarResponse());
        when(atual.reserva(any())).thenReturn(new ReservarResponse());
        when(atual.cancelarReserva(any())).thenReturn(new CancelarReservaResponse());
        when(atual.cancelarBilhete(any())).thenReturn(new ConsultarEticketResponse());
        enviar("/tarifar", "{\"sistema\":\"Wooba\",\"classes\":[],\"identificacaoViagem\":\"IDA\"}");
        enviar("/reservar", "{\"sistema\":\"Wooba\",\"classesSelecionadas\":[],\"identificacaoDaViagem\":\"IDA\"}");
        enviar("/cancelarReserva", "{\"localizador\":\"TESTE1\",\"motivo\":\"Desistencia\"}");
        enviar("/cancelarBilhete", "{\"localizador\":\"TESTE1\",\"eticket\":\"123456\"}");
        verify(atual).tarifar(argThat(req -> "IDA".equals(req.getIdentificacaoViagem())));
        verify(atual).reserva(argThat(req -> "IDA".equals(req.getIdentificacaoDaViagem())));
        verify(atual).cancelarReserva(argThat(req -> "TESTE1".equals(req.getLocalizador())));
        verify(atual).cancelarBilhete(argThat(req -> "123456".equals(req.getEticket())));
        verifyNoInteractions(v2);
    }

    @Test void cancelarBilhetePreservaContratoModel() throws Exception {
        when(v2.cancelarBilhete(any())).thenReturn(new ConsultarEticketResponse());
        enviar("/cancelarBilhete", "{\"localizador\":\"TESTE1\",\"codgReservaAereoDB\":123,\"passageiros\":[]}");
        verify(v2).cancelarBilhete(argThat(req -> "TESTE1".equals(req.getLocalizador())
                && req.getCodgReservaAereoDB() == 123L));
        verifyNoInteractions(atual);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/tarifar", "/reservar", "/cancelarReserva", "/cancelarBilhete"})
    void modalidadeInvalidaOuCorpoInvalidoNaoExecutamOperacao(String rota) throws Exception {
        for (String json : List.of("{\"tipoTarifa\":\"OUTRA\",\"trechos\":[],\"passageiros\":[]}",
                "[]", "{\"codgReservaAereoDB\":\"invalido\",\"classes\":{},\"classesSelecionadas\":{}}")) {
            mvc.perform(post("/v2/aereo" + rota).contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(atual, v2);
    }

    private String enviar(String caminho, String json) throws Exception {
        return mvc.perform(post("/v2/aereo"+caminho).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
}
