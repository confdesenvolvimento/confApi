package com.confApi.mobile;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MobileRateLimitCapacityTest {
    @Test void threeThousandUsersCanConfigureLoginAndRenewWithoutExceedingIndividualLimits() {
        var sessions=new MobileSessions(new MobileSessionAvailabilityTest.Upstream());
        for(int i=0;i<3000;i++){
            String login="fixture.capacity."+i;
            String peer="198.51."+(i/250)+"."+(i%250+1);
            // Mesma sequencia de limites usada pelo MobileApi e pelo aplicativo.
            sessions.limit("config:"+peer,120,60);
            var started=sessions.start(login,"fixture-password",MobileSessionAvailabilityTest.DEVICE,peer);
            sessions.limit("renew:"+peer,120,60);
            var renewed=sessions.renew(started.get("refreshToken").toString());
            assertEquals(login,sessions.require("Bearer "+renewed.get("accessToken")).login);
        }
    }

    @Test void largerCounterCapacityDoesNotRelaxIndividualLoginLimits() {
        var sessions=new MobileSessions(new MobileSessionAvailabilityTest.Upstream());
        for(int i=0;i<30;i++)sessions.start("fixture.ip."+i,null,MobileSessionAvailabilityTest.DEVICE,"192.0.2.1");
        var ip=assertThrows(MobileFailure.class,()->sessions.start("fixture.extra",null,MobileSessionAvailabilityTest.DEVICE,"192.0.2.1"));
        assertEquals(429,ip.status);
        for(int i=0;i<10;i++)sessions.start("fixture.user",null,MobileSessionAvailabilityTest.DEVICE,"198.51.100."+(i+1));
        var user=assertThrows(MobileFailure.class,()->sessions.start("fixture.user",null,MobileSessionAvailabilityTest.DEVICE,"198.51.100.99"));
        assertEquals(429,user.status);
    }
}
