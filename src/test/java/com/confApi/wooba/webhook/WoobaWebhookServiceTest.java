package com.confApi.wooba.webhook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WoobaWebhookServiceTest {

    private final WoobaWebhookService service = new WoobaWebhookService();

    @Test
    void processarDeveAceitarReservaAerea() {
        WoobaWebhookRequest request = new WoobaWebhookRequest();
        request.setApi("Travellink-ApiSales");
        request.setTransactionType(1);
        request.setTransactionTypeDescription("AirReservation");
        request.setId(16164L);
        request.setUniqueId("AIR-DE9F11CB-8A50-4BF0-8AFD-FE010D8DB63C");
        request.setLocator("FYVGWW");
        request.setLastUpdate("2022-03-04T18:24:38.457");

        WoobaWebhookResponse response = service.processar(request);

        assertEquals("RECEIVED", response.getStatus());
        assertTrue(response.getAccepted());
        assertTrue(response.getAirReservation());
        assertEquals("AIR-DE9F11CB-8A50-4BF0-8AFD-FE010D8DB63C", response.getUniqueId());
    }

    @Test
    void processarDeveIgnorarProdutoAindaNaoProcessado() {
        WoobaWebhookRequest request = new WoobaWebhookRequest();
        request.setApi("Travellink-ApiSales");
        request.setTransactionType(2);
        request.setTransactionTypeDescription("Hotel");
        request.setId(12050L);
        request.setUniqueId("HTL-CE569F58-962A-4B3C-9320-A269AE8AF5B2");
        request.setLocator("PV_HTL-67AC9A90D5");
        request.setLastUpdate("11/11/2022 14:57:17");

        WoobaWebhookResponse response = service.processar(request);

        assertEquals("IGNORED", response.getStatus());
        assertTrue(response.getAccepted());
        assertEquals(Boolean.FALSE, response.getAirReservation());
    }

    @Test
    void processarDeveRejeitarPayloadSemUniqueId() {
        WoobaWebhookRequest request = new WoobaWebhookRequest();
        request.setApi("Travellink-ApiSales");
        request.setTransactionType(1);

        assertThrows(IllegalArgumentException.class, () -> service.processar(request));
    }
}
