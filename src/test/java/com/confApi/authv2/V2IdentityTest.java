package com.confApi.authv2;
import org.junit.jupiter.api.*;import com.nimbusds.jose.*;import com.nimbusds.jose.crypto.*;import com.nimbusds.jose.jwk.*;import com.nimbusds.jose.jwk.gen.*;import com.nimbusds.jwt.*;import java.time.Instant;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
class V2IdentityTest {
 RSAKey key;
 @BeforeEach void setup()throws Exception{key=new RSAKeyGenerator(2048).keyID("test").generate();System.setProperty("AUTH_V2_ISSUER","synthetic-issuer");System.setProperty("AUTH_V2_JWKS_JSON",new JWKSet(key.toPublicJWK()).toString());}
 @AfterEach void cleanup(){System.clearProperty("AUTH_V2_ISSUER");System.clearProperty("AUTH_V2_JWKS_JSON");}
 String token(String audience,String use,String scope,long expiry)throws Exception{var c=new JWTClaimsSet.Builder().subject("payara").issuer("synthetic-issuer").audience(audience).issueTime(new Date()).expirationTime(Date.from(Instant.now().plusSeconds(expiry))).claim("token_use",use).claim("scope",scope).build();var j=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test").type(new JOSEObjectType("at+jwt")).build(),c);j.sign(new RSASSASigner(key));return "Bearer "+j.serialize();}
 @Test void serviceIdentityRequiresOwnAudiencePurposeAndScope()throws Exception{assertEquals("payara",V2.verify(token("manager-v2","SERVICE","v2:access portal:primary",120),"manager-v2","portal:primary").id());assertThrows(V2.Failure.class,()->V2.verify(token("wrong","SERVICE","portal:primary",120),"manager-v2","portal:primary"));assertThrows(V2.Failure.class,()->V2.verify(token("manager-v2","TRACKING","portal:primary",120),"manager-v2","portal:primary"));assertThrows(V2.Failure.class,()->V2.verify(token("manager-v2","SERVICE","other",120),"manager-v2","portal:primary"));}
 @Test void expiredLongLivedAndTechnicalCredentialsAreRejected(){assertThrows(V2.Failure.class,()->V2.verify(token("manager-v2","SERVICE","v2:access",-1),"manager-v2","v2:access"));assertThrows(V2.Failure.class,()->V2.verify(token("manager-v2","SERVICE","v2:access",3600),"manager-v2","v2:access"));assertThrows(V2.Failure.class,()->V2.verify("Bearer old-technical-token","manager-v2","v2:access"));}

 String custom(String issuer,String subject,Instant issued,Instant expires)throws Exception{
  var claims=new JWTClaimsSet.Builder().issuer(issuer).subject(subject).audience("manager-v2").issueTime(Date.from(issued)).expirationTime(Date.from(expires)).claim("token_use","SERVICE").claim("scope","v2:access").build();
  var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test").type(new JOSEObjectType("at+jwt")).build(),claims);jwt.sign(new RSASSASigner(key));return "Bearer "+jwt.serialize();
 }
 @Test void missingIssuerConfigurationCannotAcceptMatchingEmptyClaim()throws Exception{
  System.setProperty("AUTH_V2_ISSUER","");String bearer=custom("","payara",Instant.now(),Instant.now().plusSeconds(120));
  assertEquals(401,assertThrows(V2.Failure.class,()->V2.verify(bearer,"manager-v2","v2:access")).status);
 }
 @Test void emptySubjectIsNotAnAuthenticatedIdentity()throws Exception{
  for(String subject:List.of(""," ")){String bearer=custom("synthetic-issuer",subject,Instant.now(),Instant.now().plusSeconds(120));assertEquals(401,assertThrows(V2.Failure.class,()->V2.verify(bearer,"manager-v2","v2:access")).status);}
 }
 @Test void expiryMustBeAfterIssuanceEvenInsideClockTolerance()throws Exception{
  Instant future=Instant.now().plusSeconds(4);String bearer=custom("synthetic-issuer","payara",future,future);
  assertEquals(401,assertThrows(V2.Failure.class,()->V2.verify(bearer,"manager-v2","v2:access")).status);
 }

}
