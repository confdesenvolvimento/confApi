package com.confApi.security.jwt;
import io.jsonwebtoken.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
class JwtRevocationTest {
 static final String KEY="synthetic-test-key-with-at-least-sixty-four-characters-1234567890-abcd";
 static class Service extends JwtService {
  boolean allowed;int checks;boolean outage;
  @Override protected boolean validarIdentidadeIndividual(String token,Object userId){checks++;assertEquals(1,((Number)userId).intValue());if(outage)throw new IllegalStateException("synthetic outage");return allowed;}
 }
 Service service;
 @BeforeEach void setup(){service=new Service();ReflectionTestUtils.setField(service,"chaveAssinatura",KEY);System.setProperty("AUTH_V2_ENABLED","false");}
 @AfterEach void cleanup(){System.clearProperty("AUTH_V2_ENABLED");}
 String token(boolean individual){var b=Jwts.builder().setSubject("alice").setExpiration(new Date(System.currentTimeMillis()+60000));if(individual)b.claim("tipoToken","USUARIO_MANAGER").claim("codigoUsuario",1).claim("mfa_v2",true).claim("device_version",1);return b.signWith(SignatureAlgorithm.HS512,KEY.getBytes(java.nio.charset.StandardCharsets.UTF_8)).compact();}
 @Test void revokedIndividualCannotBypassManagerWhenPilotIsOff(){assertFalse(service.tokenValido(token(true)));assertEquals(1,service.checks);service.allowed=true;assertTrue(service.tokenValido(token(true)));assertEquals(2,service.checks);}
 @Test void outageDoesNotFallBackToSignatureOnly(){service.outage=true;assertFalse(service.tokenValido(token(true)));assertEquals(1,service.checks);}
 @Test void existingTechnicalContractDoesNotBecomeIndividualIdentity(){assertTrue(service.tokenValido(token(false)));assertEquals(0,service.checks);}
}
