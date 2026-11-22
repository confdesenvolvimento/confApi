package com.confApi.mobile;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/** Verifica a prova do aparelho; apenas a chave publica e hashes chegam ao banco. */
@Component
public class MobileProofs {
    private final MobileSessionRepository repository;
    private final MobileSettings settings;
    public record PublicKeyData(String publicJwk,String thumbprint) {}
    public MobileProofs(MobileSessionRepository repository,MobileSettings settings){this.repository=repository;this.settings=settings;}
    public PublicKeyData validatedKey(String input) {
        try {
            if(input==null||input.length()>4096)throw invalid();
            JWK parsed=JWK.parse(input);
            if(!(parsed instanceof ECKey key)||key.isPrivate()||!Curve.P_256.equals(key.getCurve()))throw invalid();
            key.toECPublicKey();
            ECKey canonical=new ECKey.Builder(Curve.P_256,key.getX(),key.getY()).build();
            return new PublicKeyData(canonical.toJSONString(),canonical.computeThumbprint().toString());
        }catch(MobileFailure failure){throw failure;}catch(Exception failure){throw invalid();}
    }
    public void verify(MobileSessionRepository.DurableSession session,String token,String proof,String method,String expectedPublicUri) {
        String replayHash;
        Instant now=Instant.now();
        try {
            if(session==null||!MobileSessionRepository.ACTIVE.equals(session.state())||token==null||!token.matches("[A-Za-z0-9_-]{43}")
                ||proof==null||proof.length()>8192||method==null||!method.matches("[A-Z]{3,10}"))throw invalid();
            var binding=validatedKey(session.publicJwk());
            if(!same(binding.thumbprint(),session.thumbprint()))throw invalid();
            ECKey key=ECKey.parse(binding.publicJwk());
            var jwt=SignedJWT.parse(proof);var header=jwt.getHeader();var claims=jwt.getJWTClaimsSet();
            if(!JWSAlgorithm.ES256.equals(header.getAlgorithm())||!new JOSEObjectType("dpop+jwt").equals(header.getType())
                ||header.getCriticalParams()!=null||header.getJWK()==null||header.getJWK().isPrivate()||header.getJWKURL()!=null||header.getX509CertURL()!=null
                ||!same(binding.thumbprint(),header.getJWK().computeThumbprint().toString())||!jwt.verify(new ECDSAVerifier(key)))throw invalid();
            String expected=publicUri(expectedPublicUri);
            if(claims.getIssueTime()==null||claims.getIssueTime().toInstant().isBefore(now.minusSeconds(60))||claims.getIssueTime().toInstant().isAfter(now.plusSeconds(5))
                ||!method.equals(claims.getStringClaim("htm"))||!expected.equals(claims.getStringClaim("htu"))||!same(session.nonce(),claims.getStringClaim("nonce"))
                ||!same(tokenHash(token),claims.getStringClaim("ath"))||claims.getJWTID()==null||!claims.getJWTID().matches("[A-Za-z0-9_-]{16,128}"))throw invalid();
            replayHash=hexHash("confapi:dpop:v1:"+binding.thumbprint()+":"+claims.getJWTID());
        }catch(MobileFailure failure){throw failure;}catch(Exception failure){throw invalid();}
        // Fora do bloco de validacao: indisponibilidade do banco permanece503, nunca401.
        if(!repository.recordProof(replayHash,now.plusSeconds(120)))throw new MobileFailure(401,"PROVA_REUTILIZADA");
    }
    private String publicUri(String input) {
        URI uri=URI.create(input);
        boolean local="http".equals(uri.getScheme())&&settings.development()&&MobileSettings.local(uri.getHost());
        if((!"https".equals(uri.getScheme())&&!local)||uri.getHost()==null||uri.getUserInfo()!=null||uri.getFragment()!=null)throw invalid();
        // O caminho publico vem da configuracao confiavel do gateway, nunca do cabecalhoHost do cliente.
        String path=uri.getRawPath().isEmpty()?"/":uri.getRawPath();
        return uri.getScheme()+"://"+uri.getRawAuthority()+path;
    }
    public static String tokenHash(String token){return Base64.getUrlEncoder().withoutPadding().encodeToString(digest(token));}
    public static String hexHash(String value){return HexFormat.of().formatHex(digest(value));}
    private static byte[] digest(String value){try{return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));}catch(Exception error){throw new IllegalStateException(error);}}
    private static boolean same(String left,String right){return left!=null&&right!=null&&MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8),right.getBytes(StandardCharsets.UTF_8));}
    private static MobileFailure invalid(){return new MobileFailure(401,"PROVA_INVALIDA");}
}
