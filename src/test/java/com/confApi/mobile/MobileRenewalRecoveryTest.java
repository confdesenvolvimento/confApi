package com.confApi.mobile;

import org.junit.jupiter.api.*;
import java.time.Instant;
import java.sql.*;
import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MobileRenewalRecoveryTest {
    MobileSessionPersistenceTest b;
    @BeforeEach void setup() throws Exception {b=new MobileSessionPersistenceTest();b.setup();}
    Map<String,Object> renew(MobileSessions target,Map<String,Object> issued,String id) {
        String refresh=issued.get("refreshToken").toString();
        return target.renew(refresh,b.proof(issued,refresh,"POST",b.renewUri),"POST",b.renewUri,id);
    }
    void sameTokens(Map<String,Object> expected,Map<String,Object> actual) {
        for(String field:List.of("accessToken","refreshToken","expiresAt","nonce","tokenType"))assertEquals(expected.get(field),actual.get(field),field);
        assertEquals(((com.fasterxml.jackson.databind.JsonNode)expected.get("profile")).path("login"),((com.fasterxml.jackson.databind.JsonNode)actual.get("profile")).path("login"));
    }
    @Test void lostResponseIsRecoveredAfterRestartWithoutAnotherRotationOrRawTokensInDatabase() {
        var original=b.active();String id=UUID.randomUUID().toString();
        var lost=renew(b.sessions,original,id);
        var recovered=renew(b.instance(),original,id);
        sameTokens(lost,recovered);assertNotNull(b.require(b.instance(),recovered));
        assertEquals(2,b.f.jdbc.queryForObject("SELECT COUNT(*) FROM ConfiancaManger.autenticacao_controle WHERE kind='REFRESH'",Integer.class));
        var rows=b.f.jdbc.queryForList("SELECT * FROM ConfiancaManger.autenticacao_controle").toString();
        assertFalse(rows.contains(original.get("refreshToken").toString()));assertFalse(rows.contains(recovered.get("refreshToken").toString()));
        assertFalse(rows.contains(id));
    }
    @Test void concurrentRetriesAcrossInstancesReturnTheSameSuccessor() throws Exception {
        var original=b.active();String id=UUID.randomUUID().toString();var workers=Executors.newFixedThreadPool(4);
        try {
            var tasks=new ArrayList<Callable<Map<String,Object>>>();
            for(int i=0;i<8;i++)tasks.add(()->renew(b.instance(),original,id));
            var results=workers.invokeAll(tasks,10,TimeUnit.SECONDS);var first=results.get(0).get();
            for(var result:results)assertEquals(first,result.get());
            assertNotNull(b.require(b.instance(),first));
        } finally {workers.shutdownNow();}
    }
    @Test void differentOperationCannotReuseAConsumedRefresh() {
        var original=b.active();var current=renew(b.sessions,original,UUID.randomUUID().toString());
        assertEquals(401,assertThrows(MobileFailure.class,()->renew(b.instance(),original,UUID.randomUUID().toString())).status);
        assertEquals(401,assertThrows(MobileFailure.class,()->b.require(b.instance(),current)).status);
    }
    @Test void anOlderRotationCannotRideTheRecoveryWindowOfANewerRotation() {
        var original=b.active();String first=UUID.randomUUID().toString();
        var next=renew(b.sessions,original,first);
        var latest=renew(b.instance(),next,UUID.randomUUID().toString());
        assertEquals(401,assertThrows(MobileFailure.class,()->renew(b.instance(),original,first)).status);
        assertEquals(401,assertThrows(MobileFailure.class,()->b.require(b.instance(),latest)).status);
    }
    @Test void expiredReceiptCannotRecoverAndRevokesTheFamily() {
        var original=b.active();String id=UUID.randomUUID().toString();
        var row=b.f.repository.loadAccess(MobileProofs.hexHash(original.get("accessToken").toString())).orElseThrow();
        var current=renew(b.sessions,original,id);
        var receipt=MobileRenewals.derive(original.get("refreshToken").toString(),row.id(),id).receipt();
        b.f.jdbc.update("UPDATE ConfiancaManger.autenticacao_controle SET expires_at=? WHERE kind='REPLAY' AND key_hash=?",Timestamp.from(Instant.now().minusSeconds(1)),receipt);
        assertEquals(401,assertThrows(MobileFailure.class,()->renew(b.instance(),original,id)).status);
        assertEquals(401,assertThrows(MobileFailure.class,()->b.require(b.instance(),current)).status);
    }
    @Test void missingOrRepeatedProofCannotUseARecoveryReceipt() {
        var original=b.active();String id=UUID.randomUUID().toString(),token=original.get("refreshToken").toString();
        String proof=b.proof(original,token,"POST",b.renewUri);
        var next=b.sessions.renew(token,proof,"POST",b.renewUri,id);
        assertEquals(401,assertThrows(MobileFailure.class,()->b.instance().renew(token,null,"POST",b.renewUri,id)).status);
        assertEquals(401,assertThrows(MobileFailure.class,()->b.instance().renew(token,proof,"POST",b.renewUri,id)).status);
        sameTokens(next,renew(b.instance(),original,id));
    }
    @Test void proofFromAnotherPhoneCannotRecover() throws Exception {
        var original=b.active();String id=UUID.randomUUID().toString();var next=renew(b.sessions,original,id);
        var intruder=new MobileSessionPersistenceTest();intruder.setup();String token=original.get("refreshToken").toString();
        assertEquals(401,assertThrows(MobileFailure.class,()->b.instance().renew(token,intruder.proof(original,token,"POST",b.renewUri),"POST",b.renewUri,id)).status);
        sameTokens(next,renew(b.instance(),original,id));
    }
    @Test void logoutCannotBeUndoneByRecovery() {
        var original=b.active();String id=UUID.randomUUID().toString();var next=renew(b.sessions,original,id);
        b.sessions.close(b.require(b.sessions,next));
        assertEquals(401,assertThrows(MobileFailure.class,()->renew(b.instance(),original,id)).status);
    }
    @Test void deviceRevocationAndInactiveUserStillBlockRecovery() {
        var original=b.active();String id=UUID.randomUUID().toString();renew(b.sessions,original,id);
        b.f.jdbc.update("UPDATE ConfiancaManger.usuario SET versao_dispositivo=4,status=0 WHERE codg_usuario=42");
        assertEquals(401,assertThrows(MobileFailure.class,()->renew(b.instance(),original,id)).status);
    }
    @Test void pendingSessionRecoveryNeverGrantsBusinessAccess() {
        var original=b.start(false);String id=UUID.randomUUID().toString();
        var next=renew(b.sessions,original,id);assertEquals(next,renew(b.sessions,original,id));
        var session=b.require(b.sessions,next);assertFalse(session.primary);assertFalse(session.persisted);
        assertEquals(403,assertThrows(MobileFailure.class,()->b.sessions.requireActive(session)).status);
    }
    @Test void repositoryRecoversLostCommitAcknowledgment() throws Exception {
        var row=b.f.row();b.f.repository.create(row,b.f.refresh);
        var replacement=MobileRenewals.derive(MobilePersistenceFixture.token("fixture-refresh"),row.id(),UUID.randomUUID().toString());
        var dropped=new AtomicBoolean();DataSource source=mock(DataSource.class);
        when(source.getConnection()).thenAnswer(call->{
            var real=b.f.source.getConnection();
            return java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Connection.class},(proxy,method,args)->{
                try {var result=method.invoke(real,args);if(method.getName().equals("commit")&&dropped.compareAndSet(false,true))throw new SQLException("fixture lost commit response");return result;}
                catch(java.lang.reflect.InvocationTargetException failure){throw failure.getCause();}
            });
        });
        var first=new MobileSessionRepository(source);Instant expiry=Instant.now().plusSeconds(600);
        String ah=MobileProofs.hexHash(replacement.access()),rh=MobileProofs.hexHash(replacement.refresh());
        assertEquals(503,assertThrows(MobileFailure.class,()->first.rotate(row.id(),b.f.refresh,ah,rh,expiry,Instant.now(),replacement.receipt())).status);
        var recovered=b.f.repository.rotate(row.id(),b.f.refresh,ah,rh,expiry,Instant.now(),replacement.receipt());
        assertEquals(ah,recovered.accessHash());assertEquals(MobileSessionRepository.ACTIVE,b.f.state(row.id()));
    }
    @Test void accessRenewedElsewhereDoesNotUseAnExpiredLocalCache() {
        var original=b.active();var cached=b.require(b.sessions,original);cached.accessUntil=Instant.now().minusSeconds(1);
        var next=renew(b.instance(),original,UUID.randomUUID().toString());
        assertNotNull(b.require(b.sessions,next));
    }
    @Test void legacyBearerCannotUseRecoveryEvenWithAnOperationIdentifier() {
        var original=b.sessions.start(b.login,"fixture-password",b.device,"127.0.0.1");String id=UUID.randomUUID().toString(),token=original.get("refreshToken").toString();
        b.sessions.renew(token,null,"POST",b.renewUri,id);
        assertEquals(401,assertThrows(MobileFailure.class,()->b.sessions.renew(token,null,"POST",b.renewUri,id)).status);
    }
    @Test void recoveryIdentifierSurvivesTheActualHttpController() throws Exception {
        var original=b.active();String id=UUID.randomUUID().toString(),token=original.get("refreshToken").toString();
        var settings=new MobileSettings(MobileUpstream.JSON.valueToTree(Map.of("services",Map.of("confapi","https://fixture.invalid/confApi4/"))));
        var api=new MobileApi(b.sessions,new MobileUpstream(settings),new MobileChatDelegations());
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(api).build();
        var body=MobileUpstream.JSON.writeValueAsString(Map.of("refreshToken",token,"renewalId",id));
        com.fasterxml.jackson.databind.JsonNode first=null;
        for(int attempt=0;attempt<2;attempt++) {
            var request=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/confApi4/api/mobile/renovar")
                .contextPath("/confApi4").contentType("application/json").content(body).header("DPoP",b.proof(original,token,"POST",b.renewUri));
            var response=mvc.perform(request).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn().getResponse();
            var payload=MobileUpstream.JSON.readTree(response.getContentAsString());
            if(first==null)first=payload;else {assertEquals(first.get("accessToken"),payload.get("accessToken"));assertEquals(first.get("refreshToken"),payload.get("refreshToken"));}
        }
    }
    @Test void invalidOperationIdDoesNotConsumeTheRefresh() {
        var original=b.active();
        assertEquals(400,assertThrows(MobileFailure.class,()->renew(b.sessions,original,"invalid")).status);
        assertNotNull(renew(b.sessions,original,UUID.randomUUID().toString()));
    }
}
