package com.confApi.mobile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Sessões opacas; sessões DPoP aprovadas são persistidas nas tabelas existentes. */
@Component
public class MobileSessions {
    // Mantém o prazo de login existente no app; cada token de acesso continua curto e revogável.
    static final Duration ACCESS=Duration.ofMinutes(15),TOTAL=Duration.ofDays(20);
    static final Duration PENDING=Duration.ofMinutes(30);
    static final int MAX_ACTIVE_SESSIONS=10000;
    // Um usuario pode ter contadores distintos de config, IP, login, renovacao, OTP e operacoes.
    static final int MAX_RATE_LIMIT_KEYS=MAX_ACTIVE_SESSIONS*10;
    private final MobileUpstream upstream;
    private final MobileSessionRepository repository;
    private final MobileProofs proofs;
    private final SecureRandom random=new SecureRandom();
    private final Map<String,Session> access=new ConcurrentHashMap<>(),refresh=new ConcurrentHashMap<>();
    private final Map<String,Session> spentRefresh=new ConcurrentHashMap<>();
    private final Map<String,Window> limits=new ConcurrentHashMap<>();
    private final AtomicLong nextCleanup=new AtomicLong();
    private final byte[] otpKey=new byte[32];
    MobileSessions(MobileUpstream upstream){this(upstream,null,null);}
    @org.springframework.beans.factory.annotation.Autowired
    MobileSessions(MobileUpstream upstream,MobileSessionRepository repository,MobileProofs proofs){this.upstream=upstream;this.repository=repository;this.proofs=proofs;random.nextBytes(otpKey);}
    static final class Session {
        final String login,device;volatile Instant until;
        volatile JsonNode profile; volatile boolean primary,contact,revoked;String accessHash,refreshHash,otpHash,contactResetId="";
        Instant accessUntil,otpUntil=Instant.EPOCH,nextSend=Instant.EPOCH;int attempts;
        String id=UUID.randomUUID().toString(),publicJwk,thumbprint,nonce,userId,companyId;
        long deviceVersion;Instant createdAt=Instant.now();volatile boolean persisted;
        final Object persistenceLock=new Object();
        String retryRefreshHash,retryReceiptHash;Instant retryUntil=Instant.EPOCH;
        Session(String login,String device,JsonNode profile,boolean primary){this.login=login;this.device=device;this.profile=profile;this.primary=primary;until=Instant.now().plus(primary?TOTAL:PENDING);}
    }
    private record Window(int count,Instant until) {}
    private void cleanup(Instant now) {
        long current=now.toEpochMilli(),next=nextCleanup.get();
        if(current<next||!nextCleanup.compareAndSet(next,current+30_000))return;
        // Uma varredura periodica, sem bloquear outras requisicoes nem repetir a cada acesso.
        limits.entrySet().removeIf(e->!e.getValue().until.isAfter(now));
        removeExpired(access,now);removeExpired(refresh,now);removeExpired(spentRefresh,now);
    }
    private void removeExpired(Map<String,Session> entries,Instant now) {
        entries.forEach((key,session)->{
            if(session.revoked||!session.until.isAfter(now))synchronized(this) {
                // A promocao de identificacao para sessao autenticada pode estender o prazo.
                if(session.revoked||!session.until.isAfter(now))entries.remove(key,session);
            }
        });
    }
    void limit(String key,int max,int seconds) {
        Instant now=Instant.now();
        cleanup(now);
        if(limits.size()>=MAX_RATE_LIMIT_KEYS&&!limits.containsKey(key))throw new MobileFailure(429,"AGUARDE_PARA_TENTAR_NOVAMENTE");
        Window current=limits.compute(key,(ignored,previous)->previous==null||!previous.until.isAfter(now)
                ?new Window(1,now.plusSeconds(seconds))
                :new Window(Math.min(max+1,previous.count+1),previous.until));
        if(current.count>max)throw new MobileFailure(429,"AGUARDE_PARA_TENTAR_NOVAMENTE");
    }
    Map<String,Object> start(String login,String password,String device,String peer) {
        return start(login,password,device,peer,null);
    }
    Map<String,Object> start(String login,String password,String device,String peer,JsonNode publicJwk) {
        if(login==null||!login.matches("[\\p{L}0-9._@-]{1,100}")||device==null||!device.matches("[a-fA-F0-9-]{36}"))throw new MobileFailure(400,"DADOS_INVALIDOS");
        limit("start-ip:"+peer,30,300);limit("start-user:"+login.toLowerCase(Locale.ROOT),10,300);
        if(refresh.size()>=MAX_ACTIVE_SESSIONS)throw new MobileFailure(503,"SESSOES_INDISPONIVEIS");
        MobileProofs.PublicKeyData key=null;
        if(publicJwk!=null&&!publicJwk.isNull()) {
            if(proofs==null)throw new MobileFailure(503,"PERSISTENCIA_INDISPONIVEL");
            key=proofs.validatedKey(publicJwk.toString());
        }
        boolean primary=password!=null;
        if(primary&&(password.isEmpty()||password.length()>1024))throw new MobileFailure(401,"ACESSO_INVALIDO");
        JsonNode profile=primary?upstream.call("manager","usuario/auth","POST",MobileUpstream.JSON.valueToTree(Map.of("loginUsuario",login,"senha",password))):upstream.call("manager","wooba/turUsuarios/"+login,"POST",null);
        if(profile==null||!profile.isObject()||profile.isEmpty()||profile.path("mensagemDeErro").path("idMensagem").asInt(200)!=200)throw new MobileFailure(401,"ACESSO_INVALIDO");
        if(primary&&profile.path("id").asInt(0)<=0)throw new MobileFailure(401,"ACESSO_INVALIDO");
        var safe=(ObjectNode)MobileUpstream.sanitized(profile);safe.put("login",login);
        Session session=new Session(login,device,safe,primary);
        if(key!=null){session.publicJwk=key.publicJwk();session.thumbprint=key.thumbprint();session.nonce=randomToken();}
        return issue(session);
    }
    private synchronized Map<String,Object> issue(Session session) {return issue(session,null);}
    private synchronized Map<String,Object> issue(Session session,MobileRenewals.Tokens replacement) {
        if(session.refreshHash==null&&refresh.size()>=MAX_ACTIVE_SESSIONS)throw new MobileFailure(503,"SESSOES_INDISPONIVEIS");
        if(session.accessHash!=null)access.remove(session.accessHash);
        if(session.refreshHash!=null)refresh.remove(session.refreshHash);
        String token=replacement==null?randomToken():replacement.access(),renewal=replacement==null?randomToken():replacement.refresh();
        session.accessHash=hash(token);session.refreshHash=hash(renewal);session.accessUntil=Instant.now().plus(ACCESS);
        if(session.accessUntil.isAfter(session.until))session.accessUntil=session.until;
        access.put(session.accessHash,session);refresh.put(session.refreshHash,session);
        return tokens(session,token,renewal);
    }
    Session require(String authorization) {
        return require(authorization,null,null,null);
    }
    Session require(String authorization,String proof,String method,String uri) {
        if(authorization==null||!authorization.matches("Bearer [A-Za-z0-9_-]{43}"))throw new MobileFailure(401,"SESSAO_INVALIDA");
        String token=authorization.substring(7),fingerprint=hash(token);
        Session session=access.get(fingerprint);
        MobileSessionRepository.DurableSession row=null;
        if(repository!=null&&(session==null||session.persisted)) {
            row=repository.loadAccess(fingerprint).orElseThrow(()->new MobileFailure(401,"SESSAO_EXPIRADA"));
            if(!row.expiresAt().isAfter(Instant.now())||!row.absoluteExpiresAt().isAfter(Instant.now()))throw new MobileFailure(401,"SESSAO_EXPIRADA");
            // Validate possession before loading a profile or changing any authorization state.
            proofs.verify(row,token,proof,method,uri);
            session=restore(row,session);
        }
        if(session==null||session.revoked||!session.accessUntil.isAfter(Instant.now())||!session.until.isAfter(Instant.now()))throw new MobileFailure(401,"SESSAO_EXPIRADA");
        if(row==null&&session.publicJwk!=null)proofs.verify(snapshot(session),token,proof,method,uri);
        return session;
    }
    Map<String,Object> renew(String token) {return renew(token,null,null,null);}
    Map<String,Object> renew(String token,String proof,String method,String uri) {return renew(token,proof,method,uri,null);}
    Map<String,Object> renew(String token,String proof,String method,String uri,String renewalId) {
        MobileRenewals.validateId(renewalId);
        if(token==null||!token.matches("[A-Za-z0-9_-]{43}"))throw new MobileFailure(401,"SESSAO_INVALIDA");
        String fingerprint=hash(token);
        Session known=refresh.get(fingerprint);
        if(known==null)known=spentRefresh.get(fingerprint);
        if(known!=null)synchronized(known.persistenceLock) {
            // A promocao e a renovacao da mesma sessao usam a mesma trava, sem bloquear outros usuarios.
            if(known.persisted)return renewPersistent(token,fingerprint,known,proof,method,uri,renewalId);
            if(known.publicJwk!=null)proofs.verify(snapshot(known),token,proof,method,uri);
            // Bearer-only clients retain strict replay detection; recovery requires possession of the device key.
            var replacement=known.publicJwk==null?null:MobileRenewals.derive(token,known.id,renewalId);
            synchronized(this) {
                Session reused=spentRefresh.get(fingerprint);
                if(reused!=null){
                    if(replacement!=null&&!reused.revoked&&reused.until.isAfter(Instant.now())&&reused.retryUntil.isAfter(Instant.now())
                        &&fingerprint.equals(reused.retryRefreshHash)&&replacement.receipt().equals(reused.retryReceiptHash)
                        &&hash(replacement.access()).equals(reused.accessHash)&&hash(replacement.refresh()).equals(reused.refreshHash))
                        return tokens(reused,replacement.access(),replacement.refresh());
                    revokeMemory(reused);throw new MobileFailure(401,"SESSAO_INVALIDA");
                }
                Session session=refresh.remove(fingerprint);
                if(session!=null)spentRefresh.put(fingerprint,session);
                if(session==null||session.revoked||!session.until.isAfter(Instant.now()))throw new MobileFailure(401,"SESSAO_EXPIRADA");
                var result=issue(session,replacement);
                if(replacement!=null){session.retryRefreshHash=fingerprint;session.retryReceiptHash=replacement.receipt();session.retryUntil=Instant.now().plus(MobileRenewals.RECOVERY);}
                return result;
            }
        }
        if(repository!=null)return renewPersistent(token,fingerprint,null,proof,method,uri,renewalId);
        throw new MobileFailure(401,"SESSAO_EXPIRADA");
    }
    private Map<String,Object> renewPersistent(String token,String fingerprint,Session known,String proof,String method,String uri,String renewalId) {
        var stored=repository.loadRefresh(fingerprint).orElseThrow(()->new MobileFailure(401,"SESSAO_EXPIRADA"));
        var row=stored.session();proofs.verify(row,token,proof,method,uri);
        if(stored.used()&&renewalId==null){repository.close(row.id());throw new MobileFailure(401,"SESSAO_INVALIDA");}
        Session session=restore(row,known);
        validateDevice(session,registeredDevice(session),true);
        var replacement=MobileRenewals.derive(token,row.id(),renewalId);
        String accessToken=replacement==null?randomToken():replacement.access(),refreshToken=replacement==null?randomToken():replacement.refresh();
        Instant expiry=Instant.now().plus(ACCESS);if(expiry.isAfter(row.absoluteExpiresAt()))expiry=row.absoluteExpiresAt();
        var rotated=repository.rotate(row.id(),fingerprint,hash(accessToken),hash(refreshToken),expiry,Instant.now(),replacement==null?null:replacement.receipt());
        synchronized(session.persistenceLock) {
            access.remove(session.accessHash,session);if(session.refreshHash!=null)refresh.remove(session.refreshHash,session);
            session.accessHash=rotated.accessHash();session.refreshHash=hash(refreshToken);session.accessUntil=rotated.expiresAt();
            access.put(session.accessHash,session);refresh.put(session.refreshHash,session);
            return tokens(session,accessToken,refreshToken);
        }
    }
    void close(Session session){
        synchronized(session.persistenceLock) {
            if(session.persisted)repository.close(session.id);
            revokeMemory(session);
        }
    }
    private synchronized void revokeMemory(Session session){session.revoked=true;access.remove(session.accessHash);if(session.refreshHash!=null)refresh.remove(session.refreshHash);session.otpHash=null;}
    private Map<String,Object> tokens(Session session,String token,String renewal) {
        var result=new LinkedHashMap<String,Object>();result.put("accessToken",token);result.put("refreshToken",renewal);
        result.put("expiresAt",session.accessUntil.toString());result.put("profile",profile(session));
        if(session.publicJwk!=null){result.put("nonce",session.nonce);result.put("tokenType","DPoP");}
        return result;
    }
    private MobileSessionRepository.DurableSession snapshot(Session session) {
        return new MobileSessionRepository.DurableSession(session.id,session.userId,session.companyId,session.publicJwk,session.thumbprint,
                session.device,session.deviceVersion,"CONFAPI_ATIVA",session.accessHash,session.nonce,session.accessUntil,session.until,session.createdAt);
    }
    private Session restore(MobileSessionRepository.DurableSession row,Session cached) {
        if(!row.absoluteExpiresAt().isAfter(Instant.now()))throw new MobileFailure(401,"SESSAO_EXPIRADA");
        var identity=repository.loadIdentity(row.userId());
        if(identity.status()!=1||identity.deviceVersion()!=row.deviceVersion()||!identity.companyId().equals(row.companyId())) {
            repository.close(row.id());throw new MobileFailure(401,"SESSAO_REVOGADA");
        }
        if(cached!=null&&cached.id.equals(row.id())&&cached.login.equalsIgnoreCase(identity.login())) {
            synchronized(cached.persistenceLock){cached.accessHash=row.accessHash();cached.accessUntil=row.expiresAt();cached.until=row.absoluteExpiresAt();}
            return cached;
        }
        JsonNode account=upstream.call("manager","usuario/findByIdUsuario/"+row.userId(),"GET",null);
        if(!row.userId().equals(account.path("codgUsuario").asText())||account.path("status").asInt()!=1||!identity.login().equals(account.path("loginUsuario").asText()))
            throw new MobileFailure(401,"SESSAO_REVOGADA");
        var safe=(ObjectNode)MobileUpstream.sanitized(account);
        safe.put("id",account.path("codgUsuario").asInt());safe.put("nome",account.path("nomeCompleto").asText());
        safe.put("login",identity.login());safe.put("codigo",account.path("codigoWooba").asInt());safe.put("tokenAcesso",account.path("idToken").asText());
        safe.put("usuarioBackOffice",account.path("codgSistemaBackOffice").asText());
        // A identificacao Wooba e a fonte usada pelo login por OTP; o cadastro Manager pode estar desatualizado.
        JsonNode identification=upstream.call("manager","wooba/turUsuarios/"+identity.login(),"POST",null);
        if(identification==null||!identification.isObject()||identification.isEmpty()||identification.path("mensagemDeErro").path("idMensagem").asInt(200)!=200)
            throw new MobileFailure(503,"PERFIL_INDISPONIVEL");
        if(!identification.path("tokenIdentificacao").asText("").isBlank())safe.put("tokenAcesso",identification.path("tokenIdentificacao").asText());
        if(identification.has("senhaTemporaria"))safe.set("senhaTemporaria",identification.get("senhaTemporaria"));
        Session session=new Session(identity.login(),row.deviceId(),safe,true);
        session.id=row.id();session.userId=row.userId();session.companyId=row.companyId();session.deviceVersion=row.deviceVersion();
        session.publicJwk=row.publicJwk();session.thumbprint=row.thumbprint();session.nonce=row.nonce();
        session.accessHash=row.accessHash();session.accessUntil=row.expiresAt();session.until=row.absoluteExpiresAt();session.createdAt=row.createdAt();session.persisted=true;
        // Contact verification is deliberately not reconstructed: a later registration needs a new OTP.
        if(access.size()<MAX_ACTIVE_SESSIONS)access.put(session.accessHash,session);
        return session;
    }
    private void persist(Session session,JsonNode registered) {
        if(session.publicJwk==null||session.persisted||!registered.path("autorizado").asBoolean()||!session.device.equals(registered.path("codigoDispositivo").asText()))return;
        synchronized(session.persistenceLock) {
            requireOpen(session);if(session.persisted)return;
            if(!registered.path("versaoDispositivo").isIntegralNumber())throw new MobileFailure(503,"MANAGER_PERSISTENCIA_NAO_ATUALIZADO");
            String user=session.profile.path("id").asText();
            var identity=repository.loadIdentity(user);
            if(!session.login.equalsIgnoreCase(identity.login())||identity.status()!=1||identity.deviceVersion()!=registered.path("versaoDispositivo").asLong())throw new MobileFailure(401,"SESSAO_REVOGADA");
            session.userId=user;session.companyId=identity.companyId();session.deviceVersion=identity.deviceVersion();
            var consumed=new ArrayList<String>();spentRefresh.forEach((key,value)->{if(value==session)consumed.add(key);});
            repository.create(snapshot(session),session.refreshHash,consumed);session.persisted=true;
        }
    }
    JsonNode profile(Session session) {
        if(session.primary)return session.profile.deepCopy();
        var result=MobileUpstream.JSON.createObjectNode();result.put("login",session.login);
        result.put("senhaTemporaria",session.profile.path("senhaTemporaria").asInt());
        String phone=session.profile.path("celular").asText("");result.put("celular",phone.length()<4?"":phone.substring(phone.length()-4));
        // Somente a indicação de conta encontrada é necessária na tela existente.
        result.put("email",session.profile.path("email").asText("").isBlank()?"":"cadastrado");
        return result;
    }
    Map<String,Object> sendCode(Session session,String channel) {
        return sendCode(session,channel,false);
    }
    Map<String,Object> sendCode(Session session,String channel,boolean copyEmail) {
        // Uma integracao lenta deve bloquear apenas o fluxo deste usuario.
        synchronized(session) {
            requireOpen(session);
            limit("otp-user:"+session.login,5,900);
            if(session.nextSend.isAfter(Instant.now()))throw new MobileFailure(429,"AGUARDE_PARA_REENVIAR");
            session.nextSend=Instant.now().plusSeconds(60);
            String code=String.format(Locale.ROOT,"%06d",random.nextInt(1_000_000));
            if(copyEmail&&"sms".equals(channel))upstream.deliverLoginCode(session.profile,code);
            else upstream.deliver(session.profile,channel,code);
            synchronized(this) {
                requireOpen(session);
                session.otpHash=otpHash(session,code);session.otpUntil=Instant.now().plusSeconds(300);session.attempts=0;
            }
            return Map.of("expiresIn",300,"retryAfter",60);
        }
    }
    JsonNode validateCode(Session session,String code) {
        synchronized(session) {
            requireOpen(session);
            if(session.otpHash==null||!session.otpUntil.isAfter(Instant.now())||++session.attempts>5||code==null||!code.matches("[0-9]{6}")||!MessageDigest.isEqual(session.otpHash.getBytes(StandardCharsets.UTF_8),otpHash(session,code).getBytes(StandardCharsets.UTF_8)))throw new MobileFailure(401,"CODIGO_INVALIDO");
            session.otpHash=null;
            JsonNode account=upstream.call("manager","usuario/findByLogin/"+session.login,"GET",null);
            if(account.path("status").asInt()!=1)throw new MobileFailure(403,"USUARIO_INATIVO");
            var profile=(ObjectNode)MobileUpstream.sanitized(account);
            if(session.primary)profile.setAll((ObjectNode)session.profile);
            profile.put("id",account.path("codgUsuario").asInt());profile.put("login",session.login);profile.put("nome",account.path("nomeCompleto").asText());profile.put("codigo",account.path("codigoWooba").asInt());profile.put("tokenAcesso",account.path("idToken").asText());
            if(session.profile.has("tokenIdentificacao"))profile.set("tokenAcesso",session.profile.get("tokenIdentificacao"));
            if(session.profile.has("senhaTemporaria"))profile.set("senhaTemporaria",session.profile.get("senhaTemporaria"));
            JsonNode device=upstream.call("manager","dispositivo/login/"+session.login+"?codigoDispositivo="+session.device,"GET",null);
            synchronized(this) {
                requireOpen(session);
                session.contactResetId=device.path("resetValidacaoId").asText("");
                if(!session.primary)session.until=Instant.now().plus(TOTAL);
                session.profile=profile;session.primary=true;session.contact=true;return profile(session);
            }
        }
    }
    private static void requireOpen(Session session) {
        if(session.revoked||!session.until.isAfter(Instant.now()))throw new MobileFailure(401,"SESSAO_EXPIRADA");
    }
    JsonNode device(Session session) {
        if(!session.primary) {
            JsonNode registered=upstream.call("manager","dispositivo/login/"+session.login,"GET",null);
            // Mantém a indicação do canal existente, sem expor o cadastro nem autorizar a sessão limitada.
            return MobileUpstream.JSON.valueToTree(Map.of("autorizado",false,"situacao","NAO_CADASTRADO","enviarEmail",registered.path("enviarEmail").asInt()));
        }
        JsonNode registered=registeredDevice(session);
        if(session.persisted)validateDevice(session,registered,false);
        else persist(session,registered);
        return registered;
    }
    private JsonNode registeredDevice(Session session) {
        return upstream.call("manager","dispositivo/login/"+session.login+"?codigoDispositivo="+session.device,"GET",null);
    }
    private void validateDevice(Session session,JsonNode registered,boolean renewing) {
        if(!registered.path("versaoDispositivo").isIntegralNumber())throw new MobileFailure(503,"MANAGER_PERSISTENCIA_NAO_ATUALIZADO");
        if(registered.path("versaoDispositivo").asLong()!=session.deviceVersion
                ||!registered.path("autorizado").asBoolean()||!session.device.equals(registered.path("codigoDispositivo").asText())) {
            close(session);throw new MobileFailure(401,"SESSAO_REVOGADA");
        }
    }
    void requireActive(Session session) {
        if(!session.primary)throw new MobileFailure(403,"AUTENTICACAO_NECESSARIA");
        JsonNode device=device(session);
        if(!device.path("autorizado").asBoolean(false)||!session.device.equals(device.path("codigoDispositivo").asText()))throw new MobileFailure(403,"DISPOSITIVO_NAO_AUTORIZADO");
    }
    JsonNode recognize(Session session,JsonNode input) {
        if(!session.primary)throw new MobileFailure(403,"AUTENTICACAO_NECESSARIA");
        limit("recognize:"+session.login,10,300);
        var body=MobileUpstream.JSON.createObjectNode();
        body.put("login",session.login);body.put("codigoDispositivo",session.device);
        for(String field:List.of("codigoDispositivoLegado","modeloDispositivo","marcaDispositivo","tokenExpo")) {
            if(input!=null && input.path(field).isTextual() && input.path(field).asText().length()<=256)body.set(field,input.get(field));
        }
        JsonNode result=upstream.call("manager","dispositivo/reconhecer","POST",body);
        persist(session,result);return result;
    }
    JsonNode register(Session session,String clientIp,JsonNode input) {
        if(!session.primary)throw new MobileFailure(403,"AUTENTICACAO_NECESSARIA");
        JsonNode device=device(session);
        if(!device.path("autorizado").asBoolean() && (!session.contact || !device.path("resetValidacaoId").asText("").equals(session.contactResetId)))
            throw new MobileFailure(403,"VALIDACAO_SMS_NECESSARIA");
        var body=MobileUpstream.JSON.createObjectNode();body.put("login",session.login);body.put("codigoDispositivo",session.device);
        body.put("resetValidacaoId",session.contactResetId);
        for(String field:List.of("modeloDispositivo","marcaDispositivo","metadados","tokenExpo"))if(input.has(field))body.set(field,input.get(field));
        JsonNode result=upstream.registerDevice(body,clientIp);
        persist(session,result);return result;
    }
    private String randomToken(){byte[] bytes=new byte[32];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private String otpHash(Session session,String code){try{var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec(otpKey,"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal((session.login+":"+session.device+":"+code).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
