package com.confApi.wooba.webhook;

import com.confApi.wooba.sales.WoobaAirReservationService;
import com.confApi.wooba.sales.WoobaAirReservationSyncResult;
import com.confApi.wooba.sales.dto.WoobaSalesDetailsResponse;
import com.confApi.util.TelegramErrorAlert;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${wooba.webhook.trace.enabled:false}")
    private boolean traceEnabled;

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
            rastrear("Webhook Wooba /sales recebido. " + resumo(request));
            WoobaWebhookResponse response = woobaWebhookService.processar(request);
            rastrear("Webhook Wooba /sales respondido com sucesso. "
                    + resumo(request) + ", Status=" + response.getStatus()
                    + ", Accepted=" + response.getAccepted()
                    + ", AirReservation=" + response.getAirReservation());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            telegramErrorAlert.enviar(this, "Payload invalido recebido no webhook Wooba /sales. " + resumo(request), ex);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(WoobaWebhookResponse.invalid(ex.getMessage()));
        } catch (Exception ex) {
            telegramErrorAlert.enviar(this, "Erro ao processar webhook Wooba /sales. " + resumo(request), ex);
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
            rastrear("Webhook Wooba /sales/details recebido para teste.");
            WoobaAirReservationSyncResult result = airReservationService.processarDetails(details);
            rastrear("Webhook Wooba /sales/details processado. Action="
                    + result.getAction() + ", Created=" + result.isCreated() + ", Updated=" + result.isUpdated());
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

    private String resumo(WoobaWebhookRequest request) {
        return request == null ? "Payload=null" : request.resumo();
    }

    private void rastrear(String mensagem) {
        if (traceEnabled) {
            telegramErrorAlert.enviar(this, "[TRACE] " + mensagem);
        }
    }
}
