package com.confApi.wooba.webhook;

import com.confApi.wooba.sales.WoobaAirReservationService;
import com.confApi.wooba.sales.WoobaAirReservationSyncResult;
import com.confApi.wooba.sales.dto.WoobaSalesDetailsResponse;
import com.confApi.util.TelegramErrorAlert;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks/wooba")
public class WoobaWebhookController {

    private final WoobaWebhookService woobaWebhookService;
    private final WoobaAirReservationService airReservationService;
    private final TelegramErrorAlert telegramErrorAlert;

    public WoobaWebhookController(WoobaWebhookService woobaWebhookService,
                                  WoobaAirReservationService airReservationService,
                                  TelegramErrorAlert telegramErrorAlert) {
        this.woobaWebhookService = woobaWebhookService;
        this.airReservationService = airReservationService;
        this.telegramErrorAlert = telegramErrorAlert;
    }

    @PostMapping(
            value = "/sales",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WoobaWebhookResponse> receberSales(@RequestBody WoobaWebhookRequest request) {
        try {
            return ResponseEntity.ok(woobaWebhookService.processar(request));
        } catch (IllegalArgumentException ex) {
            telegramErrorAlert.enviar(this, "Payload invalido recebido no webhook Wooba /sales", ex);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(WoobaWebhookResponse.invalid(ex.getMessage()));
        } catch (Exception ex) {
            telegramErrorAlert.enviar(this, "Erro ao processar webhook Wooba /sales", ex);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WoobaWebhookResponse.invalid("Erro interno ao processar webhook Wooba."));
        }
    }

    /*@PostMapping(
            value = "/sales/details",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )*/
    public ResponseEntity<?> sincronizarSalesDetails(@RequestBody WoobaSalesDetailsResponse details) {
        try {
            WoobaAirReservationSyncResult result = airReservationService.processarDetails(details);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            telegramErrorAlert.enviar(this, "Payload invalido recebido no webhook Wooba /sales/details", ex);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(WoobaWebhookResponse.invalid(ex.getMessage()));
        } catch (Exception ex) {
            telegramErrorAlert.enviar(this, "Erro ao processar webhook Wooba /sales/details", ex);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WoobaWebhookResponse.invalid("Erro interno ao processar details Wooba."));
        }
    }
}
