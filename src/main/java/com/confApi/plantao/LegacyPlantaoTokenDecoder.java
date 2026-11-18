package com.confApi.plantao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Compatibilidade do cService anterior; o código nunca sai do backend para o portal. */
@Component
public class LegacyPlantaoTokenDecoder {
    private final String secret;
    public LegacyPlantaoTokenDecoder(@Value("${cservices.token.hash-secret:${CSERVICES_TOKEN_HASH_SECRET:cServices2-token-hash-dev}}") String secret) {
        this.secret = secret;
    }
    public String decode(String value) {
        if (value != null && value.matches("[0-9]{4}")) return value;
        try {
            if (secret == null || secret.isBlank()) throw new IllegalArgumentException();
            byte[] payload = Base64.getDecoder().decode(value);
            if (payload.length <= 28) throw new IllegalArgumentException();
            byte[] key = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, Arrays.copyOfRange(payload, 0, 12)));
            String code = new String(cipher.doFinal(Arrays.copyOfRange(payload, 12, payload.length)), StandardCharsets.UTF_8);
            if (!code.matches("[0-9]{4}")) throw new IllegalArgumentException();
            return code;
        } catch (Exception invalid) { throw new IllegalStateException("Resposta de token inválida."); }
    }
}
