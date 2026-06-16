package com.confApi.wooba.sales;

import com.confApi.db.confManager.aeroporto.Aeroporto;
import com.confApi.db.confManager.aeroporto.AeroportoService;
import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.companhiaAerea.CompanhiaAerea;
import com.confApi.db.confManager.companhiaAerea.CompanhiaAereaApi;
import com.confApi.db.confManager.formaPagamento.FormaPagamento;
import com.confApi.db.confManager.formaPagamento.FormaPagamentoApi;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.db.confManager.trecho.Trecho;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.db.confManager.voo.Voo;
import com.confApi.endPoints.agencia.AgenciaApi;
import com.confApi.endPoints.usuario.UsuarioApi;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WoobaAirReservationManagerResolverTest {

    private final AgenciaApi agenciaApi = mock(AgenciaApi.class);
    private final UsuarioApi usuarioApi = mock(UsuarioApi.class);
    private final CompanhiaAereaApi companhiaAereaApi = mock(CompanhiaAereaApi.class);
    private final AeroportoService aeroportoService = mock(AeroportoService.class);
    private final FormaPagamentoApi formaPagamentoApi = mock(FormaPagamentoApi.class);
    private final WoobaAirReservationManagerResolver resolver =
            new WoobaAirReservationManagerResolver(agenciaApi, usuarioApi, companhiaAereaApi, aeroportoService, formaPagamentoApi);

    @Test
    void deveResolverAgenciaPorIdWoobaEUsuarioPorUsername() {
        ReservaAereo reserva = new ReservaAereo();
        reserva.setLocalizador("GXNHDJ");

        Agencia agenciaWooba = new Agencia();
        agenciaWooba.setCodgSistemaBackOffice("600745");
        agenciaWooba.setIdWoobaAgencia(15055);
        reserva.setCodgAgencia(agenciaWooba);

        Usuario usuarioWooba = new Usuario();
        usuarioWooba.setLoginUsuario("mateus.cwb");
        usuarioWooba.setCodigoWooba(54287);
        reserva.setCodgUsuarioCriacao(usuarioWooba);

        CompanhiaAerea companhiaWooba = new CompanhiaAerea();
        companhiaWooba.setIataCia("LX");
        reserva.setCodgCompanhiaAerea(companhiaWooba);

        Trecho trecho = new Trecho();
        trecho.setCodgCompanhiaAerea(new CompanhiaAerea("LX"));
        trecho.setCodgAeroportoOrigem(new Aeroporto("LDB"));
        trecho.setCodgAeroportoDestino(new Aeroporto("GRU"));

        Voo voo = new Voo();
        voo.setCodgCompanhiaAerea(new CompanhiaAerea("LX"));
        voo.setCodgAeroportoOrigem(new Aeroporto("LDB"));
        voo.setCodgAeroportoDestino(new Aeroporto("GRU"));
        trecho.setVoos(List.of(voo));
        reserva.setTrechos(List.of(trecho));

        FormaPagamento formaWooba = new FormaPagamento();
        formaWooba.setNomeFormaPagto("FATURADO");
        Recebimento recebimento = new Recebimento();
        recebimento.setCodgFormaPagto(formaWooba);
        reserva.setRecebimentos(List.of(recebimento));

        Agencia agenciaManager = new Agencia();
        agenciaManager.setCodgAgencia(321);
        agenciaManager.setCodgSistemaBackOffice("600745");
        agenciaManager.setIdWoobaAgencia(15055);

        Usuario usuarioManager = new Usuario();
        usuarioManager.setCodgUsuario(654);
        usuarioManager.setLoginUsuario("mateus.cwb");

        CompanhiaAerea companhiaManager = new CompanhiaAerea();
        companhiaManager.setCodgCompanhiaAerea(11);
        companhiaManager.setIataCia("LX");

        Aeroporto aeroportoLdb = new Aeroporto("LDB");
        aeroportoLdb.setCodgAeroporto(101);
        Aeroporto aeroportoGru = new Aeroporto("GRU");
        aeroportoGru.setCodgAeroporto(202);

        FormaPagamento formaManager = new FormaPagamento();
        formaManager.setCodgFormaPagto(7);
        formaManager.setNomeFormaPagto("FATURADO");

        when(agenciaApi.findByIdWoobaAgencia(15055)).thenReturn(agenciaManager);
        when(usuarioApi.consultaUsuarioByLogin("mateus.cwb")).thenReturn(usuarioManager);
        when(companhiaAereaApi.findByIataCia("LX")).thenReturn(companhiaManager);
        when(aeroportoService.findAeroportoByIata("LDB")).thenReturn(aeroportoLdb);
        when(aeroportoService.findAeroportoByIata("GRU")).thenReturn(aeroportoGru);
        when(formaPagamentoApi.findByNomeFormaPagto("FATURADO")).thenReturn(formaManager);

        ReservaAereo resolvida = resolver.resolverReferenciasManager(reserva);

        assertEquals(321, resolvida.getCodgAgencia().getCodgAgencia());
        assertEquals("600745", resolvida.getCodgAgencia().getCodgSistemaBackOffice());
        assertEquals(15055, resolvida.getCodgAgencia().getIdWoobaAgencia());
        assertEquals(654, resolvida.getCodgUsuarioCriacao().getCodgUsuario());
        assertEquals("mateus.cwb", resolvida.getCodgUsuarioCriacao().getLoginUsuario());
        assertEquals(11, resolvida.getCodgCompanhiaAerea().getCodgCompanhiaAerea());
        assertEquals(11, resolvida.getTrechos().get(0).getCodgCompanhiaAerea().getCodgCompanhiaAerea());
        assertEquals(101, resolvida.getTrechos().get(0).getCodgAeroportoOrigem().getCodgAeroporto());
        assertEquals(202, resolvida.getTrechos().get(0).getCodgAeroportoDestino().getCodgAeroporto());
        assertEquals(11, resolvida.getTrechos().get(0).getVoos().get(0).getCodgCompanhiaAerea().getCodgCompanhiaAerea());
        assertEquals(11, resolvida.getTrechos().get(0).getVoos().get(0).getCodgCompanhiaAereaOperada().getCodgCompanhiaAerea());
        assertEquals(101, resolvida.getTrechos().get(0).getVoos().get(0).getCodgAeroportoOrigem().getCodgAeroporto());
        assertEquals(202, resolvida.getTrechos().get(0).getVoos().get(0).getCodgAeroportoDestino().getCodgAeroporto());
        assertEquals(7, resolvida.getRecebimentos().get(0).getCodgFormaPagto().getCodgFormaPagto());
        verify(agenciaApi).findByIdWoobaAgencia(15055);
        verify(agenciaApi, never()).findByCodgSistemaBackOffice("600745");
        verify(usuarioApi).consultaUsuarioByLogin("mateus.cwb");
        verify(companhiaAereaApi).findByIataCia("LX");
        verify(aeroportoService).findAeroportoByIata("LDB");
        verify(aeroportoService).findAeroportoByIata("GRU");
        verify(formaPagamentoApi).findByNomeFormaPagto("FATURADO");
    }

    @Test
    void deveResolverCompanhiaLatamJjComoLa() {
        ReservaAereo reserva = new ReservaAereo();
        reserva.setLocalizador("LA9573438SQYY");
        reserva.setCodgCompanhiaAerea(new CompanhiaAerea("JJ"));

        CompanhiaAerea companhiaManager = new CompanhiaAerea();
        companhiaManager.setCodgCompanhiaAerea(44);
        companhiaManager.setIataCia("LA");

        when(companhiaAereaApi.findByIataCia("LA")).thenReturn(companhiaManager);

        ReservaAereo resolvida = resolver.resolverReferenciasManager(reserva);

        assertEquals(44, resolvida.getCodgCompanhiaAerea().getCodgCompanhiaAerea());
        assertEquals("LA", resolvida.getCodgCompanhiaAerea().getIataCia());
        verify(companhiaAereaApi).findByIataCia("LA");
        verify(companhiaAereaApi, never()).findByIataCia("JJ");
    }
}
