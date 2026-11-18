package com.confApi.mobile;

import org.junit.jupiter.api.*;
import org.springframework.dao.DataIntegrityViolationException;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import static com.confApi.mobile.MobilePersistenceFixture.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MobileSessionRepositoryTest {
    MobilePersistenceFixture f;
    @BeforeEach void setup() throws Exception {f=new MobilePersistenceFixture();}
    @Test void canonicalIdentityUsesUserIdAgencyStatusAndDeviceGeneration() {
        var identity=f.repository.loadIdentity("42");
        assertEquals("fixture.login",identity.login());assertEquals("42",identity.userId());assertEquals("7",identity.companyId());
        assertEquals(1,identity.status());assertEquals(3,identity.deviceVersion());
        assertEquals(403,assertThrows(MobileFailure.class,()->f.repository.loadIdentity("99")).status);
    }
    @Test void survivesRepositoryRecreationWithoutProfileOrRawTokens() {
        f.repository.create(f.row(),f.refresh);
        var restarted=new MobileSessionRepository(f.source);
        assertEquals("fixture-session",restarted.loadAccess(hash(f.access)).orElseThrow().id());
        assertFalse(restarted.loadRefresh(f.refresh).orElseThrow().used());
        var row=f.jdbc.queryForMap("SELECT * FROM ConfiancaManger.autenticacao_sessao");
        for(String field:List.of("PROFILE_JSON","CLIENT_ID","SESSION_REF","REF_HASH"))assertNull(row.get(field));
        assertEquals(hash(f.access),row.get("ACCESS_HASH"));assertFalse(row.containsValue(f.access));
    }
    @Test void identicalCreateRetryAfterCommitIsIdempotentAcrossInstances() {
        var session=f.row();var spent=List.of(hash("spent-before-promotion"));
        f.repository.create(session,f.refresh,spent);
        assertDoesNotThrow(()->new MobileSessionRepository(f.source).create(session,f.refresh,spent));
        assertEquals(1,f.count("autenticacao_sessao"));assertEquals(2,f.count("autenticacao_controle"));
        assertEquals(MobileSessionRepository.ACTIVE,f.state(session.id()));
    }
    @Test void committedTransactionWithLostAcknowledgmentCanBeRetried() throws Exception {
        var session=f.row();var lost=new java.util.concurrent.atomic.AtomicBoolean();
        DataSource ambiguous=mock(DataSource.class);
        when(ambiguous.getConnection()).thenAnswer(call->{
            var real=f.source.getConnection();
            return java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{java.sql.Connection.class},(proxy,method,args)->{
                try {
                    Object result=method.invoke(real,args);
                    if(method.getName().equals("commit")&&lost.compareAndSet(false,true))throw new SQLException("fixture commit acknowledgement lost");
                    return result;
                }catch(java.lang.reflect.InvocationTargetException error){throw error.getCause();}
            });
        });
        assertEquals(503,assertThrows(MobileFailure.class,()->new MobileSessionRepository(ambiguous).create(session,f.refresh)).status);
        assertTrue(lost.get());assertEquals(1,f.count("autenticacao_sessao"));
        assertDoesNotThrow(()->f.repository.create(session,f.refresh));assertEquals(1,f.count("autenticacao_controle"));
    }
    @Test void identicalConcurrentCreateRetriesDoNotDuplicateRows() throws Exception {
        var session=f.row();var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {
            Callable<Boolean> create=()->{start.await();new MobileSessionRepository(f.source).create(session,f.refresh);return true;};
            var a=pool.submit(create);var b=pool.submit(create);start.countDown();
            assertTrue(a.get(3,TimeUnit.SECONDS));assertTrue(b.get(3,TimeUnit.SECONDS));
            assertEquals(1,f.count("autenticacao_sessao"));assertEquals(1,f.count("autenticacao_controle"));
        }finally{pool.shutdownNow();}
    }
    @Test void createRetryCannotResurrectRevokedSession() {
        var session=f.row();f.repository.create(session,f.refresh);f.repository.close(session.id());
        assertEquals(401,assertThrows(MobileFailure.class,()->f.repository.create(session,f.refresh)).status);
        assertEquals(MobileSessionRepository.REVOKED,f.state(session.id()));assertEquals(1,f.count("autenticacao_controle"));
    }
    @Test void createRetryCannotUndoRefreshRotation() {
        var session=f.row();f.repository.create(session,f.refresh);
        String newAccess=hash("after-rotation-access"),newRefresh=hash("after-rotation-refresh");
        f.repository.rotate(session.id(),f.refresh,newAccess,newRefresh,Instant.now().plusSeconds(600),Instant.now());
        assertEquals(401,assertThrows(MobileFailure.class,()->f.repository.create(session,f.refresh)).status);
        assertTrue(f.repository.loadAccess(newAccess).isPresent());assertFalse(f.repository.loadRefresh(newRefresh).orElseThrow().used());
        assertEquals(1,f.count("autenticacao_sessao"));assertEquals(2,f.count("autenticacao_controle"));
    }
    @Test void createRetryRejectsEveryChangedSessionField() {
        var session=f.row();f.repository.create(session,f.refresh);
        var columns=java.util.Map.of("user_id","43","company_id","8","public_jwk","{}","thumbprint",token("different-key"),
            "device_id","other-device","device_version",4L,"access_hash",hash("different-access"),"nonce",token("different-nonce"),"state","AUTENTICADA");
        for(var change:columns.entrySet()) {
            Object original=f.jdbc.queryForObject("SELECT "+change.getKey()+" FROM ConfiancaManger.autenticacao_sessao",Object.class);
            f.jdbc.update("UPDATE ConfiancaManger.autenticacao_sessao SET "+change.getKey()+"=?",change.getValue());
            assertEquals(401,assertThrows(MobileFailure.class,()->f.repository.create(session,f.refresh),change.getKey()).status);
            f.jdbc.update("UPDATE ConfiancaManger.autenticacao_sessao SET "+change.getKey()+"=?",original);
        }
        for(String timeColumn:List.of("expires_at","absolute_expires_at","created_at")) {
            var original=f.jdbc.queryForObject("SELECT "+timeColumn+" FROM ConfiancaManger.autenticacao_sessao",java.sql.Timestamp.class);
            f.jdbc.update("UPDATE ConfiancaManger.autenticacao_sessao SET "+timeColumn+"=?",java.sql.Timestamp.from(original.toInstant().plusMillis(2)));
            assertEquals(401,assertThrows(MobileFailure.class,()->f.repository.create(session,f.refresh),timeColumn).status);
            f.jdbc.update("UPDATE ConfiancaManger.autenticacao_sessao SET "+timeColumn+"=?",original);
        }
        assertEquals(1,f.count("autenticacao_sessao"));assertEquals(1,f.count("autenticacao_controle"));
    }
    @Test void createRetryRejectsChangedCurrentAndSpentRefreshFamily() {
        var session=f.row();var spent=List.of(hash("spent-token"));f.repository.create(session,f.refresh,spent);
        assertEquals(401,assertThrows(MobileFailure.class,()->f.repository.create(session,hash("other-current"),spent)).status);
        assertEquals(401,assertThrows(MobileFailure.class,()->f.repository.create(session,f.refresh,List.of())).status);
        assertEquals(401,assertThrows(MobileFailure.class,()->f.repository.create(session,f.refresh,List.of(hash("other-spent")))).status);
        f.jdbc.update("UPDATE ConfiancaManger.autenticacao_controle SET used=TRUE WHERE key_hash=?",f.refresh);
        assertEquals(401,assertThrows(MobileFailure.class,()->f.repository.create(session,f.refresh,spent)).status);
        f.jdbc.update("UPDATE ConfiancaManger.autenticacao_controle SET used=FALSE WHERE key_hash=?",f.refresh);
        f.jdbc.update("UPDATE ConfiancaManger.autenticacao_controle SET used=FALSE WHERE key_hash=?",spent.get(0));
        assertEquals(401,assertThrows(MobileFailure.class,()->f.repository.create(session,f.refresh,spent)).status);
    }
    @Test void schemaRejectsPortalFieldsOnMobileRows() {
        f.repository.create(f.row(),f.refresh);
        assertThrows(DataIntegrityViolationException.class,()->f.jdbc.update("UPDATE ConfiancaManger.autenticacao_sessao SET profile_json='{}'"));
    }
    @Test void generationChangedBeforeCreateFailsClosed() {
        f.jdbc.update("UPDATE ConfiancaManger.usuario SET versao_dispositivo=4 WHERE codg_usuario=42");
        assertEquals(403,assertThrows(MobileFailure.class,()->f.repository.create(f.row(),f.refresh)).status);
        assertEquals(0,f.count("autenticacao_sessao"));assertEquals(0,f.count("autenticacao_controle"));
    }
    @Test void inactiveCanonicalUserCannotCreate() {
        f.jdbc.update("UPDATE ConfiancaManger.usuario SET status=0 WHERE codg_usuario=42");
        assertEquals(403,assertThrows(MobileFailure.class,()->f.repository.create(f.row(),f.refresh)).status);
    }
    @Test void rotationIsDurableAndOldRefreshReplayCommitsRevocation() {
        f.repository.create(f.row(),f.refresh);
        String nextAccess=hash("next-access"),nextRefresh=hash("next-refresh");
        f.repository.rotate(f.row().id(),f.refresh,nextAccess,nextRefresh,Instant.now().plusSeconds(700),Instant.now());
        var restarted=new MobileSessionRepository(f.source);
        assertTrue(restarted.loadAccess(hash(f.access)).isEmpty());assertTrue(restarted.loadAccess(nextAccess).isPresent());
        assertTrue(restarted.loadRefresh(f.refresh).orElseThrow().used());
        assertFalse(restarted.loadRefresh(nextRefresh).orElseThrow().used());
        assertEquals(401,assertThrows(MobileFailure.class,()->restarted.rotate(f.row().id(),f.refresh,hash("third-access"),hash("third-refresh"),Instant.now().plusSeconds(700),Instant.now())).status);
        assertEquals(MobileSessionRepository.REVOKED,f.state(f.row().id()));
        assertTrue(new MobileSessionRepository(f.source).loadAccess(nextAccess).isEmpty());
    }
    @Test void prePromotionSpentTokensStayReplayProtectedAfterRestart() {
        String spent=hash("before-promotion");f.repository.create(f.row(),f.refresh,List.of(spent));
        var restarted=new MobileSessionRepository(f.source);
        assertTrue(restarted.loadRefresh(spent).orElseThrow().used());
        assertEquals(401,assertThrows(MobileFailure.class,()->restarted.rotate(f.row().id(),spent,hash("access-2"),hash("refresh-2"),Instant.now().plusSeconds(600),Instant.now())).status);
        assertEquals(MobileSessionRepository.REVOKED,f.state(f.row().id()));
    }
    @Test void duplicateSpentTokenRollsBackTheEntirePromotion() {
        f.repository.create(f.row(),f.refresh);
        assertEquals(503,assertThrows(MobileFailure.class,()->f.repository.create(f.row("another-session","43",token("other")),hash("other-refresh"),List.of(f.refresh))).status);
        assertEquals(1,f.count("autenticacao_sessao"));assertEquals(1,f.count("autenticacao_controle"));
    }
    @Test void generationChangeRevokesOnRotationAndCommits() {
        f.repository.create(f.row(),f.refresh);f.jdbc.update("UPDATE ConfiancaManger.usuario SET versao_dispositivo=4 WHERE codg_usuario=42");
        assertEquals(403,assertThrows(MobileFailure.class,()->f.repository.rotate(f.row().id(),f.refresh,hash("new-access"),hash("new-refresh"),Instant.now().plusSeconds(600),Instant.now())).status);
        assertEquals(MobileSessionRepository.REVOKED,f.state(f.row().id()));
    }
    @Test void expiryCannotBeExtendedBeyondAbsoluteLifetime() {
        var row=f.row();f.repository.create(row,f.refresh);
        var rotated=f.repository.rotate(row.id(),f.refresh,hash("new-access"),hash("new-refresh"),Instant.now().plusSeconds(90000),Instant.now());
        assertEquals(rotated.absoluteExpiresAt(),rotated.expiresAt());
    }
    @Test void logoutSurvivesRestart() {
        f.repository.create(f.row(),f.refresh);f.repository.close(f.row().id());
        var restarted=new MobileSessionRepository(f.source);assertTrue(restarted.loadAccess(hash(f.access)).isEmpty());assertTrue(restarted.loadRefresh(f.refresh).isEmpty());
        assertEquals(MobileSessionRepository.REVOKED,f.state(f.row().id()));
    }
    @Test void concurrentUseOfOneRefreshIssuesOnlyOnceThenRevokesFamily() throws Exception {
        f.repository.create(f.row(),f.refresh);var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {
            Callable<Integer> call=()->{start.await();try{new MobileSessionRepository(f.source).rotate(f.row().id(),f.refresh,hash(Thread.currentThread().getName()+"access"),hash(Thread.currentThread().getName()+"refresh"),Instant.now().plusSeconds(600),Instant.now());return 200;}catch(MobileFailure error){return error.status;}};
            var a=pool.submit(call);var b=pool.submit(call);start.countDown();
            assertEquals(List.of(200,401),java.util.stream.Stream.of(a.get(3,TimeUnit.SECONDS),b.get(3,TimeUnit.SECONDS)).sorted().toList());
            assertEquals(MobileSessionRepository.REVOKED,f.state(f.row().id()));
        }finally{pool.shutdownNow();}
    }
    @Test void lockedUserDoesNotBlockAnotherUsersSession() throws Exception {
        f.repository.create(f.row(),f.refresh);var other=f.row("other","43",token("other"));f.repository.create(other,hash("other-refresh"));
        try(var connection=f.source.getConnection()) {
            connection.setAutoCommit(false);
            try(var statement=connection.prepareStatement("SELECT * FROM ConfiancaManger.usuario WHERE codg_usuario=42 FOR UPDATE")){statement.executeQuery();}
            var pool=Executors.newSingleThreadExecutor();
            try {assertNotNull(pool.submit(()->f.repository.rotate("other",hash("other-refresh"),hash("other-access2"),hash("other-refresh2"),Instant.now().plusSeconds(600),Instant.now())).get(1,TimeUnit.SECONDS));}
            finally{pool.shutdownNow();connection.rollback();}
        }
    }
    @Test void proofReplayUniquenessIsSharedBetweenRepositoryInstances() {
        String proof=hash("proof");assertTrue(f.repository.recordProof(proof,Instant.now().plusSeconds(120)));
        assertFalse(new MobileSessionRepository(f.source).recordProof(proof,Instant.now().plusSeconds(120)));
    }
    @Test void sqlFailuresRemain503InsteadOfInvalidSession() throws Exception {
        DataSource broken=mock(DataSource.class);when(broken.getConnection()).thenThrow(new SQLException("fixture unavailable"));
        var repository=new MobileSessionRepository(broken);
        for(Runnable action:List.<Runnable>of(()->repository.loadIdentity("42"),()->repository.loadAccess(hash("access")),()->repository.loadRefresh(hash("refresh")),
            ()->repository.create(f.row(),f.refresh),()->repository.recordProof(hash("proof"),Instant.now().plusSeconds(120)),()->repository.close("fixture-session"),
            ()->repository.rotate("fixture-session",hash("refresh"),hash("new-access"),hash("new-refresh"),Instant.now().plusSeconds(600),Instant.now())))
            assertEquals(503,assertThrows(MobileFailure.class,action::run).status);
    }
    @Test void cleanupKeepsLiveRefreshAndOtherProtocolRows() {
        f.repository.create(f.row(),f.refresh);
        var foreign=f.row("v2-session","43",token("v2"));f.repository.create(foreign,hash("v2-refresh"));
        f.jdbc.update("UPDATE ConfiancaManger.autenticacao_sessao SET state='AUTENTICADA',absolute_expires_at=? WHERE id='v2-session'",java.sql.Timestamp.from(Instant.now().minusSeconds(10)));
        f.jdbc.update("UPDATE ConfiancaManger.autenticacao_controle SET expires_at=? WHERE session_id='v2-session'",java.sql.Timestamp.from(Instant.now().minusSeconds(10)));
        f.repository.recordProof(hash("expired-proof"),Instant.now().plusSeconds(120));
        f.jdbc.update("UPDATE ConfiancaManger.autenticacao_controle SET expires_at=? WHERE kind='REPLAY'",java.sql.Timestamp.from(Instant.now().minusSeconds(10)));
        f.repository.purgeExpired();
        assertEquals(2,f.count("autenticacao_sessao"));assertEquals(2,f.count("autenticacao_controle"));
        assertFalse(f.repository.loadRefresh(f.refresh).orElseThrow().used());assertEquals("AUTENTICADA",f.state("v2-session"));
        f.jdbc.update("UPDATE ConfiancaManger.autenticacao_sessao SET absolute_expires_at=? WHERE id='fixture-session'",java.sql.Timestamp.from(Instant.now().minusSeconds(10)));
        f.jdbc.update("UPDATE ConfiancaManger.autenticacao_controle SET expires_at=? WHERE session_id='fixture-session'",java.sql.Timestamp.from(Instant.now().minusSeconds(10)));
        f.repository.purgeExpired();assertEquals(1,f.count("autenticacao_sessao"));assertEquals(1,f.count("autenticacao_controle"));
    }
}
