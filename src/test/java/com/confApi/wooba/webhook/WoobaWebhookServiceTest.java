package com.confApi.wooba.webhook;

import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.util.TelegramErrorAlert;
import com.confApi.wooba.sales.WoobaAirReservationSyncResult;
import com.confApi.wooba.sales.WoobaAirReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WoobaWebhookServiceTest {

    private final WoobaAirReservationService airReservationService = mock(WoobaAirReservationService.class);
    private final WoobaWebhookService service = new WoobaWebhookService(airReservationService);

    @Test
    void processarDeveAceitarReservaAerea() {
        when(airReservationService.processarWebhook(any())).thenReturn(WoobaAirReservationSyncResult.processed(new ReservaAereo()));

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
        verify(airReservationService).processarWebhook(request);
    }

    @Test
    void processarDeveAceitarBilheteAereo() {
        when(airReservationService.processarWebhook(any())).thenReturn(WoobaAirReservationSyncResult.processed(new ReservaAereo()));

        WoobaWebhookRequest request = new WoobaWebhookRequest();
        request.setApi("Travellink-ApiSales");
        request.setTransactionType(100);
        request.setTransactionTypeDescription("AirTicket");
        request.setId(11264222L);
        request.setUniqueId("TKT-E7FE7059-5DC0-4234-850C-668E99A67148");
        request.setLocator("GXNHDJ");
        request.setTicket("7242614288032");
        request.setLastUpdate("2026-05-25T10:19:04.387");

        WoobaWebhookResponse response = service.processar(request);

        assertEquals("RECEIVED", response.getStatus());
        assertTrue(response.getAccepted());
        assertTrue(response.getAirReservation());
        assertEquals("TKT-E7FE7059-5DC0-4234-850C-668E99A67148", response.getUniqueId());
        verify(airReservationService).processarWebhook(request);
    }

    @Test
    void processarDeveAceitarLastUpdateComOffsetEFractionLonga() {
        when(airReservationService.processarWebhook(any())).thenReturn(WoobaAirReservationSyncResult.processed(new ReservaAereo()));

        WoobaWebhookRequest request = new WoobaWebhookRequest();
        request.setApi("Travellink-ApiSales");
        request.setTransactionType(100);
        request.setTransactionTypeDescription("AirTicket");
        request.setId(11343766L);
        request.setUniqueId("TKT-E91F63ED-1127-4D53-86B4-C2B41994763B");
        request.setLocator("AVP5AG");
        request.setTicket("0579293566723");
        request.setLastUpdate("2026-06-17T11:43:24.8154967-03:00");

        WoobaWebhookResponse response = service.processar(request);

        assertEquals("RECEIVED", response.getStatus());
        assertTrue(response.getAccepted());
        assertTrue(response.getAirReservation());
        verify(airReservationService).processarWebhook(request);
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
        verify(airReservationService, never()).processarWebhook(any());
    }

    @Test
    void naoDeveEnviarTelegramQuandoIntegracaoWoobaEstiverComTelegramDesabilitado() {
        TelegramErrorAlert telegramErrorAlert = mock(TelegramErrorAlert.class);
        ReflectionTestUtils.setField(service, "telegramErrorAlert", telegramErrorAlert);
        ReflectionTestUtils.setField(service, "telegramEnabled", false);

        WoobaWebhookRequest request = new WoobaWebhookRequest();
        request.setApi("Travellink-ApiSales");
        request.setTransactionType(999);
        request.setTransactionTypeDescription("ProdutoNaoMapeado");
        request.setUniqueId("XXX-123");

        WoobaWebhookResponse response = service.processar(request);

        assertEquals("IGNORED", response.getStatus());
        verify(telegramErrorAlert, never()).enviar(any(Object.class), any(String.class));
        verify(telegramErrorAlert, never()).enviar(any(Object.class), any(String.class), any(Exception.class));
    }

    @Test
    void processarDeveIgnorarQuandoWebhookSalesEstiverDesabilitado() {
        ReflectionTestUtils.setField(service, "salesWebhookEnabled", false);

        WoobaWebhookRequest request = new WoobaWebhookRequest();
        request.setApi("Travellink-ApiSales");
        request.setTransactionType(1);
        request.setTransactionTypeDescription("AirReservation");
        request.setId(16164L);
        request.setUniqueId("AIR-DE9F11CB-8A50-4BF0-8AFD-FE010D8DB63C");
        request.setLocator("FYVGWW");

        WoobaWebhookResponse response = service.processar(request);

        assertEquals("DISABLED", response.getStatus());
        assertTrue(response.getAccepted());
        assertEquals(Boolean.FALSE, response.getAirReservation());
        assertEquals("AIR-DE9F11CB-8A50-4BF0-8AFD-FE010D8DB63C", response.getUniqueId());
        verify(airReservationService, never()).processarWebhook(any());
    }

    @Test
    void processarDeveRejeitarPayloadSemUniqueId() {
        WoobaWebhookRequest request = new WoobaWebhookRequest();
        request.setApi("Travellink-ApiSales");
        request.setTransactionType(1);

        assertThrows(IllegalArgumentException.class, () -> service.processar(request));
    }
}
