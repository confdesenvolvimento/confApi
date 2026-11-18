package com.confApi.plantao;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class LegacyPlantaoTokenServiceTest {
    static final String REF = "r".repeat(43);
    @BeforeEach void setup() { System.setProperty("AUTH_V2_ENABLED", "false"); }
    @AfterEach void cleanup() { System.clearProperty("AUTH_V2_ENABLED"); }
    @Test void retornaSoContextoEConsomeUmaVezPreservandoZeros() {
        var service = new Fake(); var response = service.gerar(request(REF));
        assertTrue(response.success()); assertNull(response.token());
        assertEquals(service.clock.plusSeconds(300), Instant.parse(response.expiresAt()));
        var validation = validation(response, "0007");
        assertTrue(service.validar(validation).success()); assertFalse(service.validar(validation).success());
        assertEquals(1, service.consumes); assertEquals("0007", service.lastCode);
    }
    @Test void repeticaoDaReferenciaNaoReemiteNemEstendePrazo() {
        var service = new Fake(); var first = service.gerar(request(REF)); service.clock=service.clock.plusSeconds(30);
        assertEquals(first, service.gerar(request(REF))); assertEquals(1, service.generates);
    }
    @Test void tresFalhasBloqueiamCodigoCorretoSemChamarCservice() {
        var service = new Fake(); var response = service.gerar(request(REF));
        for (int i=0;i<3;i++) assertFalse(service.validar(validation(response,"9999")).success());
        assertFalse(service.validar(validation(response,"0007")).success()); assertEquals(0,service.consumes);
    }
    @Test void recusaOutroSolicitanteLoginCanalReferenciaEExpiracao() {
        var service = new Fake(); var response = service.gerar(request(REF));
        service.owner="outro"; assertFalse(service.validar(validation(response,"0007")).success()); service.owner="portal";
        assertFalse(service.validar(new PlantaoTokenValidationRequest("bob","0007","APP",response.challengeId(),REF)).success());
        assertFalse(service.validar(new PlantaoTokenValidationRequest("alice","0007","EMAIL",response.challengeId(),REF)).success());
        assertFalse(service.validar(new PlantaoTokenValidationRequest("alice","0007","APP",response.challengeId(),"x".repeat(43))).success());
        service.clock=service.clock.plusSeconds(300);
        assertFalse(service.validar(validation(response,"0007")).success()); assertEquals(0,service.consumes);
    }
    @Test void novaReferenciaInvalidaAnteriorEMantemLimitePorUsuario() {
        var service = new Fake(); var old=service.gerar(request(REF));
        for (String ref:List.of("b".repeat(43),"c".repeat(43))) assertTrue(service.gerar(request(ref)).success());
        assertFalse(service.validar(validation(old,"0007")).success());
        assertFalse(service.gerar(request("d".repeat(43))).success()); assertEquals(3,service.generates);
    }
    @Test void semIdentidadeOuComV2LigadoNaoUsaLegado() {
        var service = new Fake(); service.owner=null; assertFalse(service.gerar(request(REF)).success());
        service.owner="portal"; System.setProperty("AUTH_V2_ENABLED","true");
        assertFalse(service.gerar(request(REF)).success()); assertEquals(0,service.generates);
    }
    @Test void indisponibilidadeNaoViraSucessoENaoRepeteConsumo() {
        var service = new Fake(); service.generated="resposta-invalida"; assertFalse(service.gerar(request(REF)).success());
        service.generated="0007"; var response=service.gerar(request(REF)); service.consumeResult=false;
        assertFalse(service.validar(validation(response,"0007")).success());
        assertFalse(service.validar(validation(response,"0007")).success()); assertEquals(1,service.consumes);
    }
    @Test void validacoesConcorrentesSoConsomemUmaVez() throws Exception {
        var service=new Fake(); var request=validation(service.gerar(request(REF)),"0007");
        var executor=Executors.newFixedThreadPool(2);
        try { var results=executor.invokeAll(List.of(()->service.validar(request).success(),()->service.validar(request).success()));
            int ok=0; for(var result:results) if(Boolean.TRUE.equals(result.get()))ok++;
            assertEquals(1,ok); assertEquals(1,service.consumes);
        } finally {executor.shutdownNow();}
    }
    @Test void transportePreservaContratoDoCserviceSemConfiarEmTelefoneDoCliente() {
        var http=new RestTemplate(); var builder=mock(RestTemplateBuilder.class,RETURNS_SELF); when(builder.build()).thenReturn(http);
        var service=new LegacyPlantaoTokenService(builder,"https://tokens.example.test/",new LegacyPlantaoTokenDecoder("synthetic")) {
            @Override protected String caller(){return "portal";}
        };
        var server=MockRestServiceServer.createServer(http);
        server.expect(requestTo("https://tokens.example.test/api/token/gerartoken/0003PLANTAO/alice"))
                .andExpect(method(HttpMethod.GET)).andRespond(withSuccess("{\"valor\":\"0007\"}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://tokens.example.test/api/token/validar/7/0003PLANTAO"))
                .andExpect(method(HttpMethod.POST)).andExpect(content().json("{\"usuarioLoginAcesso\":\"alice\"}"))
                .andRespond(withSuccess("Token válido",MediaType.parseMediaType("text/plain;charset=UTF-8")));
        var response=service.gerar(new PlantaoTokenRequest("alice","EMAIL","numero-nao-confiavel",REF));
        assertTrue(response.success()); assertNull(response.token());
        assertTrue(service.validar(new PlantaoTokenValidationRequest("alice","0007","EMAIL",response.challengeId(),REF)).success());
        server.verify();
    }
    @Test void decoderAceitaCifraAnteriorMasRecusaChaveErradaECodigoInvalido() throws Exception {
        var cipher=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding"); byte[] iv=new byte[12];
        byte[] key=java.security.MessageDigest.getInstance("SHA-256").digest("synthetic".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE,new javax.crypto.spec.SecretKeySpec(key,"AES"),new javax.crypto.spec.GCMParameterSpec(128,iv));
        byte[] encrypted=cipher.doFinal("0007".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        byte[] payload=new byte[iv.length+encrypted.length];System.arraycopy(encrypted,0,payload,iv.length,encrypted.length);
        String encoded=Base64.getEncoder().encodeToString(payload);
        assertEquals("0007",new LegacyPlantaoTokenDecoder("synthetic").decode(encoded));
        assertThrows(IllegalStateException.class,()->new LegacyPlantaoTokenDecoder("other").decode(encoded));
        for(String value:List.of("123456","","corrompido"))assertThrows(IllegalStateException.class,()->new LegacyPlantaoTokenDecoder("synthetic").decode(value));
    }
    static PlantaoTokenRequest request(String reference){return new PlantaoTokenRequest("alice","APP",null,reference);}
    static PlantaoTokenValidationRequest validation(PlantaoTokenResponse r,String code){return new PlantaoTokenValidationRequest("alice",code,"APP",r.challengeId(),r.reference());}
    static class Fake extends LegacyPlantaoTokenService {
        Instant clock=Instant.parse("2026-11-16T12:00:00Z");String owner="portal",generated="0007",lastCode;int generates,consumes;boolean consumeResult=true;
        Fake(){super(new RestTemplateBuilder(),"https://unused.example.test",new LegacyPlantaoTokenDecoder("synthetic"));}
        @Override protected Instant now(){return clock;}
        @Override protected String caller(){return owner;}
        @Override protected String generate(String login,String channel){generates++;return generated;}
        @Override protected boolean consume(String login,String channel,String code){consumes++;lastCode=code;return consumeResult;}
    }
}
