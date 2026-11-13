package com.confApi.seguros;

import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.db.confManager.seguro.reserva.SeguroReserva;
import com.confApi.hub.seguro.HubSeguroClient;
import com.confApi.recebimento.RecebimentoService;
import com.confApi.seguros.dto.SeguroCompraModel;
import com.confApi.seguros.dto.SeguroReservaDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SegurosCompraIdentificacaoTest {
    SegurosService service;
    RecebimentoService recebimentos;
    HubSeguroClient hub;
    SegurosController controller;
    MockMvc mvc;
    ObjectMapper json = new ObjectMapper();

    @BeforeEach void setup() {
        service = mock(SegurosService.class);
        recebimentos = mock(RecebimentoService.class);
        hub = mock(HubSeguroClient.class);
        controller = new SegurosController(service);
        ReflectionTestUtils.setField(controller, "recebimentoService", recebimentos);
        ReflectionTestUtils.setField(controller, "hubSeguroClient", hub);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @ParameterizedTest @NullSource @ValueSource(ints = {0, -1})
    void agenciaInvalidaEhRecusadaAntesDeQualquerPagamento(Integer agencia) throws Exception {
        String body = "{\"identificacaoAgenciaModel\":{\"codgUsuario\":7,\"codgAgencia\":" + agencia + "}}";
        mvc.perform(post("/v1/seguro/comprar").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("mensagem").value("Informe uma agência válida antes de comprar o seguro."));
        verifyNoInteractions(recebimentos, service, hub);
    }

    @ParameterizedTest @NullSource @ValueSource(ints = {0, -1})
    void usuarioInvalidoEhRecusadoAntesDeQualquerPagamento(Integer usuario) throws Exception {
        String body = "{\"identificacaoAgenciaModel\":{\"codgUsuario\":" + usuario + ",\"codgAgencia\":42}}";
        mvc.perform(post("/v1/seguro/comprar").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(recebimentos, service, hub);
    }

    @Test void identificacaoAusenteNaoGeraErro500() throws Exception {
        mvc.perform(post("/v1/seguro/comprar").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(recebimentos, service, hub);
    }

    @Test void agenciaRecebidaNoJsonEhMantidaAteAReservaDoManager() throws Exception {
        SeguroCompraModel req = json.readValue("""
                {"identificacaoAgenciaModel":{"codgUsuario":7,"codgAgencia":42},
                 "contatoEmergencia":{"nome":"Teste"},"recebimento":{},
                 "plano":{"dataInicioCobertura":"2027-01-01","dataFinalCombertura":"2027-01-03",
                 "precoBaseBRL":100.0,"precoFinalBRL":120.0}}
                """, SeguroCompraModel.class);
        SeguroReserva reserva = new SeguroReserva(req);
        assertEquals(42, reserva.getCodgAgencia().getCodgAgencia());
        assertEquals(42, json.valueToTree(reserva).path("codgAgencia").path("codgAgencia").asInt());
    }

    @Test void compraValidaContinuaAteConfirmacaoDoFornecedor() throws Exception {
        SeguroCompraModel req = requisicaoValida();
        Recebimento pago = pagamento();
        SeguroReserva reserva = new SeguroReserva(); reserva.setCodgReservaSeguro(10);
        SeguroReservaDTO confirmacao = new SeguroReservaDTO(); confirmacao.setLocalizador("SEG123");
        when(recebimentos.criarRecebimento(req)).thenReturn(pago);
        when(service.comprar(req)).thenReturn(reserva);
        when(hub.efetuarReserva(req)).thenReturn(List.of(confirmacao));
        assertSame(reserva, controller.comprar(req));
        assertEquals("SEG123", reserva.getLocalizador());
        verify(service).atualizarReserva(reserva);
        verify(recebimentos, never()).cancelarRecebimento(any());
    }

    @Test void falhaDePersistenciaComRetornoNuloAcionaCancelamentoExistente() throws Exception {
        SeguroCompraModel req = requisicaoValida();
        Recebimento pago = pagamento();
        when(recebimentos.criarRecebimento(req)).thenReturn(pago);
        when(service.comprar(req)).thenReturn(null);
        assertTrue(controller.comprar(req).getObservacaoInterna().startsWith("ERRO:"));
        verify(recebimentos).cancelarRecebimento(pago);
        verifyNoInteractions(hub);
    }

    @Test void retornoNuloDoPagamentoNaoCausaNullPointerException() throws Exception {
        assertTrue(controller.comprar(requisicaoValida()).getObservacaoInterna().startsWith("ERRO:"));
        verifyNoInteractions(service, hub);
    }

    SeguroCompraModel requisicaoValida() throws Exception {
        return json.readValue("{\"identificacaoAgenciaModel\":{\"codgUsuario\":7,\"codgAgencia\":42}}", SeguroCompraModel.class);
    }
    Recebimento pagamento() {
        Recebimento result = new Recebimento(); result.setCodgRecebimento(30); result.setStatus(1); return result;
    }
}
