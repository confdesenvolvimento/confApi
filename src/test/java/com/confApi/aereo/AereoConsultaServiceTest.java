package com.confApi.aereo;

import com.confApi.aereo.dto.*;
import com.confApi.chatconfianca.client.ChatConfiancaManagerClient;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.db.confManager.reservaAereo.ReservaAereoValoresRQ;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AereoConsultaServiceTest {
    final ObjectMapper mapper = new ObjectMapper();
    final AereoClient aereo = mock(AereoClient.class);
    final ChatConfiancaManagerClient manager = mock(ChatConfiancaManagerClient.class);
    final AereoConsultaService service = new AereoConsultaService(aereo, manager);
    ConsultarLocalizadorRequest req; ConsultarLocalizadorResponse resposta; ReservaAereo banco;
    @BeforeEach void preparar() throws Exception {
        req = new ConsultarLocalizadorRequest(); req.setSistema("Azul"); req.setLocalizador("ABC123");
        Agencia agencia = new Agencia(); agencia.setCodgAgencia("59"); req.setAgencia(agencia);
        banco = mapper.readValue("""
            {"codgReservaAereo":10,"localizador":"ABC123","status":3,"codgAgencia":{"codgAgencia":333,"codgSistemaBackOffice":"59"},"codgSistema":{"nomeSistema":"Azul"},
             "passageiros":[{"codgPassageiro":7,"nomePassageiro":"ANA","sobrenomePassageiro":"SILVA","tipoPassageiro":1,
             "reservaValores":[{"moeda":"BRL","valorTarifa":1148,"valorTarifaNet":1148,"valorTaxaEmbarque":83.30,"valorDu":114.80}]}]}
            """, ReservaAereo.class);
        resposta = mapper.readValue("""
            {"reservas":[{"sistema":"Azul","localizador":"ABC123","passageiros":[{"nome":"ANA","sobrenome":"SILVA"}],
             "valorReserva":{"valor":2212.37,"valorBase":{"total":2212.37,"valorPassageiroList":[
             {"nomePassageiro":"ANA SILVA","tarifa":1935.52,"taxaEmbarque":83.30,"taxaDU":193.55,"total":2212.37}]}}}]}
            """, ConsultarLocalizadorResponse.class);
        when(manager.get("reservaAereo/localizador/ABC123", ReservaAereo.class)).thenReturn(banco);
        when(aereo.carregarReserva(req)).thenReturn(resposta);
    }
    ReservaAereoValoresRQ pedidoEnviado() {
        var c = ArgumentCaptor.forClass(ReservaAereoValoresRQ.class);
        verify(manager).put(eq("reservaAereo/atualizarValoresFornecedor/10"), c.capture()); return c.getValue();
    }
    @Test void codigoBackoffice59AtualizaReservaDaAgenciaInterna333SemPagamento() {
        assertSame(resposta, service.carregarReserva(req)); var pedido = pedidoEnviado();
        assertEquals(2212.37, pedido.total()); assertEquals(1346.10, pedido.anteriores().get(0).total());
        assertEquals(1935.52, pedido.passageiros().get(0).tarifaNet()); assertEquals(7, pedido.passageiros().get(0).codgPassageiro());
        assertEquals(83.30, pedido.passageiros().get(0).embarque()); assertEquals(193.55, pedido.passageiros().get(0).du());
        assertTrue(resposta.getValoresAtualizados()); assertTrue(resposta.getValoresSincronizados());
        var ordem = inOrder(manager, aereo); ordem.verify(manager).get("reservaAereo/localizador/ABC123", ReservaAereo.class);
        ordem.verify(aereo).carregarReserva(req); ordem.verify(manager).put(anyString(), any());
        verify(manager, never()).post(anyString(), any(), any(Class.class));
    }
    @Test void repeticaoSemDiferencaNaoReenviaGravacao() {
        service.carregarReserva(req); var pedido = pedidoEnviado(); var v = banco.getPassageiros().get(0).getReservaValores().get(0);
        v.setValorTarifa(1935.52D); v.setValorTarifaNet(1935.52D); v.setValorDu(193.55D);
        service.carregarReserva(req);
        verify(manager, times(1)).put(anyString(), any()); assertFalse(resposta.getValoresAtualizados()); assertTrue(resposta.getValoresSincronizados());
    }
    @Test void preservaMargemETaxasLocaisNoPedidoSemAlterarSnapshot() {
        var v = banco.getPassageiros().get(0).getReservaValores().get(0); v.setValorTarifa(1160D); v.setValorRav(7D); v.setValorRc(3D);
        service.carregarReserva(req); var p = pedidoEnviado();
        assertEquals(2234.37, p.total()); assertEquals(1947.52D, p.passageiros().get(0).tarifa());
        assertEquals(1935.52D, p.passageiros().get(0).tarifaNet()); assertEquals(7D, p.passageiros().get(0).rav()); assertEquals(3D, p.passageiros().get(0).rc());
        assertEquals(1160D, v.getValorTarifa()); assertEquals(1148D, v.getValorTarifaNet());
    }
    @Test void dadosIncompletosOuTotalDivergenteNaoSaoEnviados() {
        resposta.getReservas().get(0).getValorReserva().setValor(1D);
        service.carregarReserva(req); verify(manager, never()).put(anyString(), any()); assertFalse(resposta.getValoresSincronizados());
    }
    @Test void passageiroDiferenteOuHomonimoNaoRecebeValoresDeOutraPessoa() {
        resposta.getReservas().get(0).getPassageiros().get(0).setNome("MARIANA");
        service.carregarReserva(req); verify(manager, never()).put(anyString(), any()); assertFalse(resposta.getValoresSincronizados());
    }
    @Test void agenciaDivergenteImpedeGravacaoMasPreservaConsultaOperacional() {
        req.getAgencia().setCodgAgencia("60"); assertSame(resposta, service.carregarReserva(req));
        verify(manager, never()).put(anyString(), any()); assertFalse(resposta.getValoresSincronizados());
    }
    @Test void idInternoNaoEhAceitoNoLugarDoCodigoBackoffice() {
        req.getAgencia().setCodgAgencia("333"); service.carregarReserva(req);
        verify(manager, never()).put(anyString(), any()); assertFalse(resposta.getValoresSincronizados());
    }
    @Test void backofficeAusenteNaoUsaIdInternoComoCorrespondenciaAlternativa() {
        banco.getCodgAgencia().setCodgSistemaBackOffice(null); req.getAgencia().setCodgAgencia("333");
        service.carregarReserva(req); verify(manager, never()).put(anyString(), any()); assertFalse(resposta.getValoresSincronizados());
    }
    @Test void falhaDoManagerNaoEhApresentadaComoAtualizacaoConcluida() {
        doThrow(new IllegalStateException("indisponivel")).when(manager).put(anyString(), any());
        service.carregarReserva(req); assertFalse(resposta.getValoresAtualizados()); assertFalse(resposta.getValoresSincronizados());
        verify(aereo, times(1)).carregarReserva(req); verify(manager, times(1)).put(anyString(), any());
    }
    @Test void erroDeLeituraLocalAindaPermiteConsultarFornecedorSemGravacao() {
        when(manager.get(anyString(), eq(ReservaAereo.class))).thenThrow(new IllegalStateException("indisponivel"));
        assertSame(resposta, service.carregarReserva(req)); verify(manager, never()).put(anyString(), any()); assertFalse(resposta.getValoresSincronizados());
    }
    @Test void respostaAntigaRecusadaPeloManagerPermanecePendenteSemRepeticao() {
        doThrow(new IllegalStateException("Valores alterados durante a consulta")).when(manager).put(anyString(), any());
        service.carregarReserva(req); assertEquals(1346.10, pedidoEnviado().anteriores().get(0).total()); assertFalse(resposta.getValoresSincronizados());
    }
    @Test void outrosFornecedoresMantemConsultaSemSincronizacaoAzul() {
        req.setSistema("Wooba"); assertSame(resposta, service.carregarReserva(req)); verifyNoInteractions(manager);
    }
    @Test void controllersV1EV2UsamOrquestracaoDaConfApi() {
        var regras = mock(AereoRegrasReservaService.class); var consulta = mock(AereoConsultaService.class);
        when(consulta.carregarReserva(req)).thenReturn(resposta); when(regras.enriquecer(resposta)).thenReturn(resposta);
        var v1 = new AereoController(mock(AereoService.class), regras); ReflectionTestUtils.setField(v1, "consultaService", consulta);
        var v2 = new AereoControllerV2(mock(AereoService.class), regras); ReflectionTestUtils.setField(v2, "consultaService", consulta);
        assertSame(resposta, v1.carregaReserva(req)); assertSame(resposta, v2.carregaReserva(req)); verify(consulta, times(2)).carregarReserva(req);
    }
}
