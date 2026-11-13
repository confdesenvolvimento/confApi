package com.confApi.mobile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Sessões opacas individuais. Reiniciar esta instância encerra suas sessões. */
@Component
public class MobileSessions {
    // Mantém o prazo de login existente no app; cada token de acesso continua curto e revogável.
    static final Duration ACCESS=Duration.ofMinutes(15),TOTAL=Duration.ofDays(20);
    private final MobileUpstream upstream;
    private final SecureRandom random=new SecureRandom();
    private final Map<String,Session> access=new ConcurrentHashMap<>(),refresh=new ConcurrentHashMap<>();
    private final Map<String,Session> spentRefresh=new ConcurrentHashMap<>();
    private final Map<String,Window> limits=new ConcurrentHashMap<>();
    private final byte[] otpKey=new byte[32];
    MobileSessions(MobileUpstream upstream){this.upstream=upstream;random.nextBytes(otpKey);}
    static final class Session {
        final String login,device;final Instant until;
        volatile JsonNode profile; volatile boolean primary,contact,revoked;String accessHash,refreshHash,otpHash;
        Instant accessUntil,otpUntil=Instant.EPOCH,nextSend=Instant.EPOCH;int attempts;
        Session(String login,String device,JsonNode profile,boolean primary){this.login=login;this.device=device;this.profile=profile;this.primary=primary;until=Instant.now().plus(TOTAL);}
    }
    private record Window(int count,Instant until) {}
    synchronized void limit(String key,int max,int seconds) {
        Instant now=Instant.now();
        limits.entrySet().removeIf(e->!e.getValue().until.isAfter(now));
        if(limits.size()>10000)throw new MobileFailure(429,"AGUARDE_PARA_TENTAR_NOVAMENTE");
        Window previous=limits.get(key);int count=previous==null?1:previous.count+1;
        limits.put(key,new Window(count,previous==null?now.plusSeconds(seconds):previous.until));
        if(count>max)throw new MobileFailure(429,"AGUARDE_PARA_TENTAR_NOVAMENTE");
    }
    Map<String,Object> start(String login,String password,String device,String peer) {
        if(login==null||!login.matches("[\\p{L}0-9._@-]{1,100}")||device==null||!device.matches("[a-fA-F0-9-]{36}"))throw new MobileFailure(400,"DADOS_INVALIDOS");
        limit("start-ip:"+peer,30,300);limit("start-user:"+login.toLowerCase(Locale.ROOT),10,300);
        access.entrySet().removeIf(e->!e.getValue().until.isAfter(Instant.now()));refresh.entrySet().removeIf(e->!e.getValue().until.isAfter(Instant.now()));
        if(refresh.size()>=10000)throw new MobileFailure(503,"SESSOES_INDISPONIVEIS");
        boolean primary=password!=null;
        if(primary&&(password.isEmpty()||password.length()>1024))throw new MobileFailure(401,"ACESSO_INVALIDO");
        JsonNode profile=primary?upstream.call("manager","usuario/auth","POST",MobileUpstream.JSON.valueToTree(Map.of("loginUsuario",login,"senha",password))):upstream.call("manager","wooba/turUsuarios/"+login,"POST",null);
        if(profile==null||!profile.isObject()||profile.isEmpty()||profile.path("mensagemDeErro").path("idMensagem").asInt(200)!=200)throw new MobileFailure(401,"ACESSO_INVALIDO");
        if(primary&&profile.path("id").asInt(0)<=0)throw new MobileFailure(401,"ACESSO_INVALIDO");
        var safe=(ObjectNode)MobileUpstream.sanitized(profile);safe.put("login",login);
        return issue(new Session(login,device,safe,primary));
    }
    private synchronized Map<String,Object> issue(Session session) {
        if(session.accessHash!=null)access.remove(session.accessHash);
        if(session.refreshHash!=null)refresh.remove(session.refreshHash);
        String token=randomToken(),renewal=randomToken();
        session.accessHash=hash(token);session.refreshHash=hash(renewal);session.accessUntil=Instant.now().plus(ACCESS);
        access.put(session.accessHash,session);refresh.put(session.refreshHash,session);
        return Map.of("accessToken",token,"refreshToken",renewal,"expiresAt",session.accessUntil.toString(),"profile",profile(session));
    }
    Session require(String authorization) {
        if(authorization==null||!authorization.matches("Bearer [A-Za-z0-9_-]{43}"))throw new MobileFailure(401,"SESSAO_INVALIDA");
        Session session=access.get(hash(authorization.substring(7)));
        if(session==null||session.revoked||!session.accessUntil.isAfter(Instant.now())||!session.until.isAfter(Instant.now()))throw new MobileFailure(401,"SESSAO_EXPIRADA");
        return session;
    }
    synchronized Map<String,Object> renew(String token) {
        if(token==null||!token.matches("[A-Za-z0-9_-]{43}"))throw new MobileFailure(401,"SESSAO_INVALIDA");
        String fingerprint=hash(token);
        spentRefresh.entrySet().removeIf(e->!e.getValue().until.isAfter(Instant.now()));
        Session reused=spentRefresh.get(fingerprint);
        if(reused!=null){close(reused);throw new MobileFailure(401,"SESSAO_INVALIDA");}
        Session session=refresh.remove(fingerprint);
        if(session!=null)spentRefresh.put(fingerprint,session);
        if(session==null||session.revoked||!session.until.isAfter(Instant.now()))throw new MobileFailure(401,"SESSAO_EXPIRADA");
        return issue(session);
    }
    synchronized void close(Session session){session.revoked=true;access.remove(session.accessHash);refresh.remove(session.refreshHash);session.otpHash=null;}
    JsonNode profile(Session session) {
        if(session.primary)return session.profile.deepCopy();
        var result=MobileUpstream.JSON.createObjectNode();result.put("login",session.login);
        result.put("senhaTemporaria",session.profile.path("senhaTemporaria").asInt());
        String phone=session.profile.path("celular").asText("");result.put("celular",phone.length()<4?"":phone.substring(phone.length()-4));
        // Somente a indicação de conta encontrada é necessária na tela existente.
        result.put("email",session.profile.path("email").asText("").isBlank()?"":"cadastrado");
        return result;
    }
    synchronized Map<String,Object> sendCode(Session session,String channel) {
        return sendCode(session,channel,false);
    }
    synchronized Map<String,Object> sendCode(Session session,String channel,boolean copyEmail) {
        limit("otp-user:"+session.login,5,900);
        if(session.nextSend.isAfter(Instant.now()))throw new MobileFailure(429,"AGUARDE_PARA_REENVIAR");
        session.nextSend=Instant.now().plusSeconds(60);
        String code=String.format(Locale.ROOT,"%06d",random.nextInt(1_000_000));
        if(copyEmail&&"sms".equals(channel))upstream.deliverLoginCode(session.profile,code);
        else upstream.deliver(session.profile,channel,code);
        session.otpHash=otpHash(session,code);session.otpUntil=Instant.now().plusSeconds(300);session.attempts=0;
        return Map.of("expiresIn",300,"retryAfter",60);
    }
    synchronized JsonNode validateCode(Session session,String code) {
        if(session.otpHash==null||!session.otpUntil.isAfter(Instant.now())||++session.attempts>5||code==null||!code.matches("[0-9]{6}")||!MessageDigest.isEqual(session.otpHash.getBytes(StandardCharsets.UTF_8),otpHash(session,code).getBytes(StandardCharsets.UTF_8)))throw new MobileFailure(401,"CODIGO_INVALIDO");
        session.otpHash=null;
        JsonNode account=upstream.call("manager","usuario/findByLogin/"+session.login,"GET",null);
        if(account.path("status").asInt()!=1)throw new MobileFailure(403,"USUARIO_INATIVO");
        var profile=(ObjectNode)MobileUpstream.sanitized(account);
        if(session.primary)profile.setAll((ObjectNode)session.profile);
        profile.put("id",account.path("codgUsuario").asInt());profile.put("login",session.login);profile.put("nome",account.path("nomeCompleto").asText());profile.put("codigo",account.path("codigoWooba").asInt());profile.put("tokenAcesso",account.path("idToken").asText());
        if(session.profile.has("tokenIdentificacao"))profile.set("tokenAcesso",session.profile.get("tokenIdentificacao"));
        if(session.profile.has("senhaTemporaria"))profile.set("senhaTemporaria",session.profile.get("senhaTemporaria"));
        session.profile=profile;session.primary=true;session.contact=true;return profile(session);
    }
    JsonNode device(Session session) {
        if(!session.primary) {
            JsonNode registered=upstream.call("manager","dispositivo/login/"+session.login,"GET",null);
            // Mantém a indicação do canal existente, sem expor o cadastro nem autorizar a sessão limitada.
            return MobileUpstream.JSON.valueToTree(Map.of("autorizado",false,"situacao","NAO_CADASTRADO","enviarEmail",registered.path("enviarEmail").asInt()));
        }
        return upstream.call("manager","dispositivo/login/"+session.login+"?codigoDispositivo="+session.device,"GET",null);
    }
    void requireActive(Session session) {
        if(!session.primary)throw new MobileFailure(403,"AUTENTICACAO_NECESSARIA");
        JsonNode device=device(session);
        if(!device.path("autorizado").asBoolean(false)||!session.device.equals(device.path("codigoDispositivo").asText()))throw new MobileFailure(403,"DISPOSITIVO_NAO_AUTORIZADO");
    }
    JsonNode register(Session session,JsonNode input) {
        if(!session.primary)throw new MobileFailure(403,"AUTENTICACAO_NECESSARIA");
        if(!session.contact&&!device(session).path("autorizado").asBoolean())throw new MobileFailure(403,"VALIDACAO_SMS_NECESSARIA");
        var body=MobileUpstream.JSON.createObjectNode();body.put("login",session.login);body.put("codigoDispositivo",session.device);
        for(String field:List.of("modeloDispositivo","marcaDispositivo","metadados","tokenExpo"))if(input.has(field))body.set(field,input.get(field));
        return upstream.call("manager","dispositivo/novo","POST",body);
    }
    private String randomToken(){byte[] bytes=new byte[32];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private String otpHash(Session session,String code){try{var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec(otpKey,"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal((session.login+":"+session.device+":"+code).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
