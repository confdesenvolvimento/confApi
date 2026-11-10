package com.confApi.authv2;
import com.fasterxml.jackson.databind.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jwt.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.security.*;
public final class V2 {
 public static final ObjectMapper JSON=new ObjectMapper().findAndRegisterModules().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
 private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
 public static class Failure extends RuntimeException { public final int status; public Failure(int status){super("AUTH_V2_"+status);this.status=status;} }
 public record Actor(String id,Set<String> scopes,JWTClaimsSet claims) { public void require(String scope){if(!scopes.contains(scope))throw new Failure(403);} }
 public static String setting(String name){return System.getProperty(name,System.getenv().getOrDefault(name,""));}
 public static boolean enabled(){return "true".equalsIgnoreCase(setting("AUTH_V2_ENABLED"));}
 public static String random(){byte[] b=new byte[32];new SecureRandom().nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
 public static String hash(String value){try{return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 public static Actor verify(String authorization,String audience,String scope){
  try {
   if(authorization==null||!authorization.startsWith("Bearer ")||authorization.length()>8192)throw new Failure(401);
   var jwt=SignedJWT.parse(authorization.substring(7));var h=jwt.getHeader();var c=jwt.getJWTClaimsSet();Instant now=Instant.now();
   var key=JWKSet.parse(setting("AUTH_V2_JWKS_JSON")).getKeyByKeyId(h.getKeyID());
   if(setting("AUTH_V2_ISSUER").isBlank()||h.getKeyID()==null||!(key instanceof RSAKey rsa)||rsa.isPrivate()||rsa.size()<2048||!JWSAlgorithm.RS256.equals(h.getAlgorithm())||!new JOSEObjectType("at+jwt").equals(h.getType())||h.getCriticalParams()!=null||!jwt.verify(new RSASSAVerifier(rsa))
    ||!setting("AUTH_V2_ISSUER").equals(c.getIssuer())||!List.of(audience).equals(c.getAudience())||c.getExpirationTime()==null||!c.getExpirationTime().toInstant().isAfter(now)||c.getIssueTime()==null||!c.getExpirationTime().after(c.getIssueTime())||c.getIssueTime().toInstant().isAfter(now.plusSeconds(5))||c.getExpirationTime().toInstant().isAfter(c.getIssueTime().toInstant().plusSeconds(300))||(c.getNotBeforeTime()!=null&&c.getNotBeforeTime().toInstant().isAfter(now))||c.getSubject()==null||c.getSubject().isBlank()||c.getSubject().length()>128)throw new Failure(401);
   String use=c.getStringClaim("token_use");if(!Set.of("SERVICE","OPERATOR").contains(use))throw new Failure(401);
   var actor=new Actor(c.getSubject(),new HashSet<>(Arrays.asList(Optional.ofNullable(c.getStringClaim("scope")).orElse("").split(" "))),c);actor.require(scope);return actor;
  }catch(Failure e){throw e;}catch(Exception e){throw new Failure(401);}
 }
 public static void operator(Actor actor){try{if(!"OPERATOR".equals(actor.claims().getStringClaim("token_use"))||!actor.claims().getStringListClaim("amr").contains("mfa")||actor.claims().getLongClaim("auth_time")<Instant.now().minusSeconds(300).getEpochSecond()||actor.claims().getLongClaim("auth_time")>Instant.now().plusSeconds(5).getEpochSecond())throw new Failure(403);}catch(Exception e){throw new Failure(403);}}
 public static String token(String fileSetting){try{String token=Files.readString(Path.of(setting(fileSetting))).trim();if(token.isBlank()||token.contains("\n")||token.contains("\r"))throw new Failure(503);return "Bearer "+token;}catch(Exception e){throw new Failure(503);}}
 public static JsonNode request(String base,String route,String method,Object body,String authorization,String idempotency){
  try {URI uri=URI.create(base+route);if(!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null)throw new Failure(503);
   var b=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(8)).header("Accept","application/json").header("Authorization",authorization).header("Content-Type","application/json");
   if(idempotency!=null)b.header("Idempotency-Key",idempotency);
   b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
   var response=HTTP.send(b.build(),HttpResponse.BodyHandlers.ofString());if(response.statusCode()<200||response.statusCode()>=300)throw new Failure(Set.of(400,401,403,409,429).contains(response.statusCode())?response.statusCode():503);
   return response.body().isBlank()?JSON.createObjectNode():JSON.readTree(response.body());
  }catch(Failure e){throw e;}catch(InterruptedException e){Thread.currentThread().interrupt();throw new Failure(503);}catch(Exception e){throw new Failure(503);}
 }
 public static String text(JsonNode node,String field){String value=node.path(field).asText("");if(value.isBlank()||value.length()>2048)throw new Failure(400);return value;}
 private V2(){}
}
