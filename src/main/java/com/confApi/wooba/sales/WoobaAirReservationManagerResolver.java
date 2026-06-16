package com.confApi.wooba.sales;

import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.endPoints.agencia.AgenciaApi;
import com.confApi.endPoints.usuario.UsuarioApi;
import org.springframework.stereotype.Component;

import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class WoobaAirReservationManagerResolver {

    private static final Logger LOG = Logger.getLogger(WoobaAirReservationManagerResolver.class.getName());

    private final AgenciaApi agenciaApi;
    private final UsuarioApi usuarioApi;

    public WoobaAirReservationManagerResolver(AgenciaApi agenciaApi, UsuarioApi usuarioApi) {
        this.agenciaApi = agenciaApi;
        this.usuarioApi = usuarioApi;
    }

    public ReservaAereo resolverReferenciasManager(ReservaAereo reserva) {
        if (reserva == null) {
            return null;
        }

        resolverAgencia(reserva);
        resolverUsuario(reserva);
        return reserva;
    }

    private void resolverAgencia(ReservaAereo reserva) {
        Agencia agenciaWooba = reserva.getCodgAgencia();
        if (agenciaWooba == null || isBlank(agenciaWooba.getCodgSistemaBackOffice())) {
            LOG.log(Level.WARNING, "Reserva Wooba sem BackofficeDetail.Code para localizar agencia. Localizador: {0}", reserva.getLocalizador());
            return;
        }

        Agencia agenciaManager = agenciaApi.findByCodgSistemaBackOffice(agenciaWooba.getCodgSistemaBackOffice());
        if (agenciaManager != null && agenciaManager.getCodgAgencia() != null) {
            reserva.setCodgAgencia(agenciaManager);
            return;
        }

        LOG.log(
                Level.WARNING,
                "Agencia nao localizada no Manager por codg_sistema_backoffice={0}. Localizador: {1}",
                new Object[]{agenciaWooba.getCodgSistemaBackOffice(), reserva.getLocalizador()}
        );
    }

    private void resolverUsuario(ReservaAereo reserva) {
        Usuario usuarioWooba = reserva.getCodgUsuarioCriacao();
        if (usuarioWooba == null || isBlank(usuarioWooba.getLoginUsuario())) {
            LOG.log(Level.WARNING, "Reserva Wooba sem User.Username para localizar usuario. Localizador: {0}", reserva.getLocalizador());
            return;
        }

        Usuario usuarioManager = usuarioApi.consultaUsuarioByLogin(usuarioWooba.getLoginUsuario());
        if (usuarioManager != null && usuarioManager.getCodgUsuario() != null) {
            reserva.setCodgUsuarioCriacao(usuarioManager);
            return;
        }

        LOG.log(
                Level.WARNING,
                "Usuario nao localizado no Manager por login_usuario={0}. Localizador: {1}",
                new Object[]{usuarioWooba.getLoginUsuario(), reserva.getLocalizador()}
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
