package com.confApi.plantao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/** Decodes CService response values only when a numeric code is needed for SMS. */
@Component
public class CserviceTokenValueDecoder {
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private final String hashSecret;

    public CserviceTokenValueDecoder(
            @Value("${cservices.token.hash-secret:${CSERVICES_TOKEN_HASH_SECRET:cServices2-token-hash-dev}}") String hashSecret) {
        this.hashSecret = hashSecret;
    }

    public String codigoParaEnvio(String value) {
        // Allows CService and ConfApi to be deployed in either order.
        if (value != null && value.matches("[0-9]{4}")) return value;
        try {
            if (value == null || hashSecret == null || hashSecret.isBlank()) throw new IllegalArgumentException();
            byte[] payload = Base64.getDecoder().decode(value);
            if (payload.length <= IV_BYTES + TAG_BITS / 8) throw new IllegalArgumentException();
            byte[] key = MessageDigest.getInstance("SHA-256").digest(hashSecret.getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_BITS, Arrays.copyOfRange(payload, 0, IV_BYTES)));
            String code = new String(cipher.doFinal(Arrays.copyOfRange(payload, IV_BYTES, payload.length)),
                    StandardCharsets.UTF_8);
            if (!code.matches("[0-9]{4}")) throw new IllegalArgumentException();
            return code;
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Nao foi possivel descriptografar o token do CService");
        }
    }
}
