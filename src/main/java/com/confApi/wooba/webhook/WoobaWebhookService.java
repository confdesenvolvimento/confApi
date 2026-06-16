package com.confApi.wooba.webhook;

import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.util.TelegramErrorAlert;
import com.confApi.wooba.sales.WoobaAirReservationSyncResult;
import com.confApi.wooba.sales.WoobaAirReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class WoobaWebhookService {

    private static final Logger LOG = Logger.getLogger(WoobaWebhookService.class.getName());
    private final WoobaAirReservationService airReservationService;

    @Autowired(required = false)
    private TelegramErrorAlert telegramErrorAlert;

    private static final List<DateTimeFormatter> LAST_UPDATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
    );

    public WoobaWebhookService(WoobaAirReservationService airReservationService) {
        this.airReservationService = airReservationService;
    }

    public WoobaWebhookResponse processar(WoobaWebhookRequest request) {
        validarPayload(request);

        Optional<WoobaTransactionType> transactionType =
                WoobaTransactionType.fromCode(request.getTransactionType());

        if (transactionType.isEmpty()) {
            LOG.log(
                    Level.WARNING,
                    "Webhook Wooba recebido com tipo de transacao desconhecido. Payload: {0}",
                    request
            );
            alertarErro("Webhook Wooba recebido com tipo de transacao desconhecido. UniqueId: "
                    + request.getUniqueId() + ", TransactionType: " + request.getTransactionType());
            return WoobaWebhookResponse.ignored(request, "Tipo de transacao Wooba nao mapeado.");
        }

        if (!isReservaAerea(request, transactionType.get())) {
            LOG.log(
                    Level.INFO,
                    "Webhook Wooba recebido e ignorado por nao ser reserva aerea. Tipo: {0}, UniqueId: {1}",
                    new Object[]{transactionType.get().getDescription(), request.getUniqueId()}
            );
            return WoobaWebhookResponse.ignored(request, "Evento recebido, mas ainda nao processado para este produto.");
        }

        processarReservaAerea(request);
        return WoobaWebhookResponse.receivedAirReservation(request);
    }

    private void validarPayload(WoobaWebhookRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Payload do webhook Wooba nao informado.");
        }

        if (isBlank(request.getApi())) {
            throw new IllegalArgumentException("Campo Api do webhook Wooba nao informado.");
        }

        if (request.getTransactionType() == null) {
            throw new IllegalArgumentException("Campo TransactionType do webhook Wooba nao informado.");
        }

        if (isBlank(request.getUniqueId())) {
            throw new IllegalArgumentException("Campo UniqueId do webhook Wooba nao informado.");
        }
    }

    private boolean isReservaAerea(WoobaWebhookRequest request, WoobaTransactionType transactionType) {
        if (transactionType.isAir()) {
            return true;
        }

        String uniqueId = request.getUniqueId();
        if (uniqueId == null) {
            return false;
        }

        String normalized = uniqueId.toUpperCase(Locale.ROOT);
        return normalized.startsWith("AIR-") || normalized.startsWith("TKT-");
    }

    private void processarReservaAerea(WoobaWebhookRequest request) {
        LocalDateTime lastUpdate = parseLastUpdate(request.getLastUpdate());
        WoobaAirReservationSyncResult syncResult = airReservationService.processarWebhook(request);
        ReservaAereo reservaAereo = syncResult.getReserva();

        LOG.log(
                Level.INFO,
                "Webhook Wooba de reserva aerea processado. UniqueId: {0}, Locator: {1}, Id: {2}, LastUpdate: {3}, Action: {4}, Created: {5}, Updated: {6}, Passageiros: {7}, Trechos: {8}",
                new Object[]{
                        request.getUniqueId(),
                        request.getLocator(),
                        request.getId(),
                        lastUpdate,
                        syncResult.getAction(),
                        syncResult.isCreated(),
                        syncResult.isUpdated(),
                        reservaAereo != null && reservaAereo.getPassageiros() != null ? reservaAereo.getPassageiros().size() : 0,
                        reservaAereo != null && reservaAereo.getTrechos() != null ? reservaAereo.getTrechos().size() : 0
                }
        );
    }

    private LocalDateTime parseLastUpdate(String lastUpdate) {
        if (isBlank(lastUpdate)) {
            return null;
        }

        String normalized = lastUpdate.trim();

        for (DateTimeFormatter formatter : LAST_UPDATE_FORMATS) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // Tenta o proximo formato aceito pela Wooba.
            }
        }

        LOG.log(Level.WARNING, "Nao foi possivel converter LastUpdate do webhook Wooba: {0}", lastUpdate);
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void alertarErro(String mensagem) {
        if (telegramErrorAlert != null) {
            telegramErrorAlert.enviar(this, mensagem);
        }
    }
}
