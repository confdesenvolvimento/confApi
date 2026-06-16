package com.confApi.wooba.sales;

import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.wooba.sales.dto.WoobaSalesDetailsResponse;
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

    public WoobaAirReservationTestController(WoobaAirReservationService airReservationService) {
        this.airReservationService = airReservationService;
    }

    @PostMapping("/reserva-aereo/popular")
    public ResponseEntity<?> popularReservaAereo(@RequestBody WoobaSalesDetailsResponse details,
                                                 @RequestParam(defaultValue = "true") boolean consultarManager) {
        try {
            ReservaAereo reservaAereo = airReservationService.popularReservaAerea(details, consultarManager);
            return ResponseEntity.ok(reservaAereo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }
}
