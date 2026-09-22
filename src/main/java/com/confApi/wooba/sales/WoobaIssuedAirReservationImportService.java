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

import java.util.ArrayList;
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
            reconciliarDivisao(details, expected);

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
        if (details == null || details.getTransaction() == null) {
            throw new IllegalStateException("Wooba nao retornou details da transacao vinculada.");
        }
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
        ReservaAereo original = buscarVinculoAnterior(expected, links);
        if (original != null) {
            throw new IllegalStateException("Bilhete ainda vinculado a " + original.getLocalizador()
                    + ". Divisao nao reconciliada para " + expected.getLocalizador());
        }
    }

    private ReservaAereo buscarVinculoAnterior(ReservaAereo expected, JsonNode links) {
        Set<String> tickets = numerosBilhetes(expected);
        for (JsonNode link : links) {
            if (link.path("TransactionType").asInt() == 100 && !link.path("Ticket").asText("").isBlank()) {
                tickets.add(link.path("Ticket").asText().trim());
            }
        }
        Set<String> checked = new HashSet<>();
        List<ReservaAereo> originals = new ArrayList<>();
        list(expected.getTrechos()).forEach(trecho -> list(trecho.getVoos()).forEach(voo -> {
            String locator = normalize(voo.getLocalizadorCia());
            if (!tickets.isEmpty() && !locator.isBlank() && !locator.equals(normalize(expected.getLocalizador()))
                    && checked.add(locator)) {
                ReservaAereo original = reservas.findByLocalizadorCompanhiaParaSincronizacao(locator, expected.getCodgCompanhiaAerea());
                if (original != null) {
                    Set<String> duplicates = numerosBilhetes(original);
                    duplicates.retainAll(tickets);
                    if (!duplicates.isEmpty()) {
                        originals.add(original);
                    }
                }
            }
        }));
        if (originals.size() > 1) {
            throw new IllegalStateException("Bilhetes da divisao vinculados a mais de uma reserva original.");
        }
        return originals.isEmpty() ? null : originals.get(0);
    }

    private void reconciliarDivisao(WoobaSalesDetailsResponse details, ReservaAereo expected) {
        ReservaAereo original = buscarVinculoAnterior(expected, details.getTransaction().path("Links"));
        if (original == null) return;
        WoobaSalesDetailsResponse air = details;
        if (details.getTransaction().path("Header").path("TransactionType").asInt() == 100) {
            air = null;
            for (JsonNode link : details.getTransaction().path("Links")) {
                if (link.path("TransactionType").asInt() == 1) {
                    if (air != null) throw new IllegalStateException("Bilhete com mais de um vinculo AIR.");
                    air = client.details(requiredUniqueId(link));
                }
            }
        }
        if (!elegivel(air, 1)) throw new IllegalStateException("Divisao sem AIR emitida elegivel.");
        ReservaAereo destination = mapper.toReservaAereo(air, null);
        validarMesmaReserva(expected, destination);
        destination.setRecebimentos(new ArrayList<>());
        for (Passageiro passenger : list(destination.getPassageiros())) passenger.setBilhetes(new ArrayList<>());
        Set<String> loaded = new HashSet<>();
        for (JsonNode link : air.getTransaction().path("Links")) {
            if (!WoobaSalesListTransactions.matches(link, 100, 4) || !loaded.add(requiredUniqueId(link))) continue;
            WoobaSalesDetailsResponse ticket = client.details(requiredUniqueId(link));
            if (!elegivel(ticket, 100)) throw new IllegalStateException("Bilhete da divisao nao esta emitido/elegivel.");
            ReservaAereo ticketReservation = mapper.toReservaAereo(ticket, null);
            validarMesmaReserva(destination, ticketReservation);
            validarAgenciaWooba(air, ticket);
            for (Passageiro ticketPassenger : list(ticketReservation.getPassageiros())) {
                Passageiro passenger = encontrarPassageiro(destination, ticketPassenger);
                if (passenger == null) throw new IllegalStateException("Passageiro do TKT nao pertence ao AIR da divisao.");
                passenger.getBilhetes().addAll(list(ticketPassenger.getBilhetes()));
            }
            destination.getRecebimentos().addAll(list(ticketReservation.getRecebimentos()));
        }
        if (list(destination.getPassageiros()).isEmpty() || destination.getPassageiros().stream()
                .anyMatch(p -> list(p.getBilhetes()).isEmpty())) {
            throw new IllegalStateException("Details da divisao sem bilhetes de todos os passageiros.");
        }
        // A referencia do voo e apenas uma pista. A original atual deve confirmar a saida do passageiro.
        String originalId = regra(original.getRegraReserva(), "WoobaUniqueId");
        if (!originalId.startsWith("AIR-")) originalId = regra(original.getRegraReserva(), "WoobaAirUniqueId");
        if (!originalId.startsWith("AIR-")) throw new IllegalStateException("Original sem referencia AIR para conferir divisao.");
        WoobaSalesDetailsResponse originalDetails = client.details(originalId);
        if (originalDetails == null || originalDetails.getTransaction() == null
                || originalDetails.getTransaction().path("Header").path("TransactionType").asInt() != 1
                || originalDetails.getTransaction().path("Context").hasNonNull("Customer")) {
            throw new IllegalStateException("Details da original indisponivel ou nao elegivel para divisao.");
        }
        ReservaAereo originalNow = mapper.toReservaAereo(originalDetails, null);
        if (list(originalNow.getPassageiros()).isEmpty()) {
            throw new IllegalStateException("AIR original sem passageiros para conferir a divisao.");
        }
        validarMesmaReserva(original, originalNow);
        validarAgenciaWooba(air, originalDetails);
        for (Passageiro passenger : destination.getPassageiros()) {
            if (list(originalNow.getPassageiros()).stream().anyMatch(p ->
                    normalize(p.getNomePassageiro()).equals(normalize(passenger.getNomePassageiro()))
                            && normalize(p.getSobrenomePassageiro()).equals(normalize(passenger.getSobrenomePassageiro())))) {
                throw new IllegalStateException("Passageiro ainda consta no AIR original. Aguardar confirmacao da divisao.");
            }
        }
        Set<String> movedTickets = numerosBilhetes(destination);
        Set<String> originalTickets = numerosBilhetes(originalNow);
        for (JsonNode link : originalDetails.getTransaction().path("Links")) {
            if (link.path("TransactionType").asInt() == 100) originalTickets.add(link.path("Ticket").asText());
        }
        if (originalTickets.stream().anyMatch(movedTickets::contains)) {
            throw new IllegalStateException("Bilhete ainda consta nos vinculos do AIR original.");
        }
        reservas.reconciliarDivisaoWooba(original.getCodgReservaAereo(), resolver.resolverReferenciasManager(destination));
        ReservaAereo saved = buscar(destination);
        ReservaAereo sourceAfter = buscar(original);
        if (saved == null || !completa(saved, destination) || sourceAfter == null
                || numerosBilhetes(sourceAfter).stream().anyMatch(movedTickets::contains)) {
            throw new IllegalStateException("Manager nao confirmou os vinculos apos a divisao.");
        }
    }

    private void validarAgenciaWooba(WoobaSalesDetailsResponse first, WoobaSalesDetailsResponse second) {
        long agency = first.getTransaction().path("Context").path("Agency").path("Id").asLong();
        if (agency <= 0 || agency != second.getTransaction().path("Context").path("Agency").path("Id").asLong()) {
            throw new IllegalStateException("Agencia divergente nos details da divisao.");
        }
    }

    private String regra(String message, String key) {
        for (String part : normalizeMessage(message).split(";")) {
            if (part.trim().startsWith(key + "=")) return part.trim().substring(key.length() + 1).trim();
        }
        return "";
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
