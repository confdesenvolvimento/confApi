package com.confApi.mobile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.*;
import java.time.Instant;
import java.util.*;
import static com.confApi.mobile.MobilePersistenceFixture.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MobileProofsTest {
    MobilePersistenceFixture f;MobileProofs proofs;MobileSettings settings;
    final String uri="https://fixture.invalid/confApi4/mobile/perfil";
    @BeforeEach void setup() throws Exception {
        f=new MobilePersistenceFixture();settings=new MobileSettings(new ObjectMapper().createObjectNode());proofs=new MobileProofs(f.repository,settings);
    }
    String proof(Map<String,Object> overrides,ECKey key) throws Exception {
        var claims=new JWTClaimsSet.Builder().issueTime(Date.from(Instant.now())).jwtID(UUID.randomUUID().toString())
            .claim("htm","GET").claim("htu",uri).claim("nonce",f.row().nonce()).claim("ath",MobileProofs.tokenHash(f.access));
        overrides.forEach(claims::claim);
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(new JOSEObjectType("dpop+jwt")).jwk(key.toPublicJWK()).build(),claims.build());
        jwt.sign(new ECDSASigner(key));return jwt.serialize();
    }
    @Test void publicKeyIsValidatedAndCanonicalized() throws Exception {
        var key=proofs.validatedKey(f.key.toPublicJWK().toJSONString());
        assertEquals(f.key.computeThumbprint().toString(),key.thumbprint());assertFalse(JWK.parse(key.publicJwk()).isPrivate());
        assertEquals(401,assertThrows(MobileFailure.class,()->proofs.validatedKey(f.key.toJSONString())).status);
        assertEquals(401,assertThrows(MobileFailure.class,()->proofs.validatedKey(new ECKeyGenerator(Curve.P_384).generate().toPublicJWK().toJSONString())).status);
        assertEquals(401,assertThrows(MobileFailure.class,()->proofs.validatedKey("{}" )).status);
    }
    @Test void validProofIsAcceptedOnceAcrossInstances() throws Exception {
        String proof=proof(Map.of(),f.key);proofs.verify(f.row(),f.access,proof,"GET",uri);
        assertEquals(1,f.count("autenticacao_controle"));
        assertEquals(401,assertThrows(MobileFailure.class,()->new MobileProofs(new MobileSessionRepository(f.source),settings).verify(f.row(),f.access,proof,"GET",uri)).status);
    }
    @Test void claimsMustBindMethodPathNonceTokenTimeAndUniqueId() throws Exception {
        for(Map<String,Object> invalid:List.<Map<String,Object>>of(Map.of("htm","POST"),Map.of("htu",uri+"/other"),Map.of("nonce",token("other-nonce")),
            Map.of("ath",hash(f.access)),Map.of("ath",MobileProofs.tokenHash("other-token")),Map.of("iat",Date.from(Instant.now().minusSeconds(90))),
            Map.of("iat",Date.from(Instant.now().plusSeconds(30))),Map.of("jti","short"))) {
            String bad=proof(invalid,f.key);assertEquals(401,assertThrows(MobileFailure.class,()->proofs.verify(f.row(),f.access,bad,"GET",uri)).status);
        }
        assertEquals(0,f.count("autenticacao_controle"));
    }
    @Test void differentKeyAndModifiedSignatureAreRejected() throws Exception {
        String different=proof(Map.of(),new ECKeyGenerator(Curve.P_256).generate());
        assertEquals(401,assertThrows(MobileFailure.class,()->proofs.verify(f.row(),f.access,different,"GET",uri)).status);
        String valid=proof(Map.of(),f.key);String[] pieces=valid.split("\\.");String damaged=pieces[0]+"."+pieces[1]+"."+(pieces[2].charAt(0)=='A'?"B":"A")+pieces[2].substring(1);
        assertEquals(401,assertThrows(MobileFailure.class,()->proofs.verify(f.row(),f.access,damaged,"GET",uri)).status);
        assertEquals(0,f.count("autenticacao_controle"));
    }
    @Test void athUsesBase64UrlSha256AndStorageUsesHexHash() {
        assertEquals(43,MobileProofs.tokenHash(f.access).length());assertEquals(64,hash(f.access).length());assertNotEquals(MobileProofs.tokenHash(f.access),hash(f.access));
    }
    @Test void expectedPublicUriExcludesQuery() throws Exception {
        proofs.verify(f.row(),f.access,proof(Map.of(),f.key),"GET",uri+"?operation=test");
        assertEquals(1,f.count("autenticacao_controle"));
    }
    @Test void httpPrivateUriRequiresExplicitDevelopmentMode() throws Exception {
        String local="http://127.0.0.1:18088/mobile/perfil";String proof=proof(Map.of("htu",local),f.key);
        assertEquals(401,assertThrows(MobileFailure.class,()->proofs.verify(f.row(),f.access,proof,"GET",local)).status);
        var development=new MobileSettings(new ObjectMapper().createObjectNode().put("development",true));
        new MobileProofs(f.repository,development).verify(f.row(),f.access,proof,"GET",local);
    }
    @Test void replayStoreOutageIs503NotBadCredentials() throws Exception {
        var unavailable=mock(MobileSessionRepository.class);when(unavailable.recordProof(anyString(),any())).thenThrow(new MobileFailure(503,"PERSISTENCIA_INDISPONIVEL"));
        String proof=proof(Map.of(),f.key);
        assertEquals(503,assertThrows(MobileFailure.class,()->new MobileProofs(unavailable,settings).verify(f.row(),f.access,proof,"GET",uri)).status);
    }
    @Test void replayNamespaceDoesNotCollideWithV2KeyDerivation() throws Exception {
        String jti="fixture-replay-identifier";String proof=proof(Map.of("jti",jti),f.key);proofs.verify(f.row(),f.access,proof,"GET",uri);
        String stored=f.jdbc.queryForObject("SELECT key_hash FROM ConfiancaManger.autenticacao_controle WHERE kind='REPLAY'",String.class);
        assertEquals(hash("confapi:dpop:v1:"+f.key.computeThumbprint()+":"+jti),stored);
    }
}
