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
import com.confApi.util.TelegramErrorAlert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class WoobaAirReservationManagerResolver {

    private static final Logger LOG = Logger.getLogger(WoobaAirReservationManagerResolver.class.getName());

    private final AgenciaApi agenciaApi;
    private final UsuarioApi usuarioApi;
    private final CompanhiaAereaApi companhiaAereaApi;
    private final AeroportoService aeroportoService;
    private final FormaPagamentoApi formaPagamentoApi;

    @Autowired(required = false)
    private TelegramErrorAlert telegramErrorAlert;

    @Value("${wooba.telegram.enabled:true}")
    private boolean telegramEnabled = true;

    public WoobaAirReservationManagerResolver(AgenciaApi agenciaApi,
                                              UsuarioApi usuarioApi,
                                              CompanhiaAereaApi companhiaAereaApi,
                                              AeroportoService aeroportoService,
                                              FormaPagamentoApi formaPagamentoApi) {
        this.agenciaApi = agenciaApi;
        this.usuarioApi = usuarioApi;
        this.companhiaAereaApi = companhiaAereaApi;
        this.aeroportoService = aeroportoService;
        this.formaPagamentoApi = formaPagamentoApi;
    }

    public ReservaAereo resolverReferenciasManager(ReservaAereo reserva) {
        if (reserva == null) {
            return null;
        }

        resolverAgencia(reserva);
        resolverUsuario(reserva);
        resolverCompanhiasEAeroportos(reserva);
        resolverFormasPagamento(reserva);
        return reserva;
    }

    private void resolverAgencia(ReservaAereo reserva) {
        Agencia agenciaWooba = reserva.getCodgAgencia();
        if (agenciaWooba == null || agenciaWooba.getIdWoobaAgencia() == null) {
            LOG.log(Level.WARNING, "Reserva Wooba sem Agency.Id para localizar agencia. Localizador: {0}", reserva.getLocalizador());
            alertarErro("Reserva Wooba sem Agency.Id para localizar agencia. Localizador: " + reserva.getLocalizador());
            return;
        }

        Agencia agenciaManager = agenciaApi.findByIdWoobaAgencia(agenciaWooba.getIdWoobaAgencia());
        if (agenciaManager != null && agenciaManager.getCodgAgencia() != null) {
            reserva.setCodgAgencia(agenciaManager);
            return;
        }

        LOG.log(
                Level.WARNING,
                "Agencia nao localizada no Manager por id_wooba_agencia={0}. Localizador: {1}",
                new Object[]{agenciaWooba.getIdWoobaAgencia(), reserva.getLocalizador()}
        );
        alertarErro("Agencia nao localizada no Manager por id_wooba_agencia="
                + agenciaWooba.getIdWoobaAgencia() + ". Localizador: " + reserva.getLocalizador());
    }

    private void resolverUsuario(ReservaAereo reserva) {
        Usuario usuarioWooba = reserva.getCodgUsuarioCriacao();
        if (usuarioWooba == null || isBlank(usuarioWooba.getLoginUsuario())) {
            LOG.log(Level.WARNING, "Reserva Wooba sem User.Username para localizar usuario. Localizador: {0}", reserva.getLocalizador());
            alertarErro("Reserva Wooba sem User.Username para localizar usuario. Localizador: " + reserva.getLocalizador());
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
        alertarErro("Usuario nao localizado no Manager por login_usuario="
                + usuarioWooba.getLoginUsuario() + ". Localizador: " + reserva.getLocalizador());
    }

    private void resolverCompanhiasEAeroportos(ReservaAereo reserva) {
        Map<String, CompanhiaAerea> companhias = new HashMap<>();
        Map<String, Aeroporto> aeroportos = new HashMap<>();

        CompanhiaAerea companhiaReserva = resolverCompanhia(reserva.getCodgCompanhiaAerea(), companhias, reserva.getLocalizador());
        reserva.setCodgCompanhiaAerea(companhiaReserva);

        if (reserva.getTrechos() == null) {
            return;
        }

        for (Trecho trecho : reserva.getTrechos()) {
            if (semIata(trecho.getCodgCompanhiaAerea())) {
                trecho.setCodgCompanhiaAerea(companhiaReserva);
            }
            trecho.setCodgCompanhiaAerea(resolverCompanhia(trecho.getCodgCompanhiaAerea(), companhias, reserva.getLocalizador()));
            trecho.setCodgAeroportoOrigem(resolverAeroporto(trecho.getCodgAeroportoOrigem(), aeroportos, reserva.getLocalizador()));
            trecho.setCodgAeroportoDestino(resolverAeroporto(trecho.getCodgAeroportoDestino(), aeroportos, reserva.getLocalizador()));

            if (trecho.getVoos() == null) {
                continue;
            }

            for (Voo voo : trecho.getVoos()) {
                if (semIata(voo.getCodgCompanhiaAerea())) {
                    voo.setCodgCompanhiaAerea(trecho.getCodgCompanhiaAerea());
                }
                voo.setCodgCompanhiaAerea(resolverCompanhia(voo.getCodgCompanhiaAerea(), companhias, reserva.getLocalizador()));
                if (semIata(voo.getCodgCompanhiaAereaOperada())) {
                    voo.setCodgCompanhiaAereaOperada(voo.getCodgCompanhiaAerea());
                }
                voo.setCodgCompanhiaAereaOperada(resolverCompanhia(voo.getCodgCompanhiaAereaOperada(), companhias, reserva.getLocalizador()));
                voo.setCodgCompanhiaAereaCodeShare(resolverCompanhia(voo.getCodgCompanhiaAereaCodeShare(), companhias, reserva.getLocalizador()));
                voo.setCodgAeroportoOrigem(resolverAeroporto(voo.getCodgAeroportoOrigem(), aeroportos, reserva.getLocalizador()));
                voo.setCodgAeroportoDestino(resolverAeroporto(voo.getCodgAeroportoDestino(), aeroportos, reserva.getLocalizador()));
            }
        }
    }

    private boolean semIata(CompanhiaAerea companhiaAerea) {
        return companhiaAerea == null || isBlank(companhiaAerea.getIataCia());
    }

    private CompanhiaAerea resolverCompanhia(CompanhiaAerea companhiaWooba,
                                             Map<String, CompanhiaAerea> cache,
                                             String localizador) {
        if (companhiaWooba == null || isBlank(companhiaWooba.getIataCia())) {
            return companhiaWooba;
        }

        List<String> iatas = WoobaAirlineCodeNormalizer.lookupIatas(companhiaWooba.getIataCia());
        for (String iata : iatas) {
            if (!cache.containsKey(iata)) {
                cache.put(iata, companhiaAereaApi.findByIataCia(iata));
            }

            CompanhiaAerea companhiaManager = cache.get(iata);
            if (companhiaManager != null && companhiaManager.getCodgCompanhiaAerea() != null) {
                return companhiaManager;
            }
        }

        LOG.log(
                Level.WARNING,
                "Companhia aerea nao localizada no Manager por iata_cia={0}. Localizador: {1}",
                new Object[]{String.join("/", iatas), localizador}
        );
        alertarErro("Companhia aerea nao localizada no Manager por iata_cia="
                + String.join("/", iatas) + ". Localizador: " + localizador);
        companhiaWooba.setIataCia(WoobaAirlineCodeNormalizer.canonicalIata(companhiaWooba.getIataCia()));
        return companhiaWooba;
    }

    private Aeroporto resolverAeroporto(Aeroporto aeroportoWooba,
                                        Map<String, Aeroporto> cache,
                                        String localizador) {
        if (aeroportoWooba == null || isBlank(aeroportoWooba.getIataAeroporto())) {
            return aeroportoWooba;
        }

        String iata = aeroportoWooba.getIataAeroporto().trim().toUpperCase(Locale.ROOT);
        if (!cache.containsKey(iata)) {
            cache.put(iata, aeroportoService.findAeroportoByIata(iata));
        }

        Aeroporto aeroportoManager = cache.get(iata);
        if (aeroportoManager != null && aeroportoManager.getCodgAeroporto() != null) {
            return aeroportoManager;
        }

        LOG.log(
                Level.WARNING,
                "Aeroporto nao localizado no Manager por iata_aeroporto={0}. Localizador: {1}",
                new Object[]{iata, localizador}
        );
        alertarErro("Aeroporto nao localizado no Manager por iata_aeroporto="
                + iata + ". Localizador: " + localizador);
        return aeroportoWooba;
    }

    private void resolverFormasPagamento(ReservaAereo reserva) {
        if (reserva.getRecebimentos() == null) {
            return;
        }

        Map<String, FormaPagamento> formasPagamento = new HashMap<>();
        for (Recebimento recebimento : reserva.getRecebimentos()) {
            FormaPagamento formaWooba = recebimento.getCodgFormaPagto();
            if (formaWooba == null || isBlank(formaWooba.getNomeFormaPagto())) {
                continue;
            }

            String nomeForma = formaWooba.getNomeFormaPagto().trim();
            String cacheKey = nomeForma.toUpperCase(Locale.ROOT);
            if (!formasPagamento.containsKey(cacheKey)) {
                formasPagamento.put(cacheKey, formaPagamentoApi.findByNomeFormaPagto(nomeForma));
            }

            FormaPagamento formaManager = formasPagamento.get(cacheKey);
            if (formaManager != null && formaManager.getCodgFormaPagto() != null) {
                recebimento.setCodgFormaPagto(formaManager);
                continue;
            }

            LOG.log(
                    Level.WARNING,
                    "Forma de pagamento nao localizada no Manager por nome_forma_pagto={0}. Localizador: {1}",
                    new Object[]{nomeForma, reserva.getLocalizador()}
            );
            alertarErro("Forma de pagamento nao localizada no Manager por nome_forma_pagto="
                    + nomeForma + ". Localizador: " + reserva.getLocalizador());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void alertarErro(String mensagem) {
        if (telegramEnabled && telegramErrorAlert != null) {
            telegramErrorAlert.enviar(this, mensagem);
        }
    }
}
