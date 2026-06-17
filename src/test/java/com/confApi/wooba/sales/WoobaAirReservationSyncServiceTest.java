package com.confApi.wooba.sales;

import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.bilhete.BilheteAereo;
import com.confApi.db.confManager.companhiaAerea.CompanhiaAerea;
import com.confApi.db.confManager.formaPagamento.FormaPagamento;
import com.confApi.db.confManager.notificacao.NotificacaoApi;
import com.confApi.db.confManager.passageiro.Passageiro;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.endPoints.recebimento.RecebimentoApi;
import com.confApi.endPoints.reservaAereo.ReservaAereoApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WoobaAirReservationSyncServiceTest {

    private final ReservaAereoApi reservaAereoApi = mock(ReservaAereoApi.class);
    private final RecebimentoApi recebimentoApi = mock(RecebimentoApi.class);
    private final NotificacaoApi notificacaoApi = mock(NotificacaoApi.class);
    private final WoobaAirReservationSyncService service =
            new WoobaAirReservationSyncService(reservaAereoApi, recebimentoApi, notificacaoApi);

    @Test
    void deveCriarReservaSincronizarBilhetePagamentoENotificar() throws Exception {
        ReservaAereo reservaWooba = reservaWooba("ABC123", 3);

        ReservaAereo reservaDb = reservaDb("ABC123", 3);
        when(reservaAereoApi.findByLocalizadorCompanhia(eq("ABC123"), any(CompanhiaAerea.class)))
                .thenReturn(null, reservaDb, reservaDb);
        when(reservaAereoApi.criar(any())).thenReturn(reservaDb);
        when(recebimentoApi.findByReservaAereo(999)).thenReturn(List.of());

        WoobaAirReservationSyncResult result = service.sincronizar(reservaWooba);

        assertTrue(result.isCreated());
        assertEquals(List.of("1234567890"), result.getBilhetesGravados());
        assertEquals(1, result.getPagamentosGravados());
        assertFalse(result.isCreatedNotificationSent());
        assertTrue(result.isIssuedNotificationSent());

        ArgumentCaptor<ReservaAereo> criacaoCaptor = ArgumentCaptor.forClass(ReservaAereo.class);
        verify(reservaAereoApi).criar(criacaoCaptor.capture());
        assertNull(criacaoCaptor.getValue().getRecebimentos());
        assertNull(criacaoCaptor.getValue().getPassageiros().get(0).getBilhetes());
        String passageiroNovoJson = new ObjectMapper()
                .writeValueAsString(criacaoCaptor.getValue().getPassageiros().get(0));
        assertFalse(passageiroNovoJson.contains("\"codgPassageiro\":0"));

        verify(reservaAereoApi).atualizar(eq(999), any(ReservaAereo.class));
        verify(recebimentoApi).gravar(any(Recebimento.class));
        verify(notificacaoApi, times(1)).criarParaUsuario(any());
    }

    @Test
    void deveCriarReservaAtivaSemBilheteEPagamento() {
        ReservaAereo reservaWooba = reservaWooba("ATV123", 1);
        reservaWooba.setDataCriacao(null);
        reservaWooba.setDataLimiteEmissao(null);
        reservaWooba.setDataEmissao(null);
        reservaWooba.getPassageiros().get(0).setBilhetes(List.of());
        reservaWooba.setRecebimentos(List.of());

        ReservaAereo reservaDb = reservaDb("ATV123", 1);
        reservaDb.setDataEmissao(null);
        when(reservaAereoApi.findByLocalizadorCompanhia(eq("ATV123"), any(CompanhiaAerea.class)))
                .thenReturn(null, reservaDb, reservaDb);
        when(reservaAereoApi.criar(any())).thenReturn(reservaDb);

        WoobaAirReservationSyncResult result = service.sincronizar(reservaWooba);

        assertTrue(result.isCreated());
        assertTrue(result.getBilhetesGravados().isEmpty());
        assertEquals(0, result.getPagamentosGravados());
        assertTrue(result.isCreatedNotificationSent());
        assertFalse(result.isIssuedNotificationSent());

        ArgumentCaptor<ReservaAereo> criacaoCaptor = ArgumentCaptor.forClass(ReservaAereo.class);
        verify(reservaAereoApi).criar(criacaoCaptor.capture());
        ReservaAereo payloadCriacao = criacaoCaptor.getValue();
        assertEquals(1, payloadCriacao.getStatus());
        assertNotNull(payloadCriacao.getDataCriacao());
        assertNotNull(payloadCriacao.getDataLimiteEmissao());
        assertNull(payloadCriacao.getDataEmissao());
        assertNull(payloadCriacao.getRecebimentos());
        assertNull(payloadCriacao.getPassageiros().get(0).getBilhetes());

        verify(reservaAereoApi, never()).atualizar(any(), any());
        verify(reservaAereoApi, never()).atualizarStatus(any(), any());
        verify(recebimentoApi, never()).gravar(any());
        verify(notificacaoApi, times(1)).criarParaUsuario(any());
    }

    @Test
    void deveAtualizarBilheteEPagamentoExistentes() {
        ReservaAereo reservaWooba = reservaWooba("ABC123", 3);
        ReservaAereo reservaDb = reservaDb("ABC123", 1);

        BilheteAereo bilheteDb = new BilheteAereo();
        bilheteDb.setCodgBilhete(77);
        bilheteDb.setNumrBilhete("1234567890");
        bilheteDb.setStatus(0);
        reservaDb.getPassageiros().get(0).setBilhetes(List.of(bilheteDb));

        Recebimento recebimentoDb = recebimento("1234567890", 2);
        recebimentoDb.setCodgRecebimento(88);
        reservaDb.setRecebimentos(List.of(recebimentoDb));

        when(reservaAereoApi.findByLocalizadorCompanhia(eq("ABC123"), any(CompanhiaAerea.class)))
                .thenReturn(reservaDb, reservaDb);

        WoobaAirReservationSyncResult result = service.sincronizar(reservaWooba);

        assertEquals(List.of("1234567890"), result.getBilhetesAtualizados());
        assertEquals(1, result.getPagamentosAtualizados());
        verify(reservaAereoApi, never()).criar(any());
        verify(reservaAereoApi).atualizarStatus(999, 3);
        verify(reservaAereoApi).atualizar(eq(999), any(ReservaAereo.class));
        verify(recebimentoApi).atualizar(eq(88), any(Recebimento.class));
        verify(recebimentoApi, never()).gravar(any());
    }

    @Test
    void deveCancelarReservaQuandoStatusForCancelado() {
        ReservaAereo reservaWooba = reservaWooba("ABC123", 2);
        reservaWooba.setDataCancelamento(new Date());
        ReservaAereo reservaDb = reservaDb("ABC123", 1);

        when(reservaAereoApi.findByLocalizadorCompanhia(eq("ABC123"), any(CompanhiaAerea.class)))
                .thenReturn(reservaDb, reservaDb);

        service.sincronizar(reservaWooba);

        verify(reservaAereoApi).cancelar(eq(999), any(Date.class), any(), eq(33));
        verify(reservaAereoApi, never()).atualizarStatus(eq(999), eq(2));
    }

    @Test
    void devePreservarDataEmissaoAoCancelarBilheteExistente() {
        Date dataEmissaoOriginal = new Date(1760000000000L);
        Date dataCancelamento = new Date(1761000000000L);
        ReservaAereo reservaWooba = reservaWooba("ABC123", 2);
        reservaWooba.setDataEmissao(null);
        reservaWooba.setDataCancelamento(dataCancelamento);
        BilheteAereo bilheteWooba = reservaWooba.getPassageiros().get(0).getBilhetes().get(0);
        bilheteWooba.setStatus(0);
        bilheteWooba.setDataEmissao(null);
        bilheteWooba.setDataCancelamento(dataCancelamento);

        ReservaAereo reservaDb = reservaDb("ABC123", 3);
        reservaDb.setDataEmissao(null);
        BilheteAereo bilheteDb = new BilheteAereo();
        bilheteDb.setCodgBilhete(77);
        bilheteDb.setNumrBilhete("1234567890");
        bilheteDb.setStatus(1);
        bilheteDb.setDataEmissao(dataEmissaoOriginal);
        reservaDb.getPassageiros().get(0).setBilhetes(List.of(bilheteDb));

        when(reservaAereoApi.findByLocalizadorCompanhia(eq("ABC123"), any(CompanhiaAerea.class)))
                .thenReturn(reservaDb, reservaDb);

        service.sincronizar(reservaWooba);

        ArgumentCaptor<ReservaAereo> payloadCaptor = ArgumentCaptor.forClass(ReservaAereo.class);
        verify(reservaAereoApi).atualizar(eq(999), payloadCaptor.capture());
        ReservaAereo payload = payloadCaptor.getValue();
        assertEquals(dataEmissaoOriginal, payload.getDataEmissao());
        assertNotNull(payload.getPassageiros());
        assertEquals(dataEmissaoOriginal, payload.getPassageiros().get(0).getBilhetes().get(0).getDataEmissao());
        assertEquals(dataCancelamento, payload.getPassageiros().get(0).getBilhetes().get(0).getDataCancelamento());
    }

    @Test
    void deveAtualizarPagamentoComoCancelado() {
        ReservaAereo reservaWooba = reservaWooba("ABC123", 2);
        Recebimento recebimentoWooba = recebimento("1234567890", 0);
        recebimentoWooba.setValrCancelado(1265.27);
        reservaWooba.setRecebimentos(List.of(recebimentoWooba));

        ReservaAereo reservaDb = reservaDb("ABC123", 3);
        Recebimento recebimentoDb = recebimento("1234567890", 1);
        recebimentoDb.setCodgRecebimento(88);
        recebimentoDb.setValrCancelado(0.0);
        reservaDb.setRecebimentos(List.of(recebimentoDb));

        when(reservaAereoApi.findByLocalizadorCompanhia(eq("ABC123"), any(CompanhiaAerea.class)))
                .thenReturn(reservaDb, reservaDb);

        service.sincronizar(reservaWooba);

        ArgumentCaptor<Recebimento> recebimentoCaptor = ArgumentCaptor.forClass(Recebimento.class);
        verify(recebimentoApi).atualizar(eq(88), recebimentoCaptor.capture());
        Recebimento payload = recebimentoCaptor.getValue();
        assertEquals(0, payload.getStatus());
        assertEquals(1265.27, payload.getValrCancelado(), 0.001);
        assertEquals(999, payload.getCodgReservaAereo().getCodgReservaAereo());
    }

    @Test
    void deveCancelarRecebimentoExistenteQuandoReservaCanceladaVierSemPagamentos() {
        ReservaAereo reservaWooba = reservaWooba("ABC123", 2);
        reservaWooba.setRecebimentos(List.of());
        reservaWooba.getPassageiros().get(0).setBilhetes(List.of());

        ReservaAereo reservaDb = reservaDb("ABC123", 3);
        reservaDb.setRecebimentos(null);
        Recebimento recebimentoDb = recebimento("1234567890", 1);
        recebimentoDb.setCodgRecebimento(88);
        recebimentoDb.setValrRecebimento(854.64);
        recebimentoDb.setValrCancelado(0.0);

        when(reservaAereoApi.findByLocalizadorCompanhia(eq("ABC123"), any(CompanhiaAerea.class)))
                .thenReturn(reservaDb, reservaDb);
        when(recebimentoApi.findByReservaAereo(999)).thenReturn(List.of(recebimentoDb));

        WoobaAirReservationSyncResult result = service.sincronizar(reservaWooba);

        assertEquals(1, result.getPagamentosAtualizados());
        ArgumentCaptor<Recebimento> recebimentoCaptor = ArgumentCaptor.forClass(Recebimento.class);
        verify(recebimentoApi).atualizar(eq(88), recebimentoCaptor.capture());
        Recebimento payload = recebimentoCaptor.getValue();
        assertEquals(0, payload.getStatus());
        assertEquals(854.64, payload.getValrCancelado(), 0.001);
        assertEquals(999, payload.getCodgReservaAereo().getCodgReservaAereo());
    }

    @Test
    void deveIgnorarQuandoAgenciaNaoExiste() {
        ReservaAereo reserva = reservaWooba("ABC123", 1);
        reserva.getCodgAgencia().setCodgAgencia(null);

        WoobaAirReservationSyncResult result = service.sincronizar(reserva);

        assertEquals("IGNORED", result.getAction());
        verify(reservaAereoApi, never()).criar(any());
        verify(reservaAereoApi, never()).atualizar(any(), any());
    }

    @Test
    void deveIgnorarSemGravarQuandoContextCustomerVierPreenchido() {
        ReservaAereo reserva = reservaWooba("AVP5AG", 1);
        reserva.setRegraReserva("WoobaUniqueId=AIR-CUSTOMER; WoobaCustomer=true; WoobaCustomerId=13659; WoobaCustomerName=TREVO OPERACIONAL");

        WoobaAirReservationSyncResult result = service.sincronizar(reserva);

        assertEquals("IGNORED", result.getAction());
        assertTrue(result.getReason().contains("Context.Customer informado"));
        verify(reservaAereoApi, never()).findByLocalizadorCompanhia(any(), any(CompanhiaAerea.class));
        verify(reservaAereoApi, never()).criar(any());
        verify(reservaAereoApi, never()).atualizar(any(), any());
        verify(recebimentoApi, never()).gravar(any());
        verify(notificacaoApi, never()).criarParaUsuario(any());
    }

    private ReservaAereo reservaWooba(String localizador, Integer status) {
        ReservaAereo reserva = new ReservaAereo();
        reserva.setLocalizador(localizador);
        reserva.setStatus(status);
        reserva.setDataCriacao(new Date());
        reserva.setDataEmissao(new Date());

        CompanhiaAerea cia = new CompanhiaAerea();
        cia.setCodgCompanhiaAerea(11);
        cia.setIataCia("G3");
        reserva.setCodgCompanhiaAerea(cia);

        Agencia agencia = new Agencia();
        agencia.setCodgAgencia(22);
        agencia.setCodgSistemaBackOffice("026907");
        reserva.setCodgAgencia(agencia);

        Usuario usuario = new Usuario();
        usuario.setCodgUsuario(33);
        usuario.setLoginUsuario("joelson");
        reserva.setCodgUsuarioCriacao(usuario);

        Passageiro passageiro = passageiro("1.1", 0);
        BilheteAereo bilhete = new BilheteAereo();
        bilhete.setNumrBilhete("1234567890");
        bilhete.setStatus(1);
        bilhete.setDataEmissao(new Date());
        passageiro.setBilhetes(List.of(bilhete));
        reserva.setPassageiros(List.of(passageiro));

        reserva.setRecebimentos(List.of(recebimento("1234567890", 1)));
        return reserva;
    }

    private ReservaAereo reservaDb(String localizador, Integer status) {
        ReservaAereo reserva = reservaWooba(localizador, status);
        reserva.setCodgReservaAereo(999);
        reserva.setRecebimentos(null);

        Passageiro passageiro = passageiro("1.1", 555);
        passageiro.setBilhetes(List.of());
        reserva.setPassageiros(List.of(passageiro));
        return reserva;
    }

    private Passageiro passageiro(String idPassageiroCia, int codgPassageiro) {
        Passageiro passageiro = new Passageiro();
        passageiro.setCodgPassageiro(codgPassageiro);
        passageiro.setIdPassageiroCia(idPassageiroCia);
        passageiro.setNomePassageiro("JOAO");
        passageiro.setSobrenomePassageiro("SILVA");
        passageiro.setTipoPassageiro(1);
        return passageiro;
    }

    private Recebimento recebimento(String ticket, Integer status) {
        FormaPagamento formaPagamento = new FormaPagamento();
        formaPagamento.setCodgFormaPagto(7);
        formaPagamento.setNomeFormaPagto("FATURADO");

        Recebimento recebimento = new Recebimento();
        recebimento.setCodgFormaPagto(formaPagamento);
        recebimento.setValrRecebimento(1265.27);
        recebimento.setStatus(status);
        recebimento.setDataRecebimento(new Date());
        recebimento.setQtdeParcela(1);
        recebimento.setMensagem("WoobaPaymentType=Customer; WoobaPaymentTicket=" + ticket);
        return recebimento;
    }
}
