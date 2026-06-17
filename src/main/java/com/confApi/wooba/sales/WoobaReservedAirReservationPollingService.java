package com.confApi.wooba.sales;

import com.confApi.util.TelegramErrorAlert;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@ConditionalOnProperty(name = "wooba.sales.polling.reserved.enabled", havingValue = "true")
public class WoobaReservedAirReservationPollingService {

    private static final Logger LOG = Logger.getLogger(WoobaReservedAirReservationPollingService.class.getName());
    private static final int TRANSACTION_TYPE_AIR_RESERVATION = 1;
    private static final int TRANSACTION_STATE_RESERVED = 2;
    private static final ZoneOffset DEFAULT_OFFSET = ZoneOffset.of("-03:00");

    private final WoobaSalesClient woobaSalesClient;
    private final WoobaSalesProperties properties;
    private final WoobaAirReservationService airReservationService;
    private final long lookbackMinutes;
    private final int maxItemsPerCycle;
    private final boolean traceEnabled;
    private final boolean logEnabled;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Map<String, OffsetDateTime> processadosRecentemente = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private TelegramErrorAlert telegramErrorAlert;

    public WoobaReservedAirReservationPollingService(WoobaSalesClient woobaSalesClient,
                                                     WoobaSalesProperties properties,
                                                     WoobaAirReservationService airReservationService,
                                                     @Value("${wooba.sales.polling.reserved.lookback-minutes:5}") long lookbackMinutes,
                                                     @Value("${wooba.sales.polling.reserved.max-items-per-cycle:200}") int maxItemsPerCycle,
                                                     @Value("${wooba.webhook.trace.enabled:false}") boolean traceEnabled,
                                                     @Value("${wooba.webhook.log.enabled:false}") boolean logEnabled) {
        this.woobaSalesClient = woobaSalesClient;
        this.properties = properties;
        this.airReservationService = airReservationService;
        this.lookbackMinutes = Math.max(1, lookbackMinutes);
        this.maxItemsPerCycle = Math.max(1, maxItemsPerCycle);
        this.traceEnabled = traceEnabled;
        this.logEnabled = logEnabled;
    }

    @Scheduled(
            fixedDelayString = "${wooba.sales.polling.reserved.fixed-delay-ms:60000}",
            initialDelayString = "${wooba.sales.polling.reserved.initial-delay-ms:15000}"
    )
    public void executarAgendado() {
        executarAgora();
    }

    public WoobaReservedAirReservationPollingResult executarAgora() {
        if (!running.compareAndSet(false, true)) {
            logInfo("Polling Wooba de reservas aereas reservadas ignorado porque ja existe execucao em andamento.");
            WoobaReservedAirReservationPollingResult result = new WoobaReservedAirReservationPollingResult();
            result.incrementarIgnoradas();
            return result;
        }

        try {
            return executarConsulta();
        } finally {
            running.set(false);
        }
    }

    private WoobaReservedAirReservationPollingResult executarConsulta() {
        OffsetDateTime agora = OffsetDateTime.now(zoneOffset());
        OffsetDateTime dateFrom = agora.minusMinutes(lookbackMinutes);
        ObjectNode request = montarRequest(dateFrom, agora);

        rastrear("Polling Wooba reservadas iniciado. DateFrom=" + request.path("DateFrom").asText()
                + ", DateTo=" + request.path("DateTo").asText()
                + ", LookbackMinutos=" + lookbackMinutes
                + ", MaxItens=" + maxItemsPerCycle);

        WoobaReservedAirReservationPollingResult result = new WoobaReservedAirReservationPollingResult();
        JsonNode response = woobaSalesClient.list(request);
        List<JsonNode> transacoes = extrairTransacoes(response);
        result.setEncontradas(transacoes.size());
        limparCacheAntigo(agora);

        Set<String> chavesDoCiclo = ConcurrentHashMap.newKeySet();
        for (JsonNode transacao : transacoes) {
            if (result.getProcessadas() >= maxItemsPerCycle) {
                result.incrementarIgnoradas();
                continue;
            }

            if (!isReservaAereaReservada(transacao)) {
                result.incrementarIgnoradas();
                continue;
            }

            String uniqueId = uniqueId(transacao);
            if (isBlank(uniqueId)) {
                result.incrementarErros(null);
                alertarErro("Polling Wooba reservadas encontrou transacao sem UniqueId: " + transacao, null);
                continue;
            }

            String cacheKey = cacheKey(transacao, uniqueId);
            if (!chavesDoCiclo.add(cacheKey) || processadosRecentemente.containsKey(cacheKey)) {
                result.incrementarPuladasCache();
                continue;
            }

            try {
                WoobaAirReservationSyncResult syncResult = airReservationService.processarTransactionUniqueId(uniqueId);
                result.incrementarProcessadas(uniqueId, syncResult);
                processadosRecentemente.put(cacheKey, agora);
            } catch (Exception ex) {
                result.incrementarErros(uniqueId);
                alertarErro("Erro ao processar reserva aerea reservada no polling Wooba. UniqueId="
                        + uniqueId + ", Locator=" + safe(text(transacao, "Locator")), ex);
            }
        }

        logInfo("Polling Wooba reservadas finalizado. Encontradas={0}, Processadas={1}, Criadas={2}, Atualizadas={3}, PuladasCache={4}, Ignoradas={5}, Erros={6}",
                new Object[]{
                        result.getEncontradas(),
                        result.getProcessadas(),
                        result.getCriadas(),
                        result.getAtualizadas(),
                        result.getPuladasCache(),
                        result.getIgnoradas(),
                        result.getErros()
                });
        rastrear("Polling Wooba reservadas finalizado. Encontradas=" + result.getEncontradas()
                + ", Processadas=" + result.getProcessadas()
                + ", Criadas=" + result.getCriadas()
                + ", Atualizadas=" + result.getAtualizadas()
                + ", PuladasCache=" + result.getPuladasCache()
                + ", Ignoradas=" + result.getIgnoradas()
                + ", Erros=" + result.getErros());
        return result;
    }

    private ObjectNode montarRequest(OffsetDateTime dateFrom, OffsetDateTime dateTo) {
        ObjectNode request = JsonNodeFactory.instance.objectNode();
        request.put("DateFrom", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateFrom));
        request.put("DateTo", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTo));
        request.put("FilterDateType", "LastUpdate");
        request.put("FilterLinkType", "Any");
        request.put("FilterImportState", "Any");

        ArrayNode transactionTypes = request.putArray("TransactionTypes");
        transactionTypes.add(TRANSACTION_TYPE_AIR_RESERVATION);

        ArrayNode transactionStates = request.putArray("TransactionStates");
        transactionStates.add(TRANSACTION_STATE_RESERVED);

        return request;
    }

    private List<JsonNode> extrairTransacoes(JsonNode response) {
        List<JsonNode> transacoes = new ArrayList<>();
        coletarPrimeiraListaDeTransacoes(response, transacoes);
        return transacoes;
    }

    private boolean coletarPrimeiraListaDeTransacoes(JsonNode node, List<JsonNode> transacoes) {
        if (node == null || node.isNull()) {
            return false;
        }

        if (node.isArray()) {
            List<JsonNode> candidatos = new ArrayList<>();
            for (JsonNode item : node) {
                if (pareceTransacao(item)) {
                    candidatos.add(item);
                }
            }
            if (!candidatos.isEmpty()) {
                transacoes.addAll(candidatos);
                return true;
            }

            for (JsonNode item : node) {
                if (coletarPrimeiraListaDeTransacoes(item, transacoes)) {
                    return true;
                }
            }
            return false;
        }

        if (!node.isObject()) {
            return false;
        }

        for (String fieldName : List.of("Transactions", "Sales", "Items", "Data", "Result", "Results")) {
            if (coletarPrimeiraListaDeTransacoes(node.path(fieldName), transacoes)) {
                return true;
            }
        }

        Iterator<JsonNode> children = node.elements();
        while (children.hasNext()) {
            if (coletarPrimeiraListaDeTransacoes(children.next(), transacoes)) {
                return true;
            }
        }

        return false;
    }

    private boolean pareceTransacao(JsonNode item) {
        return item != null && item.isObject()
                && (item.hasNonNull("UniqueId") || item.hasNonNull("TransactionUniqueId"))
                && (item.has("TransactionType") || item.has("TransactionTypeDescription"));
    }

    private boolean isReservaAereaReservada(JsonNode transacao) {
        Integer transactionType = intValue(transacao, "TransactionType");
        Integer transactionState = intValue(transacao, "TransactionState");
        String transactionTypeDescription = text(transacao, "TransactionTypeDescription");
        String transactionStateDescription = text(transacao, "TransactionStateDescription");
        String uniqueId = uniqueId(transacao);

        boolean reservaAerea = Integer.valueOf(TRANSACTION_TYPE_AIR_RESERVATION).equals(transactionType)
                || "AirReservation".equalsIgnoreCase(defaultString(transactionTypeDescription))
                || defaultString(uniqueId).toUpperCase(Locale.ROOT).startsWith("AIR-");
        boolean reservada = Integer.valueOf(TRANSACTION_STATE_RESERVED).equals(transactionState)
                || "Reserved".equalsIgnoreCase(defaultString(transactionStateDescription));
        return reservaAerea && reservada;
    }

    private Integer intValue(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.path(fieldName);
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isInt() || value.isLong()) {
            return value.asInt();
        }
        if (value.isTextual()) {
            try {
                return Integer.parseInt(value.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.path(fieldName);
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText(null);
        return isBlank(text) ? null : text.trim();
    }

    private String uniqueId(JsonNode transacao) {
        String uniqueId = text(transacao, "UniqueId");
        return isBlank(uniqueId) ? text(transacao, "TransactionUniqueId") : uniqueId;
    }

    private String cacheKey(JsonNode transacao, String uniqueId) {
        return uniqueId + "|" + defaultString(text(transacao, "LastUpdate"));
    }

    private void limparCacheAntigo(OffsetDateTime agora) {
        OffsetDateTime limite = agora.minusMinutes(Math.max(lookbackMinutes * 2, 10));
        processadosRecentemente.entrySet().removeIf(entry -> entry.getValue().isBefore(limite));
    }

    private ZoneOffset zoneOffset() {
        String offset = properties == null ? null : properties.getOffset();
        if (isBlank(offset)) {
            return DEFAULT_OFFSET;
        }

        try {
            return ZoneOffset.of(offset.trim());
        } catch (DateTimeException ignored) {
            return DEFAULT_OFFSET;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String safe(String value) {
        return value == null ? "null" : value;
    }

    private void alertarErro(String mensagem, Exception ex) {
        if (telegramErrorAlert == null) {
            return;
        }
        if (ex == null) {
            telegramErrorAlert.enviar(this, mensagem);
        } else {
            telegramErrorAlert.enviar(this, mensagem, ex);
        }
    }

    private void rastrear(String mensagem) {
        if (traceEnabled && telegramErrorAlert != null) {
            telegramErrorAlert.enviar(this, "[TRACE] " + mensagem);
        }
    }

    private void logInfo(String mensagem) {
        if (logEnabled) {
            LOG.log(Level.INFO, mensagem);
        }
    }

    private void logInfo(String mensagem, Object[] parametros) {
        if (logEnabled) {
            LOG.log(Level.INFO, mensagem, parametros);
        }
    }
}
