package com.confApi.autenticador;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final
    AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/validate")
    public ResponseEntity<AuthResponse> authenticate(@RequestBody AuthRequest request) throws JsonProcessingException {
        AuthResponse response = authService.validate(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/token")
    public ResponseEntity<String> generate() throws JsonProcessingException {
        String response = authService.generate();
        return ResponseEntity.ok(response);
    }
}
