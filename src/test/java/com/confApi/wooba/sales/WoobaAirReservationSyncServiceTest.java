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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void deveCriarReservaSincronizarBilhetePagamentoENotificar() {
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
        assertTrue(result.isCreatedNotificationSent());
        assertTrue(result.isIssuedNotificationSent());

        ArgumentCaptor<ReservaAereo> criacaoCaptor = ArgumentCaptor.forClass(ReservaAereo.class);
        verify(reservaAereoApi).criar(criacaoCaptor.capture());
        assertNull(criacaoCaptor.getValue().getRecebimentos());
        assertNull(criacaoCaptor.getValue().getPassageiros().get(0).getBilhetes());

        verify(reservaAereoApi).atualizar(eq(999), any(ReservaAereo.class));
        verify(recebimentoApi).gravar(any(Recebimento.class));
        verify(notificacaoApi, times(2)).criarParaUsuario(any());
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
    void deveIgnorarQuandoAgenciaNaoExiste() {
        ReservaAereo reserva = reservaWooba("ABC123", 1);
        reserva.getCodgAgencia().setCodgAgencia(null);

        WoobaAirReservationSyncResult result = service.sincronizar(reserva);

        assertEquals("IGNORED", result.getAction());
        verify(reservaAereoApi, never()).criar(any());
        verify(reservaAereoApi, never()).atualizar(any(), any());
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
