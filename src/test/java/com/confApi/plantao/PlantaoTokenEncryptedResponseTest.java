package com.confApi.plantao;

import com.confApi.config.UrlConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PlantaoTokenEncryptedResponseTest {
    private static final String SECRET = "test-only-cservice-secret";
    private RestTemplate rest;
    private MockRestServiceServer server;
    private PlantaoTokenService service;
    private String oldManager;
    private String oldHub;

    @BeforeEach
    void setup() {
        oldManager = UrlConfig.URL_CONFIANCA_MANAGER;
        oldHub = UrlConfig.URL_CONFIANCA_HUB;
        UrlConfig.URL_CONFIANCA_MANAGER = "https://manager.test/";
        UrlConfig.URL_CONFIANCA_HUB = "https://hub.test/";
        rest = new RestTemplate();
        server = MockRestServiceServer.createServer(rest);
        service = new PlantaoTokenService(rest, "https://cservice.test/", new CserviceTokenValueDecoder(SECRET));
    }

    @AfterEach
    void restoreUrls() {
        UrlConfig.URL_CONFIANCA_MANAGER = oldManager;
        UrlConfig.URL_CONFIANCA_HUB = oldHub;
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void emailSendsNumericCodeAndPreservesUpstreamResponse(boolean encrypted) throws Exception {
        String value = encrypted ? encrypt("0042", SECRET) : "0042";
        expectToken("0003PLANTAO", value);
        server.expect(requestTo("https://manager.test/wooba/turUsuarios/loginCservice/usuario.teste"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"celular\":\"5565999999999\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://hub.test/menssage"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer fixture"))
                .andExpect(jsonPath("$.to").value("5565999999999"))
                .andExpect(jsonPath("$.contents[0].text").value("Seu token de plantao e: 0042"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        PlantaoTokenResponse result = service.gerar(request("EMAIL"), "Bearer fixture");
        assertTrue(result.success());
        assertEquals(value, result.token());
        server.verify();
    }

    @Test
    void appKeepsCiphertextWithoutRequiringSmsDecryption() throws Exception {
        String value = encrypt("0042", SECRET);
        expectToken("0004PLANTAO", value);
        var appService = new PlantaoTokenService(rest, "https://cservice.test/", new CserviceTokenValueDecoder(""));
        PlantaoTokenResponse result = appService.gerar(request("APP"), null);
        assertTrue(result.success());
        assertEquals(value, result.token());
        server.verify();
    }

    @Test
    void wrongSecretFailsBeforeSendingSms() throws Exception {
        expectToken("0003PLANTAO", encrypt("0042", "different-secret"));
        PlantaoTokenResponse result = service.gerar(request("EMAIL"), null);
        assertFalse(result.success());
        assertNull(result.token());
        assertFalse(result.message().contains("0042"));
        server.verify();
    }

    @Test
    void tamperedCiphertextFailsBeforeSendingSms() throws Exception {
        byte[] payload = Base64.getDecoder().decode(encrypt("0042", SECRET));
        payload[12] ^= 1;
        expectToken("0003PLANTAO", Base64.getEncoder().encodeToString(payload));
        PlantaoTokenResponse result = service.gerar(request("EMAIL"), null);
        assertFalse(result.success());
        assertNull(result.token());
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-base64", "AA==", "", "12345"})
    void malformedCodeCannotBeSent(String value) {
        expectToken("0003PLANTAO", value);
        assertFalse(service.gerar(request("EMAIL"), null).success());
        server.verify();
    }

    @Test
    void decryptedNonNumericContentIsRejected() throws Exception {
        expectToken("0003PLANTAO", encrypt("abcd", SECRET));
        assertFalse(service.gerar(request("EMAIL"), null).success());
        server.verify();
    }

    @Test
    void validationStillSendsUserEnteredNumber() {
        server.expect(requestTo("https://cservice.test/api/token/validar/42/0003PLANTAO"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.usuarioLoginAcesso").value("usuario.teste"))
                .andRespond(withSuccess("Token valido", MediaType.TEXT_PLAIN));
        assertTrue(service.validar(new PlantaoTokenValidationRequest("usuario.teste", "0042", "EMAIL")).success());
        server.verify();
    }

    @Test
    void ciphertextIsNotAcceptedAsUserEnteredToken() throws Exception {
        assertFalse(service.validar(new PlantaoTokenValidationRequest("usuario.teste",
                encrypt("0042", SECRET), "EMAIL")).success());
        server.verify();
    }

    private PlantaoTokenRequest request(String channel) {
        return new PlantaoTokenRequest("usuario.teste", channel, "5565999999999");
    }

    private void expectToken(String key, String value) {
        server.expect(requestTo("https://cservice.test/api/token/gerartoken/" + key + "/usuario.teste"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"valor\":\"" + value + "\"}", MediaType.APPLICATION_JSON));
    }

    // Independent producer fixture using the exact CService database/response format.
    private String encrypt(String code, String secret) throws Exception {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        byte[] key = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(code.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                .put(iv).put(encrypted).array());
    }
}
