package com.confApi.mobile;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MobileSessionPersistenceTest {
    final String device="00000000-0000-4000-8000-000000000001",login="fixture.login";
    final String profileUri="https://fixture.invalid/confApi4/api/mobile/perfil",renewUri="https://fixture.invalid/confApi4/api/mobile/renovar";
    MobilePersistenceFixture f;MobileUpstream upstream;MobileSettings settings;MobileSessions sessions;
    JsonNode publicKey,account,approved;
    @BeforeEach void setup() throws Exception {
        f=new MobilePersistenceFixture();upstream=mock(MobileUpstream.class);settings=new MobileSettings(MobileUpstream.JSON.createObjectNode());
        publicKey=MobileUpstream.JSON.readTree(f.key.toPublicJWK().toJSONString());
        account=MobileUpstream.JSON.valueToTree(Map.of("codgUsuario",42,"loginUsuario",login,"status",1,"nomeCompleto","Fixture User","codigoWooba",420,"idToken","fixture-own-key","codgSistemaBackOffice","fixture-backoffice","senha","must-not-leak"));
        approved=MobileUpstream.JSON.valueToTree(Map.of("autorizado",true,"codigoDispositivo",device,"versaoDispositivo",3L,"resetValidacaoId","fixture-reset"));
        var initial=MobileUpstream.JSON.valueToTree(Map.of("id",42,"status",1,"login",login,"celular","65999990000","email","fixture@example.invalid","senhaTemporaria",1,"tokenIdentificacao","fixture-transient-identification"));
        when(upstream.call(eq("manager"),eq("usuario/auth"),eq("POST"),any())).thenReturn(initial);
        when(upstream.call("manager","wooba/turUsuarios/"+login,"POST",null)).thenReturn(initial);
        when(upstream.call("manager","usuario/findByIdUsuario/42","GET",null)).thenReturn(account);
        when(upstream.call("manager","usuario/findByLogin/"+login,"GET",null)).thenReturn(account);
        when(upstream.call("manager","dispositivo/login/"+login+"?codigoDispositivo="+device,"GET",null)).thenReturn(approved);
        when(upstream.call("manager","dispositivo/login/"+login,"GET",null)).thenReturn(approved);
        sessions=instance();
    }
    MobileSessions instance(){var repository=new MobileSessionRepository(f.source);return new MobileSessions(upstream,repository,new MobileProofs(repository,settings));}
    Map<String,Object> start(boolean password){return sessions.start(login,password?"fixture-password":null,device,"127.0.0.1",publicKey);}
    String proof(Map<String,Object> issued,String token,String method,String uri) {
        try {
            var claims=new JWTClaimsSet.Builder().issueTime(Date.from(Instant.now())).jwtID(UUID.randomUUID().toString()).claim("htm",method).claim("htu",uri)
                .claim("ath",MobileProofs.tokenHash(token)).claim("nonce",issued.get("nonce")).build();
            var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(new JOSEObjectType("dpop+jwt")).jwk(f.key.toPublicJWK()).build(),claims);
            jwt.sign(new ECDSASigner(f.key));return jwt.serialize();
        }catch(Exception error){throw new AssertionError(error);}
    }
    MobileSessions.Session require(MobileSessions target,Map<String,Object> issued) {
        String access=issued.get("accessToken").toString();return target.require("Bearer "+access,proof(issued,access,"GET",profileUri),"GET",profileUri);
    }
    Map<String,Object> renew(MobileSessions target,Map<String,Object> issued) {
        String refresh=issued.get("refreshToken").toString();return target.renew(refresh,proof(issued,refresh,"POST",renewUri),"POST",renewUri);
    }
    Map<String,Object> active() {var issued=start(true);sessions.requireActive(require(sessions,issued));return issued;}

    @Test void legacyBearerSessionsRemainTransientAndCompatible() {
        var issued=sessions.start(login,"fixture-password",device,"127.0.0.1");
        var session=sessions.require("Bearer "+issued.get("accessToken"));assertTrue(session.primary);sessions.requireActive(session);
        assertFalse(session.persisted);assertEquals(0,f.count("autenticacao_sessao"));assertFalse(issued.containsKey("nonce"));
        var renewed=sessions.renew(issued.get("refreshToken").toString());assertNotNull(sessions.require("Bearer "+renewed.get("accessToken")));
    }
    @Test void keyBoundSessionRequiresProofBeforeAndAfterPromotion() {
        var issued=start(true);assertEquals("DPoP",issued.get("tokenType"));assertEquals(0,f.count("autenticacao_sessao"));
        assertEquals(401,assertThrows(MobileFailure.class,()->sessions.require("Bearer "+issued.get("accessToken"))).status);
        sessions.requireActive(require(sessions,issued));assertEquals(1,f.count("autenticacao_sessao"));
        assertEquals(401,assertThrows(MobileFailure.class,()->instance().require("Bearer "+issued.get("accessToken"))).status);
    }
    @Test void pendingDeviceCannotCreateDurableSession() {
        when(upstream.call("manager","dispositivo/login/"+login+"?codigoDispositivo="+device,"GET",null)).thenReturn(MobileUpstream.JSON.valueToTree(Map.of("autorizado",false,"situacao","EM_AUTORIZACAO","versaoDispositivo",3)));
        var issued=start(true);assertEquals(403,assertThrows(MobileFailure.class,()->sessions.requireActive(require(sessions,issued))).status);
        assertEquals(0,f.count("autenticacao_sessao"));
    }
    @Test void approvedKeyBoundSessionSurvivesRestartAndRestoresOnlyCanonicalProfile() {
        var issued=active();var restarted=instance();var restored=require(restarted,issued);
        assertTrue(restored.primary&&restored.persisted);assertEquals(login,restored.login);assertEquals(device,restored.device);
        assertEquals(42,restarted.profile(restored).path("id").asInt());assertEquals("fixture-transient-identification",restored.profile.path("tokenAcesso").asText());
        assertEquals("Fixture User",restored.profile.path("nome").asText());assertFalse(restored.profile.has("senha"));
        assertFalse(restored.profile.has("tokenIdentificacao"));assertDoesNotThrow(()->restarted.requireActive(restored));
        verify(upstream).call("manager","usuario/findByIdUsuario/42","GET",null);
    }
    @Test void successfulRecognitionPersistsBeforeReturningToClient() {
        var issued=start(true);var session=require(sessions,issued);
        when(upstream.call(eq("manager"),eq("dispositivo/reconhecer"),eq("POST"),any())).thenReturn(approved);
        assertEquals(0,f.count("autenticacao_sessao"));
        sessions.recognize(session,MobileUpstream.JSON.createObjectNode());
        assertTrue(require(instance(),issued).persisted);assertEquals(1,f.count("autenticacao_sessao"));
    }
    @Test void successfulRegistrationPersistsBeforeReturningToClient() {
        var issued=start(true);var session=require(sessions,issued);session.contact=true;session.contactResetId="fixture-reset";
        when(upstream.call("manager","dispositivo/login/"+login+"?codigoDispositivo="+device,"GET",null)).thenReturn(MobileUpstream.JSON.valueToTree(Map.of("autorizado",false,"resetValidacaoId","fixture-reset","versaoDispositivo",3)));
        when(upstream.registerDevice(any(),eq("127.0.0.1"))).thenReturn(approved);
        assertEquals(0,f.count("autenticacao_sessao"));
        sessions.register(session,"127.0.0.1",MobileUpstream.JSON.createObjectNode());
        assertTrue(require(instance(),issued).persisted);assertEquals(1,f.count("autenticacao_sessao"));
    }
    @Test void renewalSurvivesRestartAndInvalidatesOldAccessOnCachedInstance() {
        var issued=active();var cached=require(sessions,issued);assertTrue(cached.persisted);
        var renewed=renew(instance(),issued);assertNotEquals(issued.get("accessToken"),renewed.get("accessToken"));
        assertEquals(401,assertThrows(MobileFailure.class,()->require(sessions,issued)).status);
        assertNotNull(require(instance(),renewed));assertNotNull(require(sessions,renewed));
    }
    @Test void logoutRevokesCachedAndFreshInstances() {
        var issued=active();require(sessions,issued);var other=instance();other.close(require(other,issued));
        assertEquals(401,assertThrows(MobileFailure.class,()->require(sessions,issued)).status);
        assertEquals(401,assertThrows(MobileFailure.class,()->require(instance(),issued)).status);
        assertEquals(401,assertThrows(MobileFailure.class,()->renew(instance(),issued)).status);
    }
    @Test void consumedRefreshBeforePromotionRemainsRevokingAfterRestart() {
        var before=start(true);var current=renew(sessions,before);sessions.requireActive(require(sessions,current));
        assertEquals(401,assertThrows(MobileFailure.class,()->renew(instance(),before)).status);
        assertEquals(401,assertThrows(MobileFailure.class,()->require(sessions,current)).status);
        assertEquals(MobileSessionRepository.REVOKED,f.jdbc.queryForObject("SELECT state FROM ConfiancaManger.autenticacao_sessao",String.class));
    }
    @Test void confirmedRefreshReplayRevokesEvenWhenManagerIsUnavailable() {
        var issued=active();var current=renew(instance(),issued);
        when(upstream.call("manager","usuario/findByIdUsuario/42","GET",null)).thenThrow(new MobileFailure(503,"UPSTREAM_UNAVAILABLE"));
        clearInvocations(upstream);
        assertEquals(401,assertThrows(MobileFailure.class,()->renew(instance(),issued)).status);
        assertEquals(MobileSessionRepository.REVOKED,f.jdbc.queryForObject("SELECT state FROM ConfiancaManger.autenticacao_sessao",String.class));
        assertEquals(401,assertThrows(MobileFailure.class,()->require(sessions,current)).status);
        verifyNoInteractions(upstream);
    }
    @Test void databaseOutageNeverFallsBackToCachedAuthorization() {
        var issued=active();require(sessions,issued);
        f.jdbc.execute("ALTER TABLE ConfiancaManger.autenticacao_sessao RENAME TO fixture_unavailable");
        assertEquals(503,assertThrows(MobileFailure.class,()->require(sessions,issued)).status);
        assertEquals(503,assertThrows(MobileFailure.class,()->renew(sessions,issued)).status);
        assertEquals(503,assertThrows(MobileFailure.class,()->require(instance(),issued)).status);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints={502,503,504})
    void temporaryDeviceOutagePreservesSessionAndRefreshCanBeRetried(int status) {
        var issued=active();
        String path="dispositivo/login/"+login+"?codigoDispositivo="+device;
        when(upstream.call("manager",path,"GET",null)).thenThrow(new MobileFailure(status,"DISPOSITIVO_TEMPORARIAMENTE_INDISPONIVEL"));
        assertEquals(status,assertThrows(MobileFailure.class,()->sessions.requireActive(require(sessions,issued))).status);
        assertEquals(status,assertThrows(MobileFailure.class,()->renew(sessions,issued)).status);
        assertEquals(MobileSessionRepository.ACTIVE,f.jdbc.queryForObject("SELECT state FROM ConfiancaManger.autenticacao_sessao",String.class));
        doReturn(approved).when(upstream).call("manager",path,"GET",null);
        var restarted=instance();
        var recovered=renew(restarted,issued); // The failed attempt did not consume the refresh token.
        restarted.requireActive(require(restarted,recovered));
        assertEquals(MobileSessionRepository.ACTIVE,f.jdbc.queryForObject("SELECT state FROM ConfiancaManger.autenticacao_sessao",String.class));
    }
    @Test void canonicalDeviceResetRevokesEvenCachedSession() {
        var issued=active();require(sessions,issued);f.jdbc.update("UPDATE ConfiancaManger.usuario SET versao_dispositivo=4 WHERE codg_usuario=42");
        assertEquals(401,assertThrows(MobileFailure.class,()->require(sessions,issued)).status);
        assertEquals(MobileSessionRepository.REVOKED,f.jdbc.queryForObject("SELECT state FROM ConfiancaManger.autenticacao_sessao",String.class));
        assertEquals(401,assertThrows(MobileFailure.class,()->renew(instance(),issued)).status);
    }
    @Test void restoredSessionDoesNotInheritOtpContactApproval() {
        var issued=start(false);var initial=require(sessions,issued);final String[] code={null};
        doAnswer(call->{code[0]=call.getArgument(2);return null;}).when(upstream).deliver(any(),eq("sms"),anyString());
        sessions.sendCode(initial,"sms");sessions.validateCode(initial,code[0]);assertTrue(initial.contact);assertEquals("fixture-reset",initial.contactResetId);
        sessions.requireActive(initial);assertTrue(initial.persisted);
        var restored=require(instance(),issued);assertFalse(restored.contact);assertEquals("",restored.contactResetId);assertNull(restored.otpHash);
        assertEquals("fixture-transient-identification",initial.profile.path("tokenAcesso").asText());
        assertEquals(initial.profile.path("tokenAcesso"),restored.profile.path("tokenAcesso"));
        assertEquals(initial.profile.path("senhaTemporaria"),restored.profile.path("senhaTemporaria"));
    }
    @Test void temporaryProfileHydrationFailureDoesNotRevokeSessionAndRetrySucceeds() {
        var issued=active();
        when(upstream.call("manager","wooba/turUsuarios/"+login,"POST",null))
            .thenThrow(new MobileFailure(503,"UPSTREAM_UNAVAILABLE"))
            .thenReturn(MobileUpstream.JSON.valueToTree(Map.of("tokenIdentificacao","fixture-transient-identification","senhaTemporaria",1)));
        assertEquals(503,assertThrows(MobileFailure.class,()->require(instance(),issued)).status);
        assertEquals(MobileSessionRepository.ACTIVE,f.jdbc.queryForObject("SELECT state FROM ConfiancaManger.autenticacao_sessao",String.class));
        assertEquals("fixture-transient-identification",require(instance(),issued).profile.path("tokenAcesso").asText());
    }
    @Test void wrongProofCannotLoadUserProfileFromUpstream() {
        var issued=active();clearInvocations(upstream);
        String access=issued.get("accessToken").toString();String incorrect=proof(issued,access,"POST",profileUri);
        assertEquals(401,assertThrows(MobileFailure.class,()->instance().require("Bearer "+access,incorrect,"GET",profileUri)).status);
        verifyNoInteractions(upstream);
    }
    @Test void logoutCompletesWhileSameSessionOtpDeliveryIsSlow() throws Exception {
        var issued=active();var session=require(sessions,issued);var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        doAnswer(call->{entered.countDown();if(!release.await(3,TimeUnit.SECONDS))throw new AssertionError("test gate timed out");return null;}).when(upstream).deliver(any(),eq("sms"),anyString());
        try {
            var sending=pool.submit(()->sessions.sendCode(session,"sms"));assertTrue(entered.await(1,TimeUnit.SECONDS));
            pool.submit(()->sessions.close(session)).get(1,TimeUnit.SECONDS);assertFalse(sending.isDone());release.countDown();
            var error=assertThrows(ExecutionException.class,()->sending.get(2,TimeUnit.SECONDS));assertInstanceOf(MobileFailure.class,error.getCause());assertNull(session.otpHash);
            assertEquals(401,assertThrows(MobileFailure.class,()->require(instance(),issued)).status);
        }finally{release.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(2,TimeUnit.SECONDS));}
    }
    @Test void concurrentLogoutCannotBeUndoneByAnAlreadyStartedRenewal() throws Exception {
        var issued=active();var renewingInstance=instance();var loggingOutInstance=instance();var logoutSession=require(loggingOutInstance,issued);
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        when(upstream.call("manager","dispositivo/login/"+login+"?codigoDispositivo="+device,"GET",null)).thenAnswer(call->{entered.countDown();if(!release.await(3,TimeUnit.SECONDS))throw new AssertionError("test gate timed out");return approved;});
        try {
            var renewal=pool.submit(()->renew(renewingInstance,issued));assertTrue(entered.await(1,TimeUnit.SECONDS));
            pool.submit(()->loggingOutInstance.close(logoutSession)).get(1,TimeUnit.SECONDS);release.countDown();
            var error=assertThrows(ExecutionException.class,()->renewal.get(2,TimeUnit.SECONDS));assertEquals(401,((MobileFailure)error.getCause()).status);
            assertEquals(MobileSessionRepository.REVOKED,f.jdbc.queryForObject("SELECT state FROM ConfiancaManger.autenticacao_sessao",String.class));
        }finally{release.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(2,TimeUnit.SECONDS));}
    }
}
