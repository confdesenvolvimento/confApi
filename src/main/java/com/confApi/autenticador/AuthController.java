package com.confApi.autenticador;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final
    AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/whatsapp")
    public ResponseEntity<AuthResponse> authenticate(@RequestBody AuthRequest request) throws JsonProcessingException {
        AuthResponse response = authService.validate(request);
        return ResponseEntity.ok(response);
    }
}
