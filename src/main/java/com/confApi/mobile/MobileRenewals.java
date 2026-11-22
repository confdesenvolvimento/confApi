package com.confApi.mobile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/** Recover one exact rotation without storing plaintext tokens or a new server secret. */
final class MobileRenewals {
    static final Duration RECOVERY=Duration.ofMinutes(2);
    record Tokens(String access,String refresh,String receipt) {}
    static void validateId(String id) {
        if(id!=null&&!id.matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"))
            throw new MobileFailure(400,"RENOVACAO_INVALIDA");
    }
    static Tokens derive(String refresh,String sessionId,String id) {
        if(id==null)return null;
        validateId(id);
        String context=sessionId+":"+id;
        // The old 256-bit refresh is the HMAC key. A separate persisted random operation ID
        // and distinct labels bind the two successors to this exact rotation. DPoP is mandatory.
        return new Tokens(prf(refresh,"access:"+context),prf(refresh,"refresh:"+context),
            MobileProofs.hexHash("confapi:renewal-receipt:v1:"+context+":"+MobileProofs.hexHash(refresh)));
    }
    private static String prf(String token,String context) {
        try {
            Mac mac=Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(Base64.getUrlDecoder().decode(token),"HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(("confapi:renewal:v1:"+context).getBytes(StandardCharsets.UTF_8)));
        } catch(Exception failure){throw new IllegalStateException("Unable to derive renewal tokens",failure);}
    }
}
