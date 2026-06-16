package com.confApi.wooba.webhook;

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

    public WoobaWebhookController(WoobaWebhookService woobaWebhookService) {
        this.woobaWebhookService = woobaWebhookService;
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
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(WoobaWebhookResponse.invalid(ex.getMessage()));
        }
    }
}
