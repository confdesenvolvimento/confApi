package com.confApi.wooba.sales;

import com.confApi.db.confManager.passageiro.Passageiro;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.endPoints.reservaAereo.ReservaAereoApi;
import com.confApi.wooba.sales.dto.WoobaManualImportRequest;
import com.confApi.wooba.sales.dto.WoobaManualImportResponse;
import com.confApi.wooba.sales.dto.WoobaManualImportUsuarioResponse;
import com.confApi.wooba.sales.dto.WoobaSalesDetailsResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class WoobaManualImportService {
    private final WoobaSalesClient client;
    private final WoobaSalesProperties properties;
    private final WoobaAirReservationMapper mapper;
    private final WoobaAirReservationManagerResolver resolver;
    private final WoobaAirReservationSyncService sync;
    private final WoobaIssuedAirReservationImportService issued;
    private final ReservaAereoApi reservas;

    public WoobaManualImportService(WoobaSalesClient client, WoobaSalesProperties properties,
            WoobaAirReservationMapper mapper, WoobaAirReservationManagerResolver resolver,
            WoobaAirReservationSyncService sync, WoobaIssuedAirReservationImportService issued, ReservaAereoApi reservas) {
        this.client = client;
        this.properties = properties;
        this.mapper = mapper;
        this.resolver = resolver;
        this.sync = sync;
        this.issued = issued;
        this.reservas = reservas;
    }

    public WoobaManualImportResponse importar(WoobaManualImportRequest request) {
        validar(request);
        String locator = normalize(request.localizador());
        ObjectNode query = JsonNodeFactory.instance.objectNode();
        query.put("DateFrom", request.dataCriacao().atStartOfDay().atOffset(offset()).toString());
        query.put("DateTo", request.dataCriacao().plusDays(1).atStartOfDay().minusNanos(1_000_000).atOffset(offset()).toString());
        query.put("FilterDateType", "Create");
        query.put("FilterLinkType", "Any");
        query.put("FilterImportState", "Any");
        query.put("Locator", locator);
        query.putArray("TransactionTypes").add(1);
        JsonNode result = client.list(query);
        if (result == null || (result.has("Success") && !result.path("Success").asBoolean())
                || (result.hasNonNull("Errors") && !result.path("Errors").isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Wooba nao confirmou a consulta de vendas.");
        }
        Map<String, JsonNode> candidates = new LinkedHashMap<>();
        for (JsonNode item : WoobaSalesListTransactions.extract(result)) {
            if (item.path("TransactionType").asInt() == 1 && locator.equals(normalize(item.path("Locator").asText()))) {
                String id = WoobaSalesListTransactions.uniqueId(item);
                if (id.isBlank()) throw conflict("Wooba retornou uma reserva sem UniqueId.");
                candidates.put(id, item);
            }
        }
        if (candidates.size() > 20) throw conflict("Muitas reservas para o localizador e a data. Conferir na Wooba.");
        String selected = null;
        String airline = null;
        for (String id : candidates.keySet()) {
            ReservaAereo mapped = mapear(client.details(id), id, locator);
            String code = mapped.getCodgCompanhiaAerea().getIataCia();
            if (!normalize(request.companhia()).isEmpty() && !WoobaAirlineCodeNormalizer.sameIata(request.companhia(), code)) continue;
            if (selected != null) throw conflict("Mais de uma reserva encontrada. Informe a companhia ou confira os registros na Wooba.");
            selected = id;
            airline = code;
        }
        if (selected == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Reserva nao encontrada para o localizador, data de criacao e companhia informados.");

        // Usa a mesma trava do webhook e dos pollings; os details sao relidos antes de gravar.
        synchronized (sync) {
            WoobaSalesDetailsResponse details = client.details(selected);
            ReservaAereo expected = mapear(details, selected, locator);
            if (!WoobaAirlineCodeNormalizer.sameIata(airline, expected.getCodgCompanhiaAerea().getIataCia())) {
                throw conflict("A companhia mudou durante a consulta. Confira a reserva antes de importar.");
            }
            if (details.getTransaction().path("Context").hasNonNull("Customer")) {
                throw conflict("Reserva com Context.Customer preenchido nao pode ser importada.");
            }
            int state = details.getTransaction().path("Header").path("TransactionState").asInt();
            if (state != 2 && state != 4) throw conflict("Somente reservas Reservadas ou Emitidas podem ser importadas por esta operacao.");
            if (lista(expected.getPassageiros()).isEmpty()) throw conflict("Wooba retornou uma reserva sem passageiros.");
            ReservaAereo before = buscar(expected);
            if (before != null && (Objects.equals(before.getStatus(), 2)
                    || (state == 2 && Objects.equals(before.getStatus(), 3)))) {
                throw conflict("O status existente no Manager impede esta importacao. A reserva nao sera reativada nem voltara de emitida para reservada.");
            }
            resolver.validarUsuarioImportacaoManual(expected, request.usuarioLogin());
            if (state == 4) {
                issued.processarDetailsImportacaoManual(details, request.usuarioLogin());
            } else {
                if (before != null) confirmarPassageiros(before, expected);
                WoobaAirReservationSyncResult imported = sync.sincronizar(resolver.resolverReferenciasManagerImportacaoManual(
                        mapper.toReservaAereo(details, null), request.usuarioLogin()));
                if (!"PROCESSED".equals(imported.getAction())) throw conflict(imported.getReason());
            }
            ReservaAereo saved = buscar(expected);
            if (saved == null || saved.getCodgReservaAereo() == null || saved.getCodgReservaAereo() <= 0
                    || !locator.equals(normalize(saved.getLocalizador())) || saved.getCodgCompanhiaAerea() == null
                    || !WoobaAirlineCodeNormalizer.sameIata(airline, saved.getCodgCompanhiaAerea().getIataCia())
                    || !Objects.equals(saved.getStatus(), expected.getStatus())) {
                throw conflict("Manager nao confirmou a gravacao. Confira a reserva antes de tentar novamente.");
            }
            confirmarPassageiros(saved, expected);
            if (state == 4) confirmarBilhetesVinculados(saved, details);
            int tickets = lista(saved.getPassageiros()).stream().mapToInt(p -> lista(p.getBilhetes()).size()).sum();
            return new WoobaManualImportResponse(true,
                    before == null ? "Reserva importada com sucesso." : "Reserva existente conferida e sincronizada.",
                    before == null ? "CRIADA" : "CONFERIDA", saved.getCodgReservaAereo(), saved.getLocalizador(),
                    saved.getCodgCompanhiaAerea().getIataCia(), saved.getStatus(),
                    Objects.equals(saved.getStatus(), 3) ? "Emitida" : "Reservada", lista(saved.getPassageiros()).size(),
                    tickets, lista(saved.getRecebimentos()).size());
        }
    }

    private ReservaAereo mapear(WoobaSalesDetailsResponse details, String id, String locator) {
        if (details == null || !details.isSuccess() || details.getTransaction() == null) throw conflict("Details invalidos retornados pela Wooba.");
        JsonNode header = details.getTransaction().path("Header");
        if (header.path("TransactionType").asInt() != 1 || !id.equals(WoobaSalesListTransactions.uniqueId(header))
                || !locator.equals(normalize(header.path("Locator").asText()))) {
            throw conflict("Os details nao correspondem a reserva solicitada.");
        }
        ReservaAereo mapped = mapper.toReservaAereo(details, null);
        if (mapped.getCodgCompanhiaAerea() == null || normalize(mapped.getCodgCompanhiaAerea().getIataCia()).isEmpty()) {
            throw conflict("Details sem companhia aerea.");
        }
        return mapped;
    }

    public WoobaManualImportUsuarioResponse buscarUsuario(String login) {
        var usuario = resolver.buscarUsuarioImportacaoManual(login);
        if (usuario == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado no Manager.");
        return new WoobaManualImportUsuarioResponse(usuario.getCodgUsuario(), usuario.getLoginUsuario(), usuario.getNomeCompleto());
    }

    private ReservaAereo buscar(ReservaAereo expected) {
        return reservas.findByLocalizadorCompanhiaParaSincronizacao(expected.getLocalizador(), expected.getCodgCompanhiaAerea());
    }

    private void confirmarPassageiros(ReservaAereo saved, ReservaAereo expected) {
        if (lista(saved.getPassageiros()).size() != lista(expected.getPassageiros()).size()) {
            throw conflict("Passageiros divergentes no Manager. Conferir a reserva ou divisao antes de continuar.");
        }
        for (Passageiro passenger : expected.getPassageiros()) {
            long matches = lista(saved.getPassageiros()).stream().filter(p ->
                    normalize(p.getNomePassageiro()).equals(normalize(passenger.getNomePassageiro()))
                    && normalize(p.getSobrenomePassageiro()).equals(normalize(passenger.getSobrenomePassageiro()))
                    && (normalize(passenger.getIdPassageiroCia()).isEmpty()
                        || normalize(p.getIdPassageiroCia()).equals(normalize(passenger.getIdPassageiroCia())))).count();
            if (matches != 1) throw conflict("Passageiro ausente ou ambiguo no Manager. Conferir a reserva antes de continuar.");
        }
    }

    private void confirmarBilhetesVinculados(ReservaAereo saved, WoobaSalesDetailsResponse details) {
        for (JsonNode link : details.getTransaction().path("Links")) {
            if (!WoobaSalesListTransactions.matches(link, 100, 4)) continue;
            String number = link.path("Ticket").asText("").trim();
            if (!number.isEmpty() && lista(saved.getPassageiros()).stream().flatMap(p -> lista(p.getBilhetes()).stream())
                    .noneMatch(ticket -> number.equals(ticket.getNumrBilhete()) && Objects.equals(ticket.getStatus(), 1))) {
                throw conflict("Bilhete vinculado ainda nao confirmado como emitido no Manager. Confira a reserva antes de tentar novamente.");
            }
        }
    }

    private void validar(WoobaManualImportRequest request) {
        if (request == null || !normalize(request.localizador()).matches("[A-Z0-9][A-Z0-9_-]{2,44}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um localizador valido (3 a 45 caracteres).");
        }
        if (request.dataCriacao() == null || request.dataCriacao().isAfter(LocalDate.now(offset()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma data de criacao valida, nao futura.");
        }
        if (!normalize(request.companhia()).isEmpty() && !normalize(request.companhia()).matches("[A-Z0-9]{2}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o codigo IATA da companhia com 2 caracteres.");
        }
        if (request.usuarioLogin() != null && !request.usuarioLogin().isBlank()) {
            WoobaAirReservationManagerResolver.validarLoginImportacaoManual(request.usuarioLogin());
        }
    }

    private ZoneOffset offset() {
        return ZoneOffset.of(properties.getOffset());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private <T> List<T> lista(List<T> values) { return values == null ? List.of() : values; }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message == null ? "Importacao nao confirmada." : message);
    }
}
