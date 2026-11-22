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
    private final LegacyPlantaoTokenService legacy;

    public PlantaoTokenController(PlantaoTokenService service, LegacyPlantaoTokenService legacy) {
        this.service = service;
        this.legacy = legacy;
    }

    @PostMapping("/gerar")
    public ResponseEntity<PlantaoTokenResponse> gerar(
            @RequestBody PlantaoTokenRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        PlantaoTokenResponse response = com.confApi.authv2.V2.enabled()
                ? service.gerar(request, authorization) : legacy.gerar(request);
        return response.success()
                ? ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(response)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).cacheControl(org.springframework.http.CacheControl.noStore()).body(response);
    }

    @PostMapping("/validar")
    public ResponseEntity<PlantaoTokenResponse> validar(@RequestBody PlantaoTokenValidationRequest request) {
        PlantaoTokenResponse response = com.confApi.authv2.V2.enabled()
                ? service.validar(request) : legacy.validar(request);
        return response.success()
                ? ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(response)
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).cacheControl(org.springframework.http.CacheControl.noStore()).body(response);
    }
}
