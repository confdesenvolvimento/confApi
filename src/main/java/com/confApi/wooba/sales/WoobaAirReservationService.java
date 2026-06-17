package com.confApi.wooba.sales;

import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.wooba.sales.dto.WoobaSalesDetailsResponse;
import com.confApi.wooba.webhook.WoobaWebhookRequest;
import org.springframework.stereotype.Service;

@Service
public class WoobaAirReservationService {

    private final WoobaSalesClient woobaSalesClient;
    private final WoobaAirReservationMapper mapper;
    private final WoobaAirReservationManagerResolver managerResolver;
    private final WoobaAirReservationSyncService syncService;

    public WoobaAirReservationService(WoobaSalesClient woobaSalesClient,
                                      WoobaAirReservationMapper mapper,
                                      WoobaAirReservationManagerResolver managerResolver,
                                      WoobaAirReservationSyncService syncService) {
        this.woobaSalesClient = woobaSalesClient;
        this.mapper = mapper;
        this.managerResolver = managerResolver;
        this.syncService = syncService;
    }

    public ReservaAereo carregarReservaAerea(WoobaWebhookRequest request) {
        return carregarReservaAerea(request, true);
    }

    public ReservaAereo carregarReservaAerea(WoobaWebhookRequest request, boolean consultarManager) {
        if (request == null || request.getUniqueId() == null || request.getUniqueId().trim().isEmpty()) {
            throw new IllegalArgumentException("Webhook Wooba sem UniqueId para buscar details.");
        }

        WoobaSalesDetailsResponse details = woobaSalesClient.details(request.getUniqueId());
        return popularReservaAerea(details, request, consultarManager);
    }

    public WoobaAirReservationSyncResult processarWebhook(WoobaWebhookRequest request) {
        ReservaAereo reservaAereo = carregarReservaAerea(request);
        return syncService.sincronizar(reservaAereo);
    }

    public WoobaAirReservationSyncResult processarTransactionUniqueId(String transactionUniqueId) {
        if (transactionUniqueId == null || transactionUniqueId.trim().isEmpty()) {
            throw new IllegalArgumentException("UniqueId da transacao Wooba nao informado.");
        }

        WoobaSalesDetailsResponse details = woobaSalesClient.details(transactionUniqueId);
        return processarDetails(details);
    }

    public WoobaAirReservationSyncResult processarDetails(WoobaSalesDetailsResponse details) {
        ReservaAereo reservaAereo = popularReservaAerea(details, true);
        return syncService.sincronizar(reservaAereo);
    }

    public ReservaAereo popularReservaAerea(WoobaSalesDetailsResponse details, boolean consultarManager) {
        return popularReservaAerea(details, null, consultarManager);
    }

    private ReservaAereo popularReservaAerea(WoobaSalesDetailsResponse details,
                                             WoobaWebhookRequest request,
                                             boolean consultarManager) {
        ReservaAereo reservaAereo = mapper.toReservaAereo(details, request);
        return consultarManager ? managerResolver.resolverReferenciasManager(reservaAereo) : reservaAereo;
    }
}
