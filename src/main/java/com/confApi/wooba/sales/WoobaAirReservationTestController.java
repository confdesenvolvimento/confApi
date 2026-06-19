package com.confApi.wooba.sales;

import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.util.TelegramErrorAlert;
import com.confApi.wooba.sales.dto.WoobaSalesDetailsResponse;
import com.confApi.wooba.webhook.WoobaWebhookRequest;
import com.confApi.wooba.webhook.WoobaWebhookResponse;
import com.confApi.wooba.webhook.WoobaWebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/wooba/teste")
@ConditionalOnProperty(name = "wooba.test-controller.enabled", havingValue = "true")
public class WoobaAirReservationTestController {

    private final WoobaAirReservationService airReservationService;
    private final WoobaWebhookService woobaWebhookService;
    private final WoobaSalesClient woobaSalesClient;
    private final TelegramErrorAlert telegramErrorAlert;

    @Value("${wooba.telegram.enabled:true}")
    private boolean telegramEnabled = true;

    public WoobaAirReservationTestController(WoobaAirReservationService airReservationService,
                                             WoobaWebhookService woobaWebhookService,
                                             WoobaSalesClient woobaSalesClient,
                                             TelegramErrorAlert telegramErrorAlert) {
        this.airReservationService = airReservationService;
        this.woobaWebhookService = woobaWebhookService;
        this.woobaSalesClient = woobaSalesClient;
        this.telegramErrorAlert = telegramErrorAlert;
    }

  /*  @PostMapping("/reserva-aereo/popular")*/
    public ResponseEntity<?> popularReservaAereo(@RequestBody WoobaSalesDetailsResponse details,
                                                 @RequestParam(defaultValue = "true") boolean consultarManager) {
        try {
            ReservaAereo reservaAereo = airReservationService.popularReservaAerea(details, consultarManager);
            return ResponseEntity.ok(reservaAereo);
        } catch (IllegalArgumentException e) {
            alertarErro("Payload invalido no teste de popular reserva aerea Wooba", e);
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            alertarErro("Erro no teste de popular reserva aerea Wooba", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("erro", "Erro interno ao popular reserva aerea Wooba."));
        }
    }

    @PostMapping("/webhook/sales/simular")
    public ResponseEntity<?> simularWebhookSales(@RequestBody WoobaWebhookRequest request,
                                                 @RequestParam(defaultValue = "true") boolean consultarManager,
                                                 @RequestParam(defaultValue = "false") boolean sincronizar) {
        try {
            if (sincronizar) {
                WoobaWebhookResponse response = woobaWebhookService.processar(request);
                return ResponseEntity.ok(response);
            }

            ReservaAereo reservaAereo = airReservationService.carregarReservaAerea(request, consultarManager);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("modo", "POPULAR_SEM_GRAVAR");
            response.put("sincronizar", false);
            response.put("consultarManager", consultarManager);
            response.put("webhook", request);
            response.put("reservaAereo", reservaAereo);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            alertarErro("Payload invalido no teste de webhook Wooba /sales", e);
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            alertarErro("Erro no teste de webhook Wooba /sales", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", e.getMessage() == null ? "Erro interno ao simular webhook Wooba /sales." : e.getMessage()));
        }
    }

    @PostMapping("/sales/list")
    public ResponseEntity<?> testarSalesList(@RequestBody JsonNode request) {
        try {
            return ResponseEntity.ok(woobaSalesClient.list(request));
        } catch (IllegalArgumentException e) {
            alertarErro("Payload invalido no teste Wooba sales/list", e);
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            alertarErro("Erro no teste Wooba sales/list", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", e.getMessage() == null ? "Erro interno ao chamar Wooba sales/list." : e.getMessage()));
        }
    }

    private void alertarErro(String mensagem, Exception e) {
        if (telegramEnabled && telegramErrorAlert != null) {
            telegramErrorAlert.enviar(this, mensagem, e);
        }
    }
}
