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
        when(v2.emitir(any(), eq(link))).thenReturn(resultado);
        enviar("/emitir/" + link, "{\"localizador\":\"TESTE1\"}");
        ArgumentCaptor<ReservaAereoModel> pedido = ArgumentCaptor.forClass(ReservaAereoModel.class);
        verify(v2).emitir(pedido.capture(), eq(link));
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
        when(atual.emitir(any())).thenReturn(new EmitirResponse());
        enviar("/iniciarEmissao", "{\"localizador\":\"TESTE1\",\"sistema\":\"Wooba\"}");
        enviar("/emitir", "{\"localizador\":\"TESTE1\",\"sistema\":\"Wooba\",\"formaDePagamento\":70}");
        ArgumentCaptor<EmitirRequest> pedido = ArgumentCaptor.forClass(EmitirRequest.class);
        verify(atual).iniciarEmissao(any());
        verify(atual).emitir(pedido.capture());
        assertEquals("TESTE1", pedido.getValue().getLocalizador());
        assertEquals(70, pedido.getValue().getFormaDePagamento());
        verifyNoInteractions(v2);
    }

    private String enviar(String caminho, String json) throws Exception {
        return mvc.perform(post("/v2/aereo"+caminho).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
}