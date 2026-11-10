package com.confApi.mobile;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MobileTokenResponseTest {
    private static final String KEY="000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f";
    private static final String ENCRYPTED="EBESExQVFhcYGRobTc6sJL/zQ6VQQSQPzDgIzeqzK7w=";
    private MobileSettings settings(String key) {
        return new MobileSettings(MobileUpstream.JSON.valueToTree(Map.of("cserviceTokenResponseKey",key)));
    }
    @Test void decryptsKnownAppFixturePreservingTrackingAndLeadingZeros() {
        var original=MobileUpstream.JSON.valueToTree(Map.of("valor",ENCRYPTED,"trackingToken","tracking","status","ATIVO","sequencia",7));
        var result=MobileTokenResponse.decode(original,settings(KEY));
        assertEquals("0042",result.path("valor").asText());
        assertEquals("tracking",result.path("trackingToken").asText());
        assertEquals("ATIVO",result.path("status").asText());
        assertEquals(7,result.path("sequencia").asInt());
        assertEquals(ENCRYPTED,original.path("valor").asText());
        assertFalse(result.toString().contains(KEY));
    }
    @Test void plainCodesNeedNoSharedKey() {
        for(Object value:new Object[]{"0042","0000","1234",1234}) {
            var result=MobileTokenResponse.decode(MobileUpstream.JSON.valueToTree(Map.of("valor",value)),settings(""));
            assertEquals(value.toString(),result.path("valor").asText());
            assertTrue(result.path("valor").isTextual());
        }
    }
    @Test void missingOrInvalidKeyFailsClosed() {
        for(String key:new String[]{"","invalid"}) {
            var failure=assertThrows(MobileFailure.class,()->MobileTokenResponse.decode(MobileUpstream.JSON.valueToTree(Map.of("valor",ENCRYPTED)),settings(key)));
            assertEquals(503,failure.status);
        }
    }
    @Test void wrongKeyOrTamperingNeverReturnsCiphertextOrSecrets() {
        for(String value:new String[]{ENCRYPTED,"EBESExQVFhcYGRobTc6sJL/zQ6VQQSQPzDgIzeqzK7A="}) {
            var failure=assertThrows(MobileFailure.class,()->MobileTokenResponse.decode(MobileUpstream.JSON.valueToTree(Map.of("valor",value)),settings("ff".repeat(32))));
            assertEquals(502,failure.status);
            assertEquals("RESPOSTA_TOKEN_INVALIDA",failure.getMessage());
        }
    }
    @Test void malformedOrMissingCodesAreRejected() {
        for(Object value:new Object[]{"123","12345","abcd",123,12.34,true}) {
            assertThrows(MobileFailure.class,()->MobileTokenResponse.decode(MobileUpstream.JSON.valueToTree(Map.of("valor",value)),settings(KEY)));
        }
        assertThrows(MobileFailure.class,()->MobileTokenResponse.decode(MobileUpstream.JSON.createObjectNode(),settings(KEY)));
        assertThrows(MobileFailure.class,()->MobileTokenResponse.decode(MobileUpstream.JSON.createArrayNode(),settings(KEY)));
    }
}
