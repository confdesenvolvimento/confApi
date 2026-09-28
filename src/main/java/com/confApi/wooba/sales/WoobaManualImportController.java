package com.confApi.wooba.sales;

import com.confApi.util.TelegramErrorAlert;
import com.confApi.wooba.sales.dto.WoobaManualImportRequest;
import com.confApi.wooba.sales.dto.WoobaManualImportResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.logging.Logger;

@RestController
@RequestMapping("/api/wooba/reservas-aereas")
public class WoobaManualImportController {
    private static final Logger LOG = Logger.getLogger(WoobaManualImportController.class.getName());
    private final WoobaManualImportService service;
    private final TelegramErrorAlert telegram;
    private final boolean enabled;
    private final boolean telegramEnabled;
    private final String clientLogin;

    public WoobaManualImportController(WoobaManualImportService service, TelegramErrorAlert telegram,
            @Value("${wooba.importacao-manual.enabled:true}") boolean enabled,
            @Value("${wooba.telegram.enabled:true}") boolean telegramEnabled,
            @Value("${wooba.importacao-manual.cliente-login:api.confplus}") String clientLogin) {
        this.service = service;
        this.telegram = telegram;
        this.enabled = enabled;
        this.telegramEnabled = telegramEnabled;
        this.clientLogin = clientLogin;
    }

    @PostMapping(value = "/importar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WoobaManualImportResponse> importar(@RequestBody WoobaManualImportRequest request, Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return erro(HttpStatus.UNAUTHORIZED, "Autenticacao obrigatoria.");
        }
        if (clientLogin == null || clientLogin.isBlank() || !clientLogin.equalsIgnoreCase(auth.getName())) {
            return erro(HttpStatus.FORBIDDEN, "Aplicacao nao autorizada para importar reservas.");
        }
        if (!enabled) return erro(HttpStatus.SERVICE_UNAVAILABLE, "Importacao manual Wooba desabilitada.");
        try {
            return ResponseEntity.ok(service.importar(request));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getRawStatusCode()).body(WoobaManualImportResponse.erro(ex.getReason()));
        } catch (Exception ex) {
            String message = "Falha na importacao manual Wooba: " + ex.getClass().getSimpleName();
            LOG.warning(message);
            if (telegramEnabled && telegram != null) telegram.enviar(this, message);
            return erro(HttpStatus.BAD_GATEWAY, "Nao foi possivel confirmar a importacao. Confira a reserva antes de tentar novamente.");
        }
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<WoobaManualImportResponse> invalidJson() {
        return erro(HttpStatus.BAD_REQUEST, "JSON invalido. Informe localizador e dataCriacao no formato yyyy-MM-dd.");
    }

    private ResponseEntity<WoobaManualImportResponse> erro(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(WoobaManualImportResponse.erro(message));
    }
}
