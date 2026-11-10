package com.confApi.mobile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

/** Decodes only an authorized user's generated code; the shared key stays on the server. */
final class MobileTokenResponse {
    private MobileTokenResponse() {}

    static JsonNode decode(JsonNode response, MobileSettings settings) {
        if (response == null || !response.isObject()) throw invalid();
        JsonNode value = response.path("valor");
        String code = value.asText("");
        if (!(value.isTextual() || value.isIntegralNumber()) || !code.matches("[0-9]{4}")) {
            if (!value.isTextual() || !code.matches("[A-Za-z0-9+/]{43}=")) throw invalid();
            String hex = settings.secret("cserviceTokenResponseKey");
            if (!hex.matches("[0-9a-fA-F]{64}")) throw new MobileFailure(503,"CONFIGURACAO_INDISPONIVEL");
            byte[] key = HexFormat.of().parseHex(hex);
            byte[] plain = null;
            try {
                byte[] payload = Base64.getDecoder().decode(code);
                if (payload.length != 32 || !Base64.getEncoder().encodeToString(payload).equals(code)) throw invalid();
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key,"AES"),
                        new GCMParameterSpec(128,Arrays.copyOfRange(payload,0,12)));
                plain = cipher.doFinal(payload,12,payload.length-12);
                code = new String(plain,StandardCharsets.US_ASCII);
                if (!code.matches("[0-9]{4}")) throw invalid();
            } catch (Exception failure) {
                throw invalid();
            } finally {
                Arrays.fill(key,(byte)0);
                if (plain != null) Arrays.fill(plain,(byte)0);
            }
        }
        ObjectNode result = response.deepCopy();
        result.put("valor",code);
        return result;
    }

    private static MobileFailure invalid() { return new MobileFailure(502,"RESPOSTA_TOKEN_INVALIDA"); }
}
