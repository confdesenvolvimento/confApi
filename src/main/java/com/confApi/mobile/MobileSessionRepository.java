package com.confApi.mobile;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

/** Persiste somente sessoes proprias da ConfApi nas tabelas existentes do Manager. */
@Repository
public class MobileSessionRepository {
    public static final String ACTIVE="CONFAPI_ATIVA",REVOKED="CONFAPI_REVOGADA";
    private static final String SESSIONS="ConfiancaManger.autenticacao_sessao";
    private static final String CONTROLS="ConfiancaManger.autenticacao_controle";
    private static final String USERS="ConfiancaManger.usuario";
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public record DurableSession(String id,String userId,String companyId,String publicJwk,String thumbprint,
            String deviceId,Long deviceVersion,String state,String accessHash,String nonce,
            Instant expiresAt,Instant absoluteExpiresAt,Instant createdAt) {}
    public record RefreshSession(DurableSession session,boolean used,Instant refreshExpiresAt) {}
    public record Identity(String login,String userId,String companyId,int status,long deviceVersion) {}
    private record Rotation(DurableSession session,int failure,String code) {}
    private record RefreshControl(String hash,boolean used,Instant expiresAt) {}

    public MobileSessionRepository(@Qualifier("dataSource") DataSource source) {
        jdbc=new JdbcTemplate(source);jdbc.setQueryTimeout(5);
        transaction=new TransactionTemplate(new DataSourceTransactionManager(source));
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transaction.setTimeout(5);
    }
    private static final RowMapper<DurableSession> SESSION=(row,index)->new DurableSession(
        row.getString("id"),row.getString("user_id"),row.getString("company_id"),row.getString("public_jwk"),
        row.getString("thumbprint"),row.getString("device_id"),nullableLong(row,"device_version"),row.getString("state"),
        row.getString("access_hash"),row.getString("nonce"),time(row,"expires_at"),time(row,"absolute_expires_at"),time(row,"created_at"));
    private static final RowMapper<Identity> IDENTITY=(row,index)->new Identity(row.getString("login_usuario"),
        row.getString("codg_usuario"),Objects.toString(row.getObject("codg_agencia"),"0"),row.getInt("status"),row.getLong("versao_dispositivo"));

    public Identity loadIdentity(String userId) {
        requireUserId(userId);
        return available(()->identity(userId,false).orElseThrow(()->new MobileFailure(403,"USUARIO_INDISPONIVEL")));
    }
    public Optional<DurableSession> loadAccess(String accessHash) {
        requireHash(accessHash);
        return available(()->jdbc.query("SELECT * FROM "+SESSIONS+" WHERE kind='MOBILE' AND state=? AND access_hash=?",SESSION,ACTIVE,accessHash).stream().findFirst());
    }
    public Optional<RefreshSession> loadRefresh(String refreshHash) {
        requireHash(refreshHash);
        return available(()->jdbc.query("SELECT s.*,r.used AS refresh_used,r.expires_at AS refresh_expires_at FROM "+SESSIONS+
            " s JOIN "+CONTROLS+" r ON r.session_id=s.id AND r.kind='REFRESH' WHERE s.kind='MOBILE' AND s.state=? AND r.key_hash=?",
            (row,index)->new RefreshSession(SESSION.mapRow(row,index),row.getBoolean("refresh_used"),time(row,"refresh_expires_at")),ACTIVE,refreshHash).stream().findFirst());
    }
    public void create(DurableSession session,String refreshHash) {
        create(session,refreshHash,List.of());
    }
    public void create(DurableSession session,String refreshHash,Collection<String> spentHashes) {
        validate(session);requireHash(refreshHash);
        Set<String> spent=new LinkedHashSet<>(Objects.requireNonNull(spentHashes));
        spent.forEach(MobileSessionRepository::requireHash);
        if(spent.contains(refreshHash))throw new MobileFailure(401,"SESSAO_INVALIDA");
        available(()->transaction.execute(status->{
            Identity identity=identity(session.userId(),true).orElseThrow(()->new MobileFailure(403,"USUARIO_INDISPONIVEL"));
            requireIdentity(identity,session);
            var existing=jdbc.query("SELECT * FROM "+SESSIONS+" WHERE id=? FOR UPDATE",SESSION,session.id()).stream().findFirst();
            if(existing.isPresent()) {
                // Um commit pode ter sido confirmado no banco antes de a conexao falhar.
                // Aceitar apenas a mesma promocao inteira; nunca modificar ou reativar a linha.
                if(!samePromotion(existing.get(),session)||!sameRefreshFamily(session,refreshHash,spent))throw new MobileFailure(401,"SESSAO_INVALIDA");
                return null;
            }
            jdbc.update("INSERT INTO "+SESSIONS+" (id,kind,user_id,company_id,public_jwk,thumbprint,device_id,device_version,state,access_hash,nonce,expires_at,absolute_expires_at,created_at,completed) VALUES (?,'MOBILE',?,?,?,?,?,?,?,?,?,?,?,?,FALSE)",
                session.id(),session.userId(),session.companyId(),session.publicJwk(),session.thumbprint(),session.deviceId(),session.deviceVersion(),ACTIVE,
                session.accessHash(),session.nonce(),timestamp(session.expiresAt()),timestamp(session.absoluteExpiresAt()),timestamp(session.createdAt()));
            jdbc.update("INSERT INTO "+CONTROLS+" (kind,key_hash,session_id,used,expires_at) VALUES ('REFRESH',?,?,FALSE,?)",refreshHash,session.id(),timestamp(session.absoluteExpiresAt()));
            for(String previous:spent)jdbc.update("INSERT INTO "+CONTROLS+" (kind,key_hash,session_id,used,expires_at) VALUES ('REFRESH',?,?,TRUE,?)",previous,session.id(),timestamp(session.absoluteExpiresAt()));
            return null;
        }));
    }
    /** O uso anterior revoga a familia e faz commit antes de retornar401. */
    public DurableSession rotate(String sessionId,String oldRefreshHash,String newAccessHash,String newRefreshHash,Instant newAccessExpiresAt,Instant now) {
        return rotate(sessionId,oldRefreshHash,newAccessHash,newRefreshHash,newAccessExpiresAt,now,null);
    }
    /** A receipt authorizes only the exact still-current successor, never an older rotation or another device. */
    public DurableSession rotate(String sessionId,String oldRefreshHash,String newAccessHash,String newRefreshHash,Instant newAccessExpiresAt,Instant now,String receiptHash) {
        requireHash(oldRefreshHash);requireHash(newAccessHash);requireHash(newRefreshHash);
        if(receiptHash!=null)requireHash(receiptHash);
        if(now==null||newAccessExpiresAt==null||!newAccessExpiresAt.isAfter(now)||oldRefreshHash.equals(newRefreshHash))throw new MobileFailure(401,"SESSAO_INVALIDA");
        Rotation outcome=available(()->transaction.execute(status->{
            var observed=owned(sessionId,false);
            if(observed.isEmpty())return rejected(401,"SESSAO_EXPIRADA");
            // Ordem fixa usuario -> sessao -> refresh. Nenhuma chamadaHTTP pertence a esta transacao.
            var identity=identity(observed.get().userId(),true);
            var current=owned(sessionId,true);
            if(current.isEmpty()||!ACTIVE.equals(current.get().state()))return rejected(401,"SESSAO_EXPIRADA");
            DurableSession session=current.get();
            var refresh=jdbc.query("SELECT used,expires_at FROM "+CONTROLS+" WHERE kind='REFRESH' AND key_hash=? AND session_id=? FOR UPDATE",
                (row,index)->Map.entry(row.getBoolean("used"),time(row,"expires_at")),oldRefreshHash,sessionId);
            if(refresh.size()!=1)return rejected(401,"SESSAO_EXPIRADA");
            if(!refresh.get(0).getValue().isAfter(now)||!session.absoluteExpiresAt().isAfter(now)) {revoke(sessionId);return rejected(401,"SESSAO_EXPIRADA");}
            if(identity.isEmpty()||!matches(identity.get(),session)) {revoke(sessionId);return rejected(403,"USUARIO_OU_DISPOSITIVO_ALTERADO");}
            if(refresh.get(0).getKey()) {
                boolean receipt=receiptHash!=null&&jdbc.queryForObject("SELECT COUNT(*) FROM "+CONTROLS+" WHERE kind='REPLAY' AND key_hash=? AND expires_at>?",Integer.class,receiptHash,timestamp(now))==1;
                boolean successor=newAccessHash.equals(session.accessHash())&&session.expiresAt().isAfter(now)
                    &&jdbc.queryForObject("SELECT COUNT(*) FROM "+CONTROLS+" WHERE kind='REFRESH' AND key_hash=? AND session_id=? AND used=FALSE AND expires_at>?",Integer.class,newRefreshHash,sessionId,timestamp(now))==1;
                if(receipt&&successor)return new Rotation(session,0,null);
                revoke(sessionId);return rejected(401,"SESSAO_INVALIDA");
            }
            if(newAccessHash.equals(session.accessHash()))return rejected(401,"SESSAO_INVALIDA");
            Instant expires=newAccessExpiresAt.isAfter(session.absoluteExpiresAt())?session.absoluteExpiresAt():newAccessExpiresAt;
            jdbc.update("UPDATE "+CONTROLS+" SET used=TRUE WHERE kind='REFRESH' AND key_hash=? AND session_id=?",oldRefreshHash,sessionId);
            jdbc.update("UPDATE "+SESSIONS+" SET access_hash=?,expires_at=? WHERE kind='MOBILE' AND state=? AND id=?",newAccessHash,timestamp(expires),ACTIVE,sessionId);
            jdbc.update("INSERT INTO "+CONTROLS+" (kind,key_hash,session_id,used,expires_at) VALUES ('REFRESH',?,?,FALSE,?)",newRefreshHash,sessionId,timestamp(session.absoluteExpiresAt()));
            // Domain-separated short-lived receipt uses the existing replay-control schema; no raw token is stored.
            if(receiptHash!=null)jdbc.update("INSERT INTO "+CONTROLS+" (kind,key_hash,hits,expires_at) VALUES ('REPLAY',?,NULL,?)",receiptHash,timestamp(now.plus(MobileRenewals.RECOVERY)));
            return new Rotation(owned(sessionId,false).orElseThrow(),0,null);
        }));
        if(outcome==null)throw unavailable();
        if(outcome.failure()!=0)throw new MobileFailure(outcome.failure(),outcome.code());
        return outcome.session();
    }
    public void close(String sessionId) {
        available(()->transaction.execute(status->{
            var observed=owned(sessionId,false);
            if(observed.isPresent()){
                identity(observed.get().userId(),true);
                if(owned(sessionId,true).isPresent())revoke(sessionId);
            }
            return null;
        }));
    }
    /** A unicidade no banco torna a protecao contra replay valida entre instancias. */
    public boolean recordProof(String replayHash,Instant expiresAt) {
        requireHash(replayHash);
        if(expiresAt==null||!expiresAt.isAfter(Instant.now()))throw new MobileFailure(401,"PROVA_INVALIDA");
        try {
            return Boolean.TRUE.equals(transaction.execute(status->{jdbc.update("INSERT INTO "+CONTROLS+" (kind,key_hash,hits,expires_at) VALUES ('REPLAY',?,NULL,?)",replayHash,timestamp(expiresAt));return true;}));
        }catch(DuplicateKeyException duplicate){return false;}
        catch(DataAccessException failure){throw unavailable();}
        catch(org.springframework.transaction.TransactionException failure){throw unavailable();}
    }
    /** A faixa kind/expiry possui indice existente; cada comando e uma transacao curta. */
    @Scheduled(fixedDelay=1000,initialDelay=60000)
    public void purgeExpiredControls(){purgeControls(Instant.now());}
    // A tabela de sessoes nao possui indice de expiração; evitar varre-la a cada segundo.
    @Scheduled(fixedDelay=900000,initialDelay=60000)
    public void purgeExpiredSessions(){purgeSessions(Instant.now());}
    public void purgeExpired(){purgeExpired(Instant.now());}
    void purgeExpired(Instant now) {
        purgeControls(now);purgeSessions(now);
    }
    private void purgeControls(Instant now) {
        cleanupCommand("DELETE FROM "+CONTROLS+" WHERE kind='REPLAY' AND expires_at<? LIMIT 5000",timestamp(now));
        cleanupCommand("DELETE FROM "+CONTROLS+" WHERE kind='REFRESH' AND expires_at<? AND EXISTS (SELECT 1 FROM "+SESSIONS+" s WHERE s.id="+CONTROLS+".session_id AND s.kind='MOBILE' AND s.state IN (?,?)) LIMIT 5000",timestamp(now),ACTIVE,REVOKED);
    }
    private void purgeSessions(Instant now) {
        cleanupCommand("DELETE FROM "+SESSIONS+" WHERE kind='MOBILE' AND state IN (?,?) AND absolute_expires_at<? AND NOT EXISTS (SELECT 1 FROM "+CONTROLS+" c WHERE c.session_id="+SESSIONS+".id) LIMIT 1000",ACTIVE,REVOKED,timestamp(now));
    }
    private void cleanupCommand(String sql,Object... arguments) {
        available(()->transaction.execute(status->jdbc.update(sql,arguments)));
    }
    private Optional<DurableSession> owned(String id,boolean lock) {
        return jdbc.query("SELECT * FROM "+SESSIONS+" WHERE kind='MOBILE' AND state IN (?,?) AND id=?"+(lock?" FOR UPDATE":""),SESSION,ACTIVE,REVOKED,id).stream().findFirst();
    }
    private boolean sameRefreshFamily(DurableSession session,String current,Set<String> spent) {
        var controls=jdbc.query("SELECT key_hash,used,expires_at FROM "+CONTROLS+" WHERE kind='REFRESH' AND session_id=? ORDER BY key_hash FOR UPDATE",
            (row,index)->new RefreshControl(row.getString("key_hash"),row.getBoolean("used"),time(row,"expires_at")),session.id());
        if(controls.size()!=spent.size()+1)return false;
        return controls.stream().allMatch(control->sameTime(control.expiresAt(),session.absoluteExpiresAt())
            &&(control.hash().equals(current)?!control.used():spent.contains(control.hash())&&control.used()));
    }
    private static boolean samePromotion(DurableSession left,DurableSession right) {
        return ACTIVE.equals(left.state())&&Objects.equals(left.id(),right.id())&&Objects.equals(left.userId(),right.userId())
            &&Objects.equals(left.companyId(),right.companyId())&&Objects.equals(left.publicJwk(),right.publicJwk())
            &&Objects.equals(left.thumbprint(),right.thumbprint())&&Objects.equals(left.deviceId(),right.deviceId())
            &&Objects.equals(left.deviceVersion(),right.deviceVersion())&&Objects.equals(left.accessHash(),right.accessHash())
            &&Objects.equals(left.nonce(),right.nonce())&&sameTime(left.expiresAt(),right.expiresAt())
            &&sameTime(left.absoluteExpiresAt(),right.absoluteExpiresAt())&&sameTime(left.createdAt(),right.createdAt());
    }
    private static boolean sameTime(Instant left,Instant right) {
        return left!=null&&right!=null&&Duration.between(left,right).abs().compareTo(Duration.ofMillis(1))<=0;
    }
    private Optional<Identity> identity(String userId,boolean lock) {
        return jdbc.query("SELECT codg_usuario,login_usuario,codg_agencia,versao_dispositivo,status FROM "+USERS+" WHERE codg_usuario=?"+(lock?" FOR UPDATE":""),IDENTITY,userId).stream().findFirst();
    }
    private void revoke(String id){jdbc.update("UPDATE "+SESSIONS+" SET state=? WHERE kind='MOBILE' AND state=? AND id=?",REVOKED,ACTIVE,id);}
    private static Rotation rejected(int status,String code){return new Rotation(null,status,code);}
    private static boolean matches(Identity identity,DurableSession session){return identity.status()==1&&identity.userId().equals(session.userId())&&identity.companyId().equals(session.companyId())&&session.deviceVersion()!=null&&identity.deviceVersion()==session.deviceVersion();}
    private static void requireIdentity(Identity identity,DurableSession session){if(!matches(identity,session))throw new MobileFailure(403,"USUARIO_OU_DISPOSITIVO_ALTERADO");}
    private static void requireUserId(String id){if(id==null||!id.matches("[1-9][0-9]{0,9}"))throw new MobileFailure(403,"USUARIO_INDISPONIVEL");}
    private static void requireHash(String hash){if(hash==null||!hash.matches("[a-fA-F0-9]{64}"))throw new MobileFailure(401,"SESSAO_INVALIDA");}
    private static void validate(DurableSession s) {
        Objects.requireNonNull(s);requireUserId(s.userId());requireHash(s.accessHash());
        if(s.id()==null||!s.id().matches("[A-Za-z0-9_-]{1,64}")||!ACTIVE.equals(s.state())||s.companyId()==null||s.companyId().isBlank()||s.companyId().length()>128
            ||s.publicJwk()==null||s.publicJwk().isBlank()||s.publicJwk().length()>4096||s.thumbprint()==null||!s.thumbprint().matches("[A-Za-z0-9_-]{43}")
            ||s.deviceId()==null||s.deviceId().isBlank()||s.deviceId().length()>128||s.deviceVersion()==null||s.deviceVersion()<0||s.nonce()==null||!s.nonce().matches("[A-Za-z0-9_-]{43}")
            ||s.createdAt()==null||s.expiresAt()==null||s.absoluteExpiresAt()==null||!s.expiresAt().isAfter(s.createdAt())||s.expiresAt().isAfter(s.absoluteExpiresAt())
            ||!s.absoluteExpiresAt().isAfter(Instant.now()))throw new MobileFailure(401,"SESSAO_INVALIDA");
    }
    private static <T>T available(Supplier<T> work){try{return work.get();}catch(DataAccessException|org.springframework.transaction.TransactionException failure){throw unavailable();}}
    private static MobileFailure unavailable(){return new MobileFailure(503,"PERSISTENCIA_INDISPONIVEL");}
    private static Long nullableLong(ResultSet row,String name)throws SQLException{long value=row.getLong(name);return row.wasNull()?null:value;}
    private static Instant time(ResultSet row,String name)throws SQLException{Timestamp value=row.getTimestamp(name);return value==null?null:value.toInstant();}
    private static Timestamp timestamp(Instant value){return Timestamp.from(value);}
}
