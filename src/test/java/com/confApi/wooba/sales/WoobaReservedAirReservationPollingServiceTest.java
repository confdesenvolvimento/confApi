package com.confApi.wooba.sales;

import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WoobaReservedAirReservationPollingServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WoobaSalesClient woobaSalesClient = mock(WoobaSalesClient.class);
    private final WoobaAirReservationService airReservationService = mock(WoobaAirReservationService.class);
    private final WoobaSalesProperties properties = new WoobaSalesProperties();

    @Test
    void deveBuscarReservasAereasReservadasEProcessarDetails() throws Exception {
        WoobaReservedAirReservationPollingService service = service(5, 200);
        when(woobaSalesClient.list(any(JsonNode.class))).thenReturn(objectMapper.readTree("""
                {
                  "Success": true,
                  "Transactions": [
                    {
                      "TransactionType": 1,
                      "TransactionTypeDescription": "AirReservation",
                      "UniqueId": "AIR-RESERVADA-1",
                      "Locator": "ABC123",
                      "LastUpdate": "2026-06-17T15:00:00",
                      "TransactionState": 2,
                      "TransactionStateDescription": "Reserved"
                    },
                    {
                      "TransactionType": 100,
                      "TransactionTypeDescription": "AirTicket",
                      "UniqueId": "TKT-EMITIDA-1",
                      "Locator": "ABC123",
                      "LastUpdate": "2026-06-17T15:01:00",
                      "TransactionState": 4,
                      "TransactionStateDescription": "Issued"
                    },
                    {
                      "TransactionType": 1,
                      "TransactionTypeDescription": "AirReservation",
                      "UniqueId": "AIR-EMITIDA-1",
                      "Locator": "XYZ123",
                      "LastUpdate": "2026-06-17T15:02:00",
                      "TransactionState": 4,
                      "TransactionStateDescription": "Issued"
                    },
                    {
                      "TransactionType": 1,
                      "TransactionTypeDescription": "AirReservation",
                      "TransactionUniqueId": "AIR-RESERVADA-2",
                      "Locator": "DEF456",
                      "LastUpdate": "2026-06-17T15:03:00",
                      "TransactionState": 2,
                      "TransactionStateDescription": "Reserved"
                    }
                  ]
                }
                """));
        when(airReservationService.processarTransactionUniqueId("AIR-RESERVADA-1"))
                .thenReturn(syncResult(true, false));
        when(airReservationService.processarTransactionUniqueId("AIR-RESERVADA-2"))
                .thenReturn(syncResult(false, true));

        WoobaReservedAirReservationPollingResult result = service.executarAgora();

        assertEquals(4, result.getEncontradas());
        assertEquals(2, result.getProcessadas());
        assertEquals(1, result.getCriadas());
        assertEquals(1, result.getAtualizadas());
        assertEquals(2, result.getIgnoradas());
        assertEquals(0, result.getErros());
        assertTrue(result.getUniqueIdsProcessados().contains("AIR-RESERVADA-1"));
        assertTrue(result.getUniqueIdsProcessados().contains("AIR-RESERVADA-2"));

        ArgumentCaptor<JsonNode> requestCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(woobaSalesClient).list(requestCaptor.capture());
        JsonNode request = requestCaptor.getValue();
        assertEquals("LastUpdate", request.path("FilterDateType").asText());
        assertEquals(1, request.path("TransactionTypes").get(0).asInt());
        assertEquals(2, request.path("TransactionStates").get(0).asInt());

        verify(airReservationService).processarTransactionUniqueId("AIR-RESERVADA-1");
        verify(airReservationService).processarTransactionUniqueId("AIR-RESERVADA-2");
    }

    @Test
    void naoDeveReprocessarMesmaReservaComMesmoLastUpdateDentroDaJanela() throws Exception {
        WoobaReservedAirReservationPollingService service = service(5, 200);
        when(woobaSalesClient.list(any(JsonNode.class))).thenReturn(objectMapper.readTree("""
                {
                  "Transactions": [
                    {
                      "TransactionType": 1,
                      "TransactionTypeDescription": "AirReservation",
                      "UniqueId": "AIR-RESERVADA-1",
                      "Locator": "ABC123",
                      "LastUpdate": "2026-06-17T15:00:00",
                      "TransactionState": 2,
                      "TransactionStateDescription": "Reserved"
                    }
                  ]
                }
                """));
        when(airReservationService.processarTransactionUniqueId("AIR-RESERVADA-1"))
                .thenReturn(syncResult(true, false));

        WoobaReservedAirReservationPollingResult primeiraExecucao = service.executarAgora();
        WoobaReservedAirReservationPollingResult segundaExecucao = service.executarAgora();

        assertEquals(1, primeiraExecucao.getProcessadas());
        assertEquals(0, segundaExecucao.getProcessadas());
        assertEquals(1, segundaExecucao.getPuladasCache());
        verify(airReservationService, times(1)).processarTransactionUniqueId("AIR-RESERVADA-1");
    }

    private WoobaReservedAirReservationPollingService service(long lookbackMinutes, int maxItemsPerCycle) {
        properties.setOffset("-03:00:00");
        return new WoobaReservedAirReservationPollingService(
                woobaSalesClient,
                properties,
                airReservationService,
                lookbackMinutes,
                maxItemsPerCycle,
                false,
                false
        );
    }

    private WoobaAirReservationSyncResult syncResult(boolean created, boolean updated) {
        WoobaAirReservationSyncResult result = WoobaAirReservationSyncResult.processed(new ReservaAereo());
        result.setCreated(created);
        result.setUpdated(updated);
        return result;
    }
}
