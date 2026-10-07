package com.confApi.wooba.sales;

import com.confApi.db.confManager.aeroporto.AeroportoService;
import com.confApi.db.confManager.companhiaAerea.CompanhiaAereaApi;
import com.confApi.db.confManager.formaPagamento.FormaPagamentoApi;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.endPoints.agencia.AgenciaApi;
import com.confApi.endPoints.usuario.UsuarioApi;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WoobaManualImportUsuarioResolverTest {
    private final UsuarioApi users = mock(UsuarioApi.class);
    private final WoobaAirReservationManagerResolver resolver = new WoobaAirReservationManagerResolver(
            mock(AgenciaApi.class), users, mock(CompanhiaAereaApi.class), mock(AeroportoService.class), mock(FormaPagamentoApi.class));

    @Test void devePreservarUsuarioOriginalQuandoEncontrado() {
        var reserva = reserva();
        when(users.consultaUsuarioByLoginParaImportacao("wooba.original")).thenReturn(usuario(10, "wooba.original"));
        resolver.validarUsuarioImportacaoManual(reserva, "alternativo");
        assertEquals(10, reserva.getCodgUsuarioCriacao().getCodgUsuario());
        verify(users, never()).consultaUsuarioByLoginParaImportacao("alternativo");
    }

    @Test void deveSolicitarSelecaoQuandoUsuarioNaoExistir() {
        assertThrows(WoobaManualImportUsuarioPendenteException.class,
                () -> resolver.validarUsuarioImportacaoManual(reserva(), null));
        when(users.consultaUsuarioByLoginParaImportacao("wooba.original")).thenReturn(new Usuario(0));
        assertThrows(WoobaManualImportUsuarioPendenteException.class,
                () -> resolver.validarUsuarioImportacaoManual(reserva(), null));
    }

    @Test void deveConsultarNovamenteOLoginAlternativoAntesDeUsar() {
        var reserva = reserva();
        when(users.consultaUsuarioByLoginParaImportacao("alternativo")).thenReturn(usuario(20, "alternativo"));
        resolver.validarUsuarioImportacaoManual(reserva, " alternativo ");
        assertEquals(20, reserva.getCodgUsuarioCriacao().getCodgUsuario());
        when(users.consultaUsuarioByLoginParaImportacao("alternativo")).thenReturn(null);
        assertThrows(WoobaManualImportUsuarioPendenteException.class,
                () -> resolver.validarUsuarioImportacaoManual(reserva(), "alternativo"));
    }

    @Test void indisponibilidadeNaoDeveSerInterpretadaComoUsuarioAusente() {
        when(users.consultaUsuarioByLoginParaImportacao("wooba.original")).thenThrow(new IllegalStateException("offline"));
        assertThrows(IllegalStateException.class, () -> resolver.validarUsuarioImportacaoManual(reserva(), "alternativo"));
        verify(users, never()).consultaUsuarioByLoginParaImportacao("alternativo");
    }

    @Test void userNullTambemPermiteSelecaoManual() {
        var reserva = new ReservaAereo();
        when(users.consultaUsuarioByLoginParaImportacao("alternativo")).thenReturn(usuario(20, "alternativo"));
        resolver.validarUsuarioImportacaoManual(reserva, "alternativo");
        assertEquals(20, reserva.getCodgUsuarioCriacao().getCodgUsuario());
    }

    @Test void buscaNaoDeveAceitarLoginVazioOuResultadoDeOutroUsuario() {
        assertThrows(ResponseStatusException.class, () -> resolver.buscarUsuarioImportacaoManual(" "));
        verifyNoInteractions(users);
        when(users.consultaUsuarioByLoginParaImportacao("alternativo")).thenReturn(usuario(30, "outro"));
        assertEquals(502, assertThrows(ResponseStatusException.class,
                () -> resolver.buscarUsuarioImportacaoManual("alternativo")).getRawStatusCode());
    }

    private ReservaAereo reserva() {
        var reserva = new ReservaAereo();
        reserva.setCodgUsuarioCriacao(usuario(null, "wooba.original"));
        return reserva;
    }

    private Usuario usuario(Integer id, String login) {
        var user = new Usuario();
        user.setCodgUsuario(id);
        user.setLoginUsuario(login);
        return user;
    }
}
