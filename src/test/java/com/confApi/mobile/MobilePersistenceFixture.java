package com.confApi.mobile;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.Instant;
import java.util.UUID;

final class MobilePersistenceFixture {
    final JdbcDataSource source=new JdbcDataSource();
    final JdbcTemplate jdbc;
    final MobileSessionRepository repository;
    final ECKey key;
    final String access=token("access"),refresh=hash("refresh");
    MobilePersistenceFixture() throws Exception {
        source.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=1000");
        jdbc=new JdbcTemplate(source);
        jdbc.execute("CREATE SCHEMA ConfiancaManger");
        jdbc.execute("CREATE TABLE ConfiancaManger.usuario(codg_usuario INT PRIMARY KEY,login_usuario VARCHAR(128),codg_agencia INT,versao_dispositivo BIGINT,status INT)");
        jdbc.update("INSERT INTO ConfiancaManger.usuario VALUES(42,'fixture.login',7,3,1),(43,'fixture.other',7,3,1)");
        jdbc.execute("""
          CREATE TABLE ConfiancaManger.autenticacao_sessao (
           id VARCHAR(64) PRIMARY KEY, kind VARCHAR(10) NOT NULL,
           user_id VARCHAR(128) NOT NULL, company_id VARCHAR(128) NOT NULL,
           public_jwk TEXT, thumbprint VARCHAR(64), device_id VARCHAR(128), device_version BIGINT,
           state VARCHAR(20) NOT NULL, access_hash VARCHAR(64) UNIQUE, nonce VARCHAR(64),
           expires_at TIMESTAMP(6) NOT NULL, absolute_expires_at TIMESTAMP(6), created_at TIMESTAMP(6) NOT NULL,
           client_id VARCHAR(128), session_ref VARCHAR(128), profile_json TEXT,
           completed BOOLEAN NOT NULL DEFAULT FALSE, ref_hash VARCHAR(64) UNIQUE,
           CONSTRAINT ck_autenticacao_sessao_tipo CHECK (
            (kind='MOBILE' AND public_jwk IS NOT NULL AND thumbprint IS NOT NULL AND access_hash IS NOT NULL
             AND nonce IS NOT NULL AND absolute_expires_at IS NOT NULL AND client_id IS NULL
             AND session_ref IS NULL AND profile_json IS NULL AND ref_hash IS NULL AND completed=FALSE)
            OR (kind='PORTAL' AND client_id IS NOT NULL AND session_ref IS NOT NULL AND profile_json IS NOT NULL
             AND ref_hash IS NOT NULL AND public_jwk IS NULL AND thumbprint IS NULL AND access_hash IS NULL
             AND nonce IS NULL AND absolute_expires_at IS NULL AND device_id IS NULL AND device_version IS NULL)))
          """);
        jdbc.execute("""
          CREATE TABLE ConfiancaManger.autenticacao_controle (
           kind VARCHAR(10) NOT NULL, key_hash VARCHAR(64) NOT NULL, hits BIGINT,
           expires_at TIMESTAMP(6) NOT NULL, session_id VARCHAR(64), used BOOLEAN,
           PRIMARY KEY(kind,key_hash), FOREIGN KEY(session_id) REFERENCES ConfiancaManger.autenticacao_sessao(id),
           CONSTRAINT ck_autenticacao_controle_tipo CHECK (
            (kind='LIMITE' AND hits IS NOT NULL AND hits>=1 AND session_id IS NULL AND used IS NULL)
            OR (kind='REPLAY' AND hits IS NULL AND session_id IS NULL AND used IS NULL)
            OR (kind='REFRESH' AND hits IS NULL AND session_id IS NOT NULL AND used IS NOT NULL)))
          """);
        repository=new MobileSessionRepository(source);
        key=new ECKeyGenerator(Curve.P_256).generate();
    }
    MobileSessionRepository.DurableSession row(){return row("fixture-session","42",access);}
    MobileSessionRepository.DurableSession row(String id,String userId,String token) {
        try {
            Instant now=Instant.now();
            return new MobileSessionRepository.DurableSession(id,userId,"7",key.toPublicJWK().toJSONString(),key.computeThumbprint().toString(),
                "fixture-device",3L,MobileSessionRepository.ACTIVE,hash(token),token("nonce"),now.plusSeconds(600),now.plusSeconds(7200),now);
        }catch(Exception error){throw new IllegalStateException(error);}
    }
    String state(String id){return jdbc.queryForObject("SELECT state FROM ConfiancaManger.autenticacao_sessao WHERE id=?",String.class,id);}
    int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM ConfiancaManger."+table,Integer.class);}
    static String token(String text){return MobileProofs.tokenHash(text);}
    static String hash(String text){return MobileProofs.hexHash(text);}
}
