package com.confApi.wooba.sales;

import com.confApi.db.confManager.bilhete.BilheteAereo;
import com.confApi.db.confManager.passageiro.Passageiro;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.endPoints.recebimento.RecebimentoApi;
import com.confApi.endPoints.reservaAereo.ReservaAereoApi;
import com.confApi.wooba.sales.dto.WoobaSalesDetailsResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WoobaIssuedAirReservationImportService {
    private final WoobaSalesClient client;
    private final WoobaAirReservationMapper mapper;
    private final WoobaAirReservationManagerResolver resolver;
    private final WoobaAirReservationSyncService sync;
    private final ReservaAereoApi reservas;
    private final RecebimentoApi recebimentos;

    public WoobaIssuedAirReservationImportService(WoobaSalesClient client, WoobaAirReservationMapper mapper,
            WoobaAirReservationManagerResolver resolver, WoobaAirReservationSyncService sync,
            ReservaAereoApi reservas, RecebimentoApi recebimentos) {
        this.client = client;
        this.mapper = mapper;
        this.resolver = resolver;
        this.sync = sync;
        this.reservas = reservas;
        this.recebimentos = recebimentos;
    }

    public void processar(String uniqueId, int type) {
        // Compartilha a exclusao mutua com o webhook e o polling de reservadas nesta instancia.
        synchronized (sync) {
            WoobaSalesDetailsResponse details = client.details(uniqueId);
            if (!elegivel(details, type)) {
                return;
            }
            ReservaAereo expected = mapper.toReservaAereo(details, null);
            if (list(expected.getPassageiros()).isEmpty()) {
                throw new IllegalStateException("Details emitido sem passageiros: " + expected.getLocalizador());
            }
            ReservaAereo existing = buscar(expected);
            if (existing != null && (Integer.valueOf(2).equals(existing.getStatus())
                    || (type == 100 && completa(existing, expected)))) {
                return;
            }
            verificarVinculosAnteriores(expected, details.getTransaction().path("Links"));

            if (type == 100) {
                // O details de um bilhete nao representa a lista completa de passageiros.
                boolean hasParent = false;
                for (JsonNode link : details.getTransaction().path("Links")) {
                    if (link.path("TransactionType").asInt() == 1) {
                        hasParent = true;
                        WoobaSalesDetailsResponse parent = client.details(requiredUniqueId(link));
                        if (!elegivel(parent, 1)) {
                            return;
                        }
                        validarMesmaReserva(expected, mapper.toReservaAereo(parent, null));
                        importar(parent, true);
                        break;
                    }
                }
                if (!hasParent && existing == null) {
                    throw new IllegalStateException("Bilhete sem vinculo AIR para criar a reserva completa.");
                }
            }

            boolean paymentsFromTickets = type == 1 && temBilhetesVinculados(details);
            importar(details, paymentsFromTickets);
            if (type == 1) {
                for (JsonNode link : details.getTransaction().path("Links")) {
                    if (WoobaSalesListTransactions.matches(link, 100, 4)) {
                        WoobaSalesDetailsResponse ticket = client.details(requiredUniqueId(link));
                        if (elegivel(ticket, 100)) {
                            validarMesmaReserva(expected, mapper.toReservaAereo(ticket, null));
                            importar(ticket, false);
                        }
                    }
                }
            }
        }
    }

    private boolean elegivel(WoobaSalesDetailsResponse details, int type) {
        JsonNode transaction = details.getTransaction();
        return WoobaSalesListTransactions.matches(transaction.path("Header"), type, 4)
                && !transaction.path("Context").hasNonNull("Customer");
    }

    private void importar(WoobaSalesDetailsResponse details, boolean paymentsFromTickets) {
        ReservaAereo expected = mapper.toReservaAereo(details, null);
        // O pagamento agregado do AIR nao deve ser somado aos pagamentos individuais dos TKT.
        if (paymentsFromTickets) {
            expected.setRecebimentos(List.of());
        }
        if (list(expected.getPassageiros()).isEmpty()) {
            throw new IllegalStateException("Details emitido sem passageiros: " + expected.getLocalizador());
        }
        ReservaAereo existing = buscar(expected);
        if (existing != null) {
            if (Integer.valueOf(2).equals(existing.getStatus()) || completa(existing, expected)) {
                return;
            }
            for (Passageiro passenger : list(expected.getPassageiros())) {
                if (encontrarPassageiro(existing, passenger) == null) {
                    throw new IllegalStateException("Passageiro ausente ou divergente na reserva existente "
                            + expected.getLocalizador() + ". Conferir divisao antes de complementar.");
                }
            }
        }
        verificarVinculosAnteriores(expected, details.getTransaction().path("Links"));
        WoobaAirReservationSyncResult result = sync.sincronizar(resolver.resolverReferenciasManager(expected));
        if (!"PROCESSED".equals(result.getAction())) {
            throw new IllegalStateException("Emissao pendente: " + result.getReason());
        }
        // O Manager legado pode retornar 2xx mesmo quando a atualizacao dos bilhetes falha.
        ReservaAereo freshExpected = mapper.toReservaAereo(details, null);
        if (paymentsFromTickets) {
            freshExpected.setRecebimentos(List.of());
        }
        ReservaAereo saved = buscar(freshExpected);
        if (saved == null || !completa(saved, freshExpected)) {
            throw new IllegalStateException("Manager ainda nao confirmou reserva, bilhetes e pagamentos: "
                    + freshExpected.getLocalizador());
        }
    }

    private ReservaAereo buscar(ReservaAereo expected) {
        if (expected.getLocalizador() == null || expected.getLocalizador().isBlank()
                || expected.getCodgCompanhiaAerea() == null
                || expected.getCodgCompanhiaAerea().getIataCia() == null) {
            throw new IllegalStateException("Details sem localizador ou companhia.");
        }
        return reservas.findByLocalizadorCompanhiaParaSincronizacao(expected.getLocalizador(), expected.getCodgCompanhiaAerea());
    }

    private boolean completa(ReservaAereo existing, ReservaAereo expected) {
        if (!Integer.valueOf(3).equals(existing.getStatus())) {
            return false;
        }
        for (Passageiro passenger : list(expected.getPassageiros())) {
            Passageiro savedPassenger = encontrarPassageiro(existing, passenger);
            if (savedPassenger == null) {
                return false;
            }
            for (BilheteAereo ticket : list(passenger.getBilhetes())) {
                if (list(savedPassenger.getBilhetes()).stream().noneMatch(saved ->
                        Objects.equals(saved.getNumrBilhete(), ticket.getNumrBilhete())
                                && Objects.equals(saved.getStatus(), ticket.getStatus()))) {
                    return false;
                }
            }
        }
        if (list(expected.getRecebimentos()).isEmpty()) {
            return true;
        }
        List<Recebimento> payments = list(existing.getRecebimentos());
        if (payments.isEmpty()) {
            payments = recebimentos.findByReservaAereoParaSincronizacao(existing.getCodgReservaAereo());
        }
        for (Recebimento payment : expected.getRecebimentos()) {
            if (payments.stream().noneMatch(saved -> mesmoPagamento(saved, payment))) {
                return false;
            }
        }
        return true;
    }

    private Passageiro encontrarPassageiro(ReservaAereo reservation, Passageiro expected) {
        return list(reservation.getPassageiros()).stream().filter(saved -> {
            boolean sameName = normalize(saved.getNomePassageiro()).equals(normalize(expected.getNomePassageiro()))
                    && normalize(saved.getSobrenomePassageiro()).equals(normalize(expected.getSobrenomePassageiro()));
            return sameName && (expected.getIdPassageiroCia() == null || expected.getIdPassageiroCia().isBlank()
                    || Objects.equals(saved.getIdPassageiroCia(), expected.getIdPassageiroCia()));
        }).findFirst().orElse(null);
    }

    private boolean mesmoPagamento(Recebimento saved, Recebimento expected) {
        String reference = paymentReference(expected);
        return !reference.isBlank() && reference.equals(paymentReference(saved))
                && Objects.equals(saved.getStatus(), expected.getStatus())
                && saved.getValrRecebimento() != null && expected.getValrRecebimento() != null
                && Math.abs(saved.getValrRecebimento() - expected.getValrRecebimento()) < 0.005;
    }

    private String paymentReference(Recebimento payment) {
        for (String key : List.of("WoobaPaymentUniqueId", "WoobaPaymentTicket")) {
            for (String part : normalizeMessage(payment.getMensagem()).split(";")) {
                if (part.trim().startsWith(key + "=")) {
                    String value = part.trim().substring(key.length() + 1).trim();
                    if (!value.isBlank()) {
                        return key + "=" + value;
                    }
                }
            }
        }
        return "";
    }

    private void verificarVinculosAnteriores(ReservaAereo expected, JsonNode links) {
        Set<String> tickets = numerosBilhetes(expected);
        for (JsonNode link : links) {
            if (link.path("TransactionType").asInt() == 100 && !link.path("Ticket").asText("").isBlank()) {
                tickets.add(link.path("Ticket").asText().trim());
            }
        }
        Set<String> checked = new HashSet<>();
        list(expected.getTrechos()).forEach(trecho -> list(trecho.getVoos()).forEach(voo -> {
            String locator = normalize(voo.getLocalizadorCia());
            if (!tickets.isEmpty() && !locator.isBlank() && !locator.equals(normalize(expected.getLocalizador()))
                    && checked.add(locator)) {
                ReservaAereo original = reservas.findByLocalizadorCompanhiaParaSincronizacao(locator, expected.getCodgCompanhiaAerea());
                if (original != null) {
                    Set<String> duplicates = numerosBilhetes(original);
                    duplicates.retainAll(tickets);
                    if (!duplicates.isEmpty()) {
                        throw new IllegalStateException("Bilhete(s) " + duplicates + " ainda vinculado(s) a " + locator
                                + ". Conferir transferencia para " + expected.getLocalizador() + " antes de gravar.");
                    }
                }
            }
        }));
    }

    private Set<String> numerosBilhetes(ReservaAereo reservation) {
        return list(reservation.getPassageiros()).stream().flatMap(p -> list(p.getBilhetes()).stream())
                .map(BilheteAereo::getNumrBilhete).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private void validarMesmaReserva(ReservaAereo expected, ReservaAereo related) {
        if (!normalize(expected.getLocalizador()).equals(normalize(related.getLocalizador()))
                || expected.getCodgCompanhiaAerea() == null || related.getCodgCompanhiaAerea() == null
                || !WoobaAirlineCodeNormalizer.sameIata(expected.getCodgCompanhiaAerea().getIataCia(),
                        related.getCodgCompanhiaAerea().getIataCia())) {
            throw new IllegalStateException("Localizador/companhia divergente entre bilhete e reserva vinculada.");
        }
    }

    private String requiredUniqueId(JsonNode link) {
        String id = WoobaSalesListTransactions.uniqueId(link);
        if (id.isBlank()) {
            throw new IllegalStateException("Vinculo Wooba sem UniqueId.");
        }
        return id;
    }

    private boolean temBilhetesVinculados(WoobaSalesDetailsResponse details) {
        for (JsonNode link : details.getTransaction().path("Links")) {
            if (WoobaSalesListTransactions.matches(link, 100, 4)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        return normalizeMessage(text).trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeMessage(String text) {
        return text == null ? "" : text;
    }

    private <T> List<T> list(List<T> items) {
        return items == null ? List.of() : items;
    }
}
