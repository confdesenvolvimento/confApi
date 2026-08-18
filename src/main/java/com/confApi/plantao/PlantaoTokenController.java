package com.confApi.plantao;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plantao/token")
public class PlantaoTokenController {

    private final PlantaoTokenService service;

    public PlantaoTokenController(PlantaoTokenService service) {
        this.service = service;
    }

    @PostMapping("/gerar")
    public ResponseEntity<PlantaoTokenResponse> gerar(
            @RequestBody PlantaoTokenRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        PlantaoTokenResponse response = service.gerar(request, authorization);
        return response.success()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @PostMapping("/validar")
    public ResponseEntity<PlantaoTokenResponse> validar(@RequestBody PlantaoTokenValidationRequest request) {
        PlantaoTokenResponse response = service.validar(request);
        return response.success()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
}
