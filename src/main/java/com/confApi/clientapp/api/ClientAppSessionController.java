package com.confApi.clientapp.api;

import com.confApi.clientapp.security.ClientAppAuthenticationToken;
import com.confApi.clientapp.security.ClientAppPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client-app/v1/auth")
public class ClientAppSessionController {

    @GetMapping("/session")
    public ResponseEntity<ClientAppSessionResponse> session(Authentication authentication) {
        if (!(authentication instanceof ClientAppAuthenticationToken)) {
            throw new IllegalStateException("Client application principal is required");
        }

        ClientAppPrincipal principal = ((ClientAppAuthenticationToken) authentication).getPrincipal();
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl("no-store, no-cache, max-age=0, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);
        headers.set(HttpHeaders.VARY, HttpHeaders.AUTHORIZATION);
        return ResponseEntity.ok()
                .headers(headers)
                .body(ClientAppSessionResponse.from(principal));
    }
}
