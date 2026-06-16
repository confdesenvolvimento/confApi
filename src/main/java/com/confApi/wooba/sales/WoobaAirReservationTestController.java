package com.confApi.wooba.sales;

import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.util.TelegramErrorAlert;
import com.confApi.wooba.sales.dto.WoobaSalesDetailsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/wooba/teste")
public class WoobaAirReservationTestController {

    private final WoobaAirReservationService airReservationService;
    private final TelegramErrorAlert telegramErrorAlert;

    public WoobaAirReservationTestController(WoobaAirReservationService airReservationService,
                                             TelegramErrorAlert telegramErrorAlert) {
        this.airReservationService = airReservationService;
        this.telegramErrorAlert = telegramErrorAlert;
    }

  /*  @PostMapping("/reserva-aereo/popular")*/
    public ResponseEntity<?> popularReservaAereo(@RequestBody WoobaSalesDetailsResponse details,
                                                 @RequestParam(defaultValue = "true") boolean consultarManager) {
        try {
            ReservaAereo reservaAereo = airReservationService.popularReservaAerea(details, consultarManager);
            return ResponseEntity.ok(reservaAereo);
        } catch (IllegalArgumentException e) {
            telegramErrorAlert.enviar(this, "Payload invalido no teste de popular reserva aerea Wooba", e);
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            telegramErrorAlert.enviar(this, "Erro no teste de popular reserva aerea Wooba", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("erro", "Erro interno ao popular reserva aerea Wooba."));
        }
    }
}
