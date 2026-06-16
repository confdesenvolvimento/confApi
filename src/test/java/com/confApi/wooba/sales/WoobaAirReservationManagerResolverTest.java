package com.confApi.wooba.sales;

import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.endPoints.agencia.AgenciaApi;
import com.confApi.endPoints.usuario.UsuarioApi;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WoobaAirReservationManagerResolverTest {

    private final AgenciaApi agenciaApi = mock(AgenciaApi.class);
    private final UsuarioApi usuarioApi = mock(UsuarioApi.class);
    private final WoobaAirReservationManagerResolver resolver =
            new WoobaAirReservationManagerResolver(agenciaApi, usuarioApi);

    @Test
    void deveResolverAgenciaPorBackofficeEUsuarioPorUsername() {
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

        Agencia agenciaManager = new Agencia();
        agenciaManager.setCodgAgencia(321);
        agenciaManager.setCodgSistemaBackOffice("600745");

        Usuario usuarioManager = new Usuario();
        usuarioManager.setCodgUsuario(654);
        usuarioManager.setLoginUsuario("mateus.cwb");

        when(agenciaApi.findByCodgSistemaBackOffice("600745")).thenReturn(agenciaManager);
        when(usuarioApi.consultaUsuarioByLogin("mateus.cwb")).thenReturn(usuarioManager);

        ReservaAereo resolvida = resolver.resolverReferenciasManager(reserva);

        assertEquals(321, resolvida.getCodgAgencia().getCodgAgencia());
        assertEquals("600745", resolvida.getCodgAgencia().getCodgSistemaBackOffice());
        assertEquals(654, resolvida.getCodgUsuarioCriacao().getCodgUsuario());
        assertEquals("mateus.cwb", resolvida.getCodgUsuarioCriacao().getLoginUsuario());
        verify(agenciaApi).findByCodgSistemaBackOffice("600745");
        verify(usuarioApi).consultaUsuarioByLogin("mateus.cwb");
    }
}
