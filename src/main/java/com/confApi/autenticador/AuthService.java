package com.confApi.autenticador;
/** The old unscoped login+code interface is retired. Use /api/plantao/token. */
@org.springframework.stereotype.Service public class AuthService {
 public AuthResponse validate(AuthRequest request) throws com.fasterxml.jackson.core.JsonProcessingException {throw new com.confApi.authv2.V2.Failure(410);}
 public String generate() throws com.fasterxml.jackson.core.JsonProcessingException {throw new com.confApi.authv2.V2.Failure(410);}
}
