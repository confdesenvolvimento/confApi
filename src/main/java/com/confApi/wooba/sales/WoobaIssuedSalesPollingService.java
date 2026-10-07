package com.confApi.wooba.sales;

import com.confApi.util.TelegramErrorAlert;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class WoobaIssuedSalesPollingService {
    private static final Logger LOG = Logger.getLogger(WoobaIssuedSalesPollingService.class.getName());
    private final WoobaSalesClient client;
    private final WoobaIssuedAirReservationImportService importer;
    private final WoobaIssuedPollingProperties settings;
    private final WoobaIssuedPollingStore store;
    private final WoobaSalesProperties sales;
    private final Clock clock;
    private final AtomicBoolean ticketsScheduled = new AtomicBoolean();
    private final AtomicBoolean reservationsScheduled = new AtomicBoolean();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "wooba-issued-polling");
        thread.setDaemon(true);
        return thread;
    });

    @Autowired(required = false)
    private TelegramErrorAlert telegramErrorAlert;
    @Value("${wooba.telegram.enabled:true}")
    private boolean telegramEnabled;
    @Value("${wooba.webhook.log.enabled:false}")
    private boolean logEnabled;

    @Autowired
    public WoobaIssuedSalesPollingService(WoobaSalesClient client, WoobaIssuedAirReservationImportService importer,
            WoobaIssuedPollingProperties settings, WoobaIssuedPollingStore store, WoobaSalesProperties sales) {
        this(client, importer, settings, store, sales, Clock.systemUTC());
    }

    WoobaIssuedSalesPollingService(WoobaSalesClient client, WoobaIssuedAirReservationImportService importer,
            WoobaIssuedPollingProperties settings, WoobaIssuedPollingStore store, WoobaSalesProperties sales, Clock clock) {
        this.client = client;
        this.importer = importer;
        this.settings = settings;
        this.store = store;
        this.sales = sales;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${wooba.sales.polling.issued.tickets.fixed-delay-ms:300000}",
            initialDelayString = "${wooba.sales.polling.issued.tickets.initial-delay-ms:60000}")
    public void consultarBilhetes() {
        if (settings.getTickets().isEnabled()) {
            dispatch(100, settings.getTickets(), ticketsScheduled);
        }
    }

    @Scheduled(fixedDelayString = "${wooba.sales.polling.issued.reservations.fixed-delay-ms:900000}",
            initialDelayString = "${wooba.sales.polling.issued.reservations.initial-delay-ms:120000}")
    public void conferirReservasEmitidas() {
        if (settings.getReservations().isEnabled()) {
            dispatch(1, settings.getReservations(), reservationsScheduled);
        }
    }

    private void dispatch(int type, WoobaIssuedPollingProperties.Job job, AtomicBoolean scheduled) {
        if (!scheduled.compareAndSet(false, true)) {
            return;
        }
        try {
            worker.execute(() -> {
                try {
                    executar(type, job);
                } finally {
                    scheduled.set(false);
                }
            });
        } catch (RuntimeException ex) {
            scheduled.set(false);
            alert("Nao foi possivel agendar polling Wooba. Tipo=" + type, ex);
        }
    }

    @PreDestroy
    public void shutdown() {
        worker.shutdown();
        try {
            if (!worker.awaitTermination(10, TimeUnit.SECONDS)) {
                worker.shutdownNow();
            }
        } catch (InterruptedException ex) {
            worker.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Os dois agendamentos compartilham o arquivo, inclusive com schedulers multithread.
    synchronized void executar(int type, WoobaIssuedPollingProperties.Job job) {
        try {
            WoobaIssuedPollingStore.State state = store.load();
            WoobaIssuedPollingStore.Stream stream = state.stream(type);
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(offset());
            OffsetDateTime cursor = stream.getQueriedThrough() == null
                    ? now.minusMinutes(Math.max(1, job.getInitialLookbackMinutes()))
                    : OffsetDateTime.parse(stream.getQueriedThrough());
            OffsetDateTime to = cursor.plusMinutes(Math.max(1, settings.getQueryWindowMinutes()));
            if (to.isAfter(now)) {
                to = now;
            }
            OffsetDateTime from = cursor.minusMinutes(Math.max(1, settings.getOverlapMinutes()));
            try {
                enqueue(stream, request(type, from, to), now);
                stream.setQueriedThrough(to.toString());
                store.save(state);
            } catch (RuntimeException ex) {
                alert("Falha na consulta Wooba de emitidas. Tipo=" + type, ex);
            }

            int attempts = 0;
            for (Map.Entry<String, WoobaIssuedPollingStore.Pending> entry : new ArrayList<>(stream.getPending().entrySet())) {
                if (attempts++ >= Math.max(1, settings.getMaxItemsPerCycle())) {
                    break;
                }
                WoobaIssuedPollingStore.Pending pending = entry.getValue();
                try {
                    importer.processar(pending.getUniqueId(), type);
                    stream.getPending().remove(entry.getKey());
                    stream.getCompleted().put(entry.getKey(), now.toInstant().toEpochMilli());
                } catch (Exception ex) {
                    pending.setAttempts(pending.getAttempts() + 1);
                    // Persiste apenas o tipo do erro, sem respostas HTTP ou dados pessoais.
                    pending.setLastError(ex.getClass().getSimpleName());
                    stream.getPending().remove(entry.getKey());
                    stream.getPending().put(entry.getKey(), pending);
                    alert("Importacao Wooba emitida pendente. UniqueId=" + pending.getUniqueId()
                            + ", Locator=" + pending.getLocator(), ex);
                }
                store.save(state);
            }
            if (logEnabled) {
                LOG.log(Level.INFO, "Polling Wooba emitidas. Tipo={0}, Pendentes={1}",
                        new Object[]{type, stream.getPending().size()});
            }
        } catch (Exception ex) {
            alert("Falha no polling Wooba de emitidas. Tipo=" + type, ex);
        }
    }

    private void enqueue(WoobaIssuedPollingStore.Stream stream, ObjectNode request, OffsetDateTime now) {
        JsonNode response = client.list(request);
        if (response == null || (response.has("Success") && !response.path("Success").asBoolean())
                || (response.path("Errors").isArray() && response.path("Errors").size() > 0)) {
            throw new IllegalStateException("Wooba sales/list nao confirmou sucesso.");
        }
        long cutoff = now.minusDays(Math.max(settings.getCompletedRetentionDays(), 1)).toInstant().toEpochMilli();
        stream.getCompleted().entrySet().removeIf(entry -> entry.getValue() < cutoff);
        int type = request.path("TransactionTypes").get(0).asInt();
        for (JsonNode item : WoobaSalesListTransactions.extract(response)) {
            if (!WoobaSalesListTransactions.matches(item, type, 4)) {
                continue;
            }
            String uniqueId = WoobaSalesListTransactions.uniqueId(item);
            if (uniqueId.isBlank() || item.path("LastUpdate").asText("").isBlank()) {
                throw new IllegalStateException("Transacao Wooba emitida sem UniqueId ou LastUpdate.");
            }
            WoobaIssuedPollingStore.Pending pending = new WoobaIssuedPollingStore.Pending(uniqueId,
                    item.path("Locator").asText(""), item.path("LastUpdate").asText());
            if (!stream.getCompleted().containsKey(pending.key())) {
                stream.getPending().putIfAbsent(pending.key(), pending);
            }
        }
    }

    private ObjectNode request(int type, OffsetDateTime from, OffsetDateTime to) {
        ObjectNode request = JsonNodeFactory.instance.objectNode();
        request.put("DateFrom", from.toString());
        request.put("DateTo", to.toString());
        request.put("FilterDateType", "LastUpdate");
        request.put("FilterLinkType", "Any");
        request.put("FilterImportState", "Any");
        request.putArray("TransactionTypes").add(type);
        request.putArray("TransactionStates").add(4);
        return request;
    }

    private ZoneOffset offset() {
        return sales.getOffset() == null ? ZoneOffset.of("-03:00") : ZoneOffset.of(sales.getOffset());
    }

    private void alert(String message, Exception ex) {
        LOG.log(Level.WARNING, message + ": " + ex.getMessage());
        if (telegramEnabled && telegramErrorAlert != null) {
            telegramErrorAlert.enviar(this, message, ex);
        }
    }
}
