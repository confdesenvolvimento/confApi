package com.confApi.wooba.sales;

import com.confApi.db.confManager.bilhete.BilheteAereo;
import com.confApi.db.confManager.formaPagamento.FormaPagamento;
import com.confApi.db.confManager.notificacao.NotificacaoApi;
import com.confApi.db.confManager.notificacao.NotificacaoConfigDTO;
import com.confApi.db.confManager.passageiro.Passageiro;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.endPoints.recebimento.RecebimentoApi;
import com.confApi.endPoints.reservaAereo.ReservaAereoApi;
import com.confApi.util.TelegramErrorAlert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class WoobaAirReservationSyncService {

    private static final Logger LOG = Logger.getLogger(WoobaAirReservationSyncService.class.getName());
    private static final Integer STATUS_CANCELADO = 2;
    private static final Integer STATUS_EMITIDO = 3;

    private final ReservaAereoApi reservaAereoApi;
    private final RecebimentoApi recebimentoApi;
    private final NotificacaoApi notificacaoApi;

    @Autowired(required = false)
    private TelegramErrorAlert telegramErrorAlert;

    public WoobaAirReservationSyncService(ReservaAereoApi reservaAereoApi,
                                          RecebimentoApi recebimentoApi,
                                          NotificacaoApi notificacaoApi) {
        this.reservaAereoApi = reservaAereoApi;
        this.recebimentoApi = recebimentoApi;
        this.notificacaoApi = notificacaoApi;
    }

    public WoobaAirReservationSyncResult sincronizar(ReservaAereo reservaWooba) {
        String motivoValidacao = validarReserva(reservaWooba);
        if (motivoValidacao != null) {
            LOG.log(Level.WARNING, "Reserva Wooba nao sincronizada: {0}", motivoValidacao);
            alertarErro("Reserva Wooba nao sincronizada. Localizador: "
                    + safeLocalizador(reservaWooba) + ". Motivo: " + motivoValidacao);
            return WoobaAirReservationSyncResult.ignored(motivoValidacao, reservaWooba);
        }

        Map<String, List<BilheteAereo>> bilhetesWooba = bilhetesPorPassageiro(reservaWooba);
        List<Recebimento> recebimentosWooba = new ArrayList<>(safeList(reservaWooba.getRecebimentos()));

        ReservaAereo reservaDb = reservaAereoApi.findByLocalizadorCompanhia(
                reservaWooba.getLocalizador(),
                reservaWooba.getCodgCompanhiaAerea()
        );

        WoobaAirReservationSyncResult result;
        boolean novaReserva = reservaDb == null || reservaDb.getCodgReservaAereo() == null;

        if (novaReserva) {
            ReservaAereo payloadCriacao = prepararReservaParaCriacao(reservaWooba);
            reservaDb = reservaAereoApi.criar(payloadCriacao);
            reservaDb = recarregarReserva(reservaDb, reservaWooba);
            result = WoobaAirReservationSyncResult.processed(reservaDb);
            result.setCreated(true);
            notificarReservaCriada(reservaDb);
            result.setCreatedNotificationSent(true);
        } else {
            result = WoobaAirReservationSyncResult.processed(reservaDb);
        }

        boolean statusMudouParaEmitido = !STATUS_EMITIDO.equals(valorStatus(reservaDb))
                && STATUS_EMITIDO.equals(valorStatus(reservaWooba));

        if (!novaReserva) {
            atualizarStatusReservaSeNecessario(reservaDb, reservaWooba, result);
        }

        sincronizarBilhetes(reservaDb, reservaWooba, bilhetesWooba, result);
        sincronizarRecebimentos(
                reservaDb,
                recebimentosWooba,
                STATUS_CANCELADO.equals(valorStatus(reservaWooba)),
                result
        );

        List<String> bilhetesEmitidosAlterados = bilhetesEmitidosAlterados(result, bilhetesWooba);
        if (STATUS_EMITIDO.equals(valorStatus(reservaWooba))
                && (statusMudouParaEmitido || !bilhetesEmitidosAlterados.isEmpty())) {
            notificarReservaEmitida(reservaDb, bilhetesEmitidosAlterados);
            result.setIssuedNotificationSent(true);
        }

        result.setUpdated(result.isUpdated()
                || !result.getBilhetesGravados().isEmpty()
                || !result.getBilhetesAtualizados().isEmpty()
                || result.getPagamentosGravados() > 0
                || result.getPagamentosAtualizados() > 0);
        result.setReserva(recarregarReserva(reservaDb, reservaWooba));

        return result;
    }

    private String validarReserva(ReservaAereo reserva) {
        if (reserva == null) {
            return "Reserva aerea nao informada.";
        }
        if (isBlank(reserva.getLocalizador())) {
            return "Reserva aerea sem localizador.";
        }
        if (reserva.getCodgCompanhiaAerea() == null || reserva.getCodgCompanhiaAerea().getCodgCompanhiaAerea() == null) {
            return "Companhia aerea nao localizada no Manager para aplicar localizador + cia.";
        }
        if (reserva.getCodgAgencia() == null || reserva.getCodgAgencia().getCodgAgencia() == null) {
            return "Agencia nao localizada no Manager.";
        }
        if (reserva.getCodgUsuarioCriacao() == null || reserva.getCodgUsuarioCriacao().getCodgUsuario() == null) {
            return "Usuario nao localizado no Manager.";
        }
        return null;
    }

    private ReservaAereo prepararReservaParaCriacao(ReservaAereo reservaWooba) {
        reservaWooba.setCodgReservaAereo(null);
        reservaWooba.setRecebimentos(null);

        for (Passageiro passageiro : safeList(reservaWooba.getPassageiros())) {
            passageiro.setCodgPassageiro(0);
            passageiro.setBilhetes(null);
        }

        return reservaWooba;
    }

    private ReservaAereo recarregarReserva(ReservaAereo reservaDb, ReservaAereo reservaWooba) {
        ReservaAereo recarregada = reservaAereoApi.findByLocalizadorCompanhia(
                reservaWooba.getLocalizador(),
                reservaWooba.getCodgCompanhiaAerea()
        );
        if (recarregada != null && recarregada.getCodgReservaAereo() != null) {
            return recarregada;
        }
        return reservaDb;
    }

    private void atualizarStatusReservaSeNecessario(ReservaAereo reservaDb,
                                                    ReservaAereo reservaWooba,
                                                    WoobaAirReservationSyncResult result) {
        Integer statusDb = valorStatus(reservaDb);
        Integer statusWooba = valorStatus(reservaWooba);
        if (Objects.equals(statusDb, statusWooba)) {
            return;
        }

        if (STATUS_CANCELADO.equals(statusWooba)) {
            reservaAereoApi.cancelar(
                    reservaDb.getCodgReservaAereo(),
                    reservaWooba.getDataCancelamento(),
                    reservaWooba.getDescMotivoCancelamento(),
                    codgUsuario(reservaWooba.getCodgUsuarioCriacao())
            );
        } else if (statusWooba != null) {
            reservaAereoApi.atualizarStatus(reservaDb.getCodgReservaAereo(), statusWooba);
        }

        result.setUpdated(true);
    }

    private void sincronizarBilhetes(ReservaAereo reservaDb,
                                     ReservaAereo reservaWooba,
                                     Map<String, List<BilheteAereo>> bilhetesWooba,
                                     WoobaAirReservationSyncResult result) {
        if (bilhetesWooba.isEmpty() || reservaDb == null || reservaDb.getCodgReservaAereo() == null) {
            return;
        }

        List<Passageiro> passageirosPayload = new ArrayList<>();
        List<String> gravados = new ArrayList<>();
        List<String> atualizados = new ArrayList<>();

        Map<String, Passageiro> passageirosDb = passageirosPorChave(reservaDb.getPassageiros());
        Map<String, BilheteAereo> bilhetesDb = bilhetesPorNumero(reservaDb.getPassageiros());

        for (Map.Entry<String, List<BilheteAereo>> entry : bilhetesWooba.entrySet()) {
            Passageiro passageiroDb = passageirosDb.get(entry.getKey());
            if (passageiroDb == null || passageiroDb.getCodgPassageiro() <= 0) {
                LOG.log(Level.WARNING, "Passageiro nao localizado para sincronizar bilhete. Localizador: {0}, chavePax: {1}",
                        new Object[]{reservaDb.getLocalizador(), entry.getKey()});
                alertarErro("Passageiro nao localizado para sincronizar bilhete. Localizador: "
                        + reservaDb.getLocalizador() + ", chavePax: " + entry.getKey());
                continue;
            }

            List<BilheteAereo> bilhetesPayload = new ArrayList<>();
            for (BilheteAereo bilheteWooba : entry.getValue()) {
                if (bilheteWooba == null || isBlank(bilheteWooba.getNumrBilhete())) {
                    continue;
                }

                String numeroBilhete = bilheteWooba.getNumrBilhete().trim();
                BilheteAereo bilheteDb = bilhetesDb.get(numeroBilhete);
                if (bilheteDb == null) {
                    bilhetesPayload.add(prepararBilheteParaPersistir(bilheteWooba, null, reservaWooba));
                    gravados.add(numeroBilhete);
                } else if (bilheteMudou(bilheteDb, bilheteWooba)) {
                    bilhetesPayload.add(prepararBilheteParaPersistir(bilheteWooba, bilheteDb, reservaWooba));
                    atualizados.add(numeroBilhete);
                }
            }

            if (!bilhetesPayload.isEmpty()) {
                Passageiro passageiroPayload = new Passageiro();
                passageiroPayload.setCodgPassageiro(passageiroDb.getCodgPassageiro());
                passageiroPayload.setBilhetes(bilhetesPayload);
                passageirosPayload.add(passageiroPayload);
            }
        }

        if (passageirosPayload.isEmpty() && Objects.equals(valorStatus(reservaDb), valorStatus(reservaWooba))) {
            return;
        }

        ReservaAereo payload = new ReservaAereo();
        payload.setCodgReservaAereo(reservaDb.getCodgReservaAereo());
        payload.setStatus(reservaWooba.getStatus());
        payload.setDataEmissao(firstDate(
                reservaWooba.getDataEmissao(),
                primeiraDataEmissao(bilhetesPayload(passageirosPayload)),
                reservaDb.getDataEmissao()
        ));
        payload.setCodgUsuarioEmissao(codgUsuario(reservaWooba.getCodgUsuarioCriacao()));
        payload.setPassageiros(passageirosPayload);

        reservaAereoApi.atualizar(reservaDb.getCodgReservaAereo(), payload);
        result.setBilhetesGravados(gravados);
        result.setBilhetesAtualizados(atualizados);
    }

    private BilheteAereo prepararBilheteParaPersistir(BilheteAereo bilheteWooba,
                                                      BilheteAereo bilheteDb,
                                                      ReservaAereo reservaWooba) {
        BilheteAereo bilhete = new BilheteAereo();
        bilhete.setCodgBilhete(bilheteDb == null ? null : bilheteDb.getCodgBilhete());
        bilhete.setNumrBilhete(bilheteWooba.getNumrBilhete());
        bilhete.setStatus(bilheteWooba.getStatus());
        bilhete.setDataEmissao(firstDate(
                bilheteWooba.getDataEmissao(),
                bilheteDb == null ? null : bilheteDb.getDataEmissao(),
                reservaWooba.getDataEmissao(),
                reservaWooba.getDataCriacao(),
                new Date()
        ));
        bilhete.setDataCancelamento(bilheteWooba.getDataCancelamento());
        return bilhete;
    }

    private List<BilheteAereo> bilhetesPayload(List<Passageiro> passageirosPayload) {
        List<BilheteAereo> bilhetes = new ArrayList<>();
        for (Passageiro passageiro : safeList(passageirosPayload)) {
            bilhetes.addAll(safeList(passageiro.getBilhetes()));
        }
        return bilhetes;
    }

    private Date primeiraDataEmissao(List<BilheteAereo> bilhetes) {
        for (BilheteAereo bilhete : safeList(bilhetes)) {
            if (bilhete != null && bilhete.getDataEmissao() != null) {
                return bilhete.getDataEmissao();
            }
        }
        return null;
    }

    private boolean bilheteMudou(BilheteAereo bilheteDb, BilheteAereo bilheteWooba) {
        return !Objects.equals(bilheteDb.getStatus(), bilheteWooba.getStatus())
                || !sameTime(bilheteDb.getDataEmissao(), bilheteWooba.getDataEmissao())
                || !sameTime(bilheteDb.getDataCancelamento(), bilheteWooba.getDataCancelamento());
    }

    private void sincronizarRecebimentos(ReservaAereo reservaDb,
                                         List<Recebimento> recebimentosWooba,
                                         boolean reservaCancelada,
                                         WoobaAirReservationSyncResult result) {
        if (reservaDb == null || reservaDb.getCodgReservaAereo() == null) {
            return;
        }

        List<Recebimento> recebimentosDb = carregarRecebimentosDb(reservaDb);
        if (recebimentosWooba == null || recebimentosWooba.isEmpty()) {
            if (reservaCancelada) {
                cancelarRecebimentosExistentes(reservaDb, recebimentosDb, result);
            }
            return;
        }

        int gravados = 0;
        int atualizados = 0;
        for (Recebimento recebimentoWooba : recebimentosWooba) {
            if (!recebimentoValidoParaPersistir(recebimentoWooba)) {
                LOG.log(Level.WARNING, "Recebimento Wooba ignorado por falta de forma de pagamento resolvida. Localizador: {0}",
                        reservaDb.getLocalizador());
                alertarErro("Recebimento Wooba ignorado por falta de forma de pagamento resolvida. Localizador: "
                        + reservaDb.getLocalizador());
                continue;
            }

            recebimentoWooba.setCodgReservaAereo(new ReservaAereo(reservaDb.getCodgReservaAereo()));
            Recebimento recebimentoDb = encontrarRecebimento(recebimentosDb, recebimentoWooba);
            if (recebimentoDb == null) {
                recebimentoApi.gravar(recebimentoWooba);
                recebimentosDb.add(recebimentoWooba);
                gravados++;
            } else if (recebimentoMudou(recebimentoDb, recebimentoWooba)) {
                Recebimento payload = prepararRecebimentoParaAtualizar(recebimentoDb, recebimentoWooba, reservaDb);
                recebimentoApi.atualizar(recebimentoDb.getCodgRecebimento(), payload);
                atualizados++;
            }
        }

        result.setPagamentosGravados(gravados);
        result.setPagamentosAtualizados(atualizados);
    }

    private List<Recebimento> carregarRecebimentosDb(ReservaAereo reservaDb) {
        List<Recebimento> recebimentosDb = new ArrayList<>(safeList(reservaDb.getRecebimentos()));
        if (recebimentosDb.isEmpty()) {
            recebimentosDb.addAll(safeList(recebimentoApi.findByReservaAereo(reservaDb.getCodgReservaAereo())));
        }
        return recebimentosDb;
    }

    private void cancelarRecebimentosExistentes(ReservaAereo reservaDb,
                                                List<Recebimento> recebimentosDb,
                                                WoobaAirReservationSyncResult result) {
        int atualizados = 0;
        for (Recebimento recebimentoDb : safeList(recebimentosDb)) {
            if (recebimentoDb == null || recebimentoDb.getCodgRecebimento() == null) {
                continue;
            }

            Double valorCancelado = valorCanceladoDoRecebimento(recebimentoDb);
            if (Integer.valueOf(0).equals(recebimentoDb.getStatus())
                    && sameDouble(recebimentoDb.getValrCancelado(), valorCancelado)) {
                continue;
            }

            Recebimento payload = prepararRecebimentoParaCancelamento(recebimentoDb, reservaDb, valorCancelado);
            recebimentoApi.atualizar(recebimentoDb.getCodgRecebimento(), payload);
            atualizados++;
        }

        result.setPagamentosAtualizados(result.getPagamentosAtualizados() + atualizados);
    }

    private Recebimento prepararRecebimentoParaCancelamento(Recebimento recebimentoDb,
                                                            ReservaAereo reservaDb,
                                                            Double valorCancelado) {
        recebimentoDb.setCodgReservaAereo(new ReservaAereo(reservaDb.getCodgReservaAereo()));
        recebimentoDb.setStatus(0);
        recebimentoDb.setValrCancelado(valorCancelado);
        return recebimentoDb;
    }

    private Double valorCanceladoDoRecebimento(Recebimento recebimento) {
        return firstDouble(
                valorMaiorQueZero(recebimento.getValrRecebimento()),
                valorMaiorQueZero(recebimento.getValrCancelado()),
                0.0
        );
    }

    private boolean recebimentoValidoParaPersistir(Recebimento recebimento) {
        return recebimento != null
                && recebimento.getValrRecebimento() != null
                && recebimento.getCodgFormaPagto() != null
                && recebimento.getCodgFormaPagto().getCodgFormaPagto() != null;
    }

    private Recebimento prepararRecebimentoParaAtualizar(Recebimento recebimentoDb,
                                                         Recebimento recebimentoWooba,
                                                         ReservaAereo reservaDb) {
        recebimentoWooba.setCodgRecebimento(recebimentoDb.getCodgRecebimento());
        recebimentoWooba.setCodgReservaAereo(new ReservaAereo(reservaDb.getCodgReservaAereo()));
        if (recebimentoWooba.getLink() == null) {
            recebimentoWooba.setLink(recebimentoDb.getLink());
        }
        if (recebimentoWooba.getQrcodePix() == null) {
            recebimentoWooba.setQrcodePix(recebimentoDb.getQrcodePix());
        }
        if (recebimentoWooba.getCopiacolaPix() == null) {
            recebimentoWooba.setCopiacolaPix(recebimentoDb.getCopiacolaPix());
        }
        if (recebimentoWooba.getCodgTransacao() == null) {
            recebimentoWooba.setCodgTransacao(recebimentoDb.getCodgTransacao());
        }
        if (recebimentoWooba.getOrderGatewayCartao() == null) {
            recebimentoWooba.setOrderGatewayCartao(recebimentoDb.getOrderGatewayCartao());
        }
        return recebimentoWooba;
    }

    private Recebimento encontrarRecebimento(List<Recebimento> recebimentosDb, Recebimento recebimentoWooba) {
        for (Recebimento recebimentoDb : recebimentosDb) {
            if (mesmoRecebimento(recebimentoDb, recebimentoWooba)) {
                return recebimentoDb;
            }
        }
        return null;
    }

    private boolean mesmoRecebimento(Recebimento recebimentoDb, Recebimento recebimentoWooba) {
        if (!isBlank(recebimentoDb.getCodgTransacao()) && !isBlank(recebimentoWooba.getCodgTransacao())) {
            return recebimentoDb.getCodgTransacao().equalsIgnoreCase(recebimentoWooba.getCodgTransacao());
        }
        if (!isBlank(recebimentoDb.getOrderGatewayCartao()) && !isBlank(recebimentoWooba.getOrderGatewayCartao())) {
            return recebimentoDb.getOrderGatewayCartao().equalsIgnoreCase(recebimentoWooba.getOrderGatewayCartao());
        }

        String uniqueIdDb = regraValue(recebimentoDb.getMensagem(), "WoobaPaymentUniqueId");
        String uniqueIdWooba = regraValue(recebimentoWooba.getMensagem(), "WoobaPaymentUniqueId");
        if (!isBlank(uniqueIdDb) && !isBlank(uniqueIdWooba)) {
            return uniqueIdDb.equalsIgnoreCase(uniqueIdWooba);
        }

        String ticketDb = regraValue(recebimentoDb.getMensagem(), "WoobaPaymentTicket");
        String ticketWooba = regraValue(recebimentoWooba.getMensagem(), "WoobaPaymentTicket");
        if (!isBlank(ticketDb) && !isBlank(ticketWooba)) {
            return ticketDb.equalsIgnoreCase(ticketWooba);
        }

        return Objects.equals(formaPagamentoId(recebimentoDb.getCodgFormaPagto()), formaPagamentoId(recebimentoWooba.getCodgFormaPagto()))
                && sameDouble(recebimentoDb.getValrRecebimento(), recebimentoWooba.getValrRecebimento());
    }

    private boolean recebimentoMudou(Recebimento recebimentoDb, Recebimento recebimentoWooba) {
        return !sameDouble(recebimentoDb.getValrRecebimento(), recebimentoWooba.getValrRecebimento())
                || !Objects.equals(recebimentoDb.getStatus(), recebimentoWooba.getStatus())
                || !sameDouble(recebimentoDb.getValrCancelado(), recebimentoWooba.getValrCancelado())
                || !Objects.equals(formaPagamentoId(recebimentoDb.getCodgFormaPagto()), formaPagamentoId(recebimentoWooba.getCodgFormaPagto()))
                || !sameTime(recebimentoDb.getDataRecebimento(), recebimentoWooba.getDataRecebimento())
                || !sameString(recebimentoDb.getMensagem(), recebimentoWooba.getMensagem());
    }

    private void notificarReservaCriada(ReservaAereo reserva) {
        NotificacaoConfigDTO notificacao = notificacaoBase(reserva);
        notificacao.setTituloNotificacao("Reserva aerea criada");
        notificacao.setSubTituloNotificacao("Localizador " + reserva.getLocalizador());
        notificacao.setDescricaoNotificacao("A reserva aerea " + reserva.getLocalizador() + " criada com sucesso no portal do agente.");
        notificacaoApi.criarParaUsuario(notificacao);
    }

    private void notificarReservaEmitida(ReservaAereo reserva, List<String> bilhetes) {
        NotificacaoConfigDTO notificacao = notificacaoBase(reserva);
        String bilhetesTexto = bilhetes == null || bilhetes.isEmpty() ? "sem bilhetes novos informados" : String.join(", ", bilhetes);
        notificacao.setTituloNotificacao("Reserva aerea emitida");
        notificacao.setSubTituloNotificacao("Localizador " + reserva.getLocalizador());
        notificacao.setDescricaoNotificacao("A reserva aerea " + reserva.getLocalizador()
                + " foi emitida. Bilhetes: " + bilhetesTexto + ".");
        notificacaoApi.criarParaUsuario(notificacao);
    }

    private NotificacaoConfigDTO notificacaoBase(ReservaAereo reserva) {
        NotificacaoConfigDTO notificacao = new NotificacaoConfigDTO();
        notificacao.setCodgUsuario(codgUsuario(reserva.getCodgUsuarioCriacao()));
        if (reserva.getCodgAgencia() != null) {
            notificacao.setCodgAgencia(reserva.getCodgAgencia().getCodgAgencia());
            if (reserva.getCodgAgencia().getCodgUnidade() != null) {
                notificacao.setCodgUnidade(reserva.getCodgAgencia().getCodgUnidade().getCodgUnidade());
            }
        }
        return notificacao;
    }

    private Map<String, List<BilheteAereo>> bilhetesPorPassageiro(ReservaAereo reserva) {
        Map<String, List<BilheteAereo>> result = new HashMap<>();
        for (Passageiro passageiro : safeList(reserva.getPassageiros())) {
            List<BilheteAereo> bilhetes = safeList(passageiro.getBilhetes()).stream()
                    .filter(bilhete -> bilhete != null && !isBlank(bilhete.getNumrBilhete()))
                    .collect(Collectors.toList());
            if (!bilhetes.isEmpty()) {
                result.put(chavePassageiro(passageiro), bilhetes);
            }
        }
        return result;
    }

    private Map<String, Passageiro> passageirosPorChave(List<Passageiro> passageiros) {
        Map<String, Passageiro> result = new HashMap<>();
        for (Passageiro passageiro : safeList(passageiros)) {
            result.put(chavePassageiro(passageiro), passageiro);
        }
        return result;
    }

    private Map<String, BilheteAereo> bilhetesPorNumero(List<Passageiro> passageiros) {
        Map<String, BilheteAereo> result = new HashMap<>();
        for (Passageiro passageiro : safeList(passageiros)) {
            for (BilheteAereo bilhete : safeList(passageiro.getBilhetes())) {
                if (bilhete != null && !isBlank(bilhete.getNumrBilhete())) {
                    result.put(bilhete.getNumrBilhete().trim(), bilhete);
                }
            }
        }
        return result;
    }

    private List<String> bilhetesEmitidosAlterados(WoobaAirReservationSyncResult result,
                                                   Map<String, List<BilheteAereo>> bilhetesWooba) {
        Set<String> alterados = new HashSet<>();
        alterados.addAll(result.getBilhetesGravados());
        alterados.addAll(result.getBilhetesAtualizados());

        if (alterados.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> emitidos = new ArrayList<>();
        for (List<BilheteAereo> bilhetes : bilhetesWooba.values()) {
            for (BilheteAereo bilhete : bilhetes) {
                if (bilhete != null
                        && Integer.valueOf(1).equals(bilhete.getStatus())
                        && alterados.contains(bilhete.getNumrBilhete())) {
                    emitidos.add(bilhete.getNumrBilhete());
                }
            }
        }
        return emitidos;
    }

    private String chavePassageiro(Passageiro passageiro) {
        if (passageiro == null) {
            return "";
        }
        if (!isBlank(passageiro.getIdPassageiroCia())) {
            return "ID:" + passageiro.getIdPassageiroCia().trim().toUpperCase(Locale.ROOT);
        }
        return ("NM:" + defaultString(passageiro.getNomePassageiro()) + "|"
                + defaultString(passageiro.getSobrenomePassageiro()) + "|"
                + passageiro.getTipoPassageiro()).toUpperCase(Locale.ROOT);
    }

    private String regraValue(String regra, String key) {
        if (isBlank(regra) || isBlank(key)) {
            return null;
        }

        String prefix = key + "=";
        for (String part : regra.split(";")) {
            String item = part.trim();
            if (item.startsWith(prefix)) {
                return item.substring(prefix.length()).trim();
            }
        }
        return null;
    }

    private Integer valorStatus(ReservaAereo reserva) {
        return reserva == null ? null : reserva.getStatus();
    }

    private Integer codgUsuario(Usuario usuario) {
        return usuario == null ? null : usuario.getCodgUsuario();
    }

    private Integer formaPagamentoId(FormaPagamento formaPagamento) {
        return formaPagamento == null ? null : formaPagamento.getCodgFormaPagto();
    }

    private Date firstDate(Date... values) {
        if (values == null) {
            return null;
        }
        for (Date value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Double firstDouble(Double... values) {
        if (values == null) {
            return null;
        }
        for (Double value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Double valorMaiorQueZero(Double value) {
        return value != null && value > 0 ? value : null;
    }

    private boolean sameTime(Date left, Date right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.getTime() == right.getTime();
    }

    private boolean sameDouble(Double left, Double right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return Math.abs(left - right) < 0.001;
    }

    private boolean sameString(String left, String right) {
        return Objects.equals(defaultString(left).trim(), defaultString(right).trim());
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safeLocalizador(ReservaAereo reserva) {
        return reserva == null ? null : reserva.getLocalizador();
    }

    private void alertarErro(String mensagem) {
        if (telegramErrorAlert != null) {
            telegramErrorAlert.enviar(this, mensagem);
        }
    }
}
