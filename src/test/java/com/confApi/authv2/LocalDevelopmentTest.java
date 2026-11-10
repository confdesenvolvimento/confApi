package com.confApi.authv2;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;

class LocalDevelopmentTest {
    static final String LOCAL="http://192.168.50.4:8888";
    LocalDevelopment policy(boolean dev, String... profiles) {
        var env = new MockEnvironment().withProperty("app.dev", Boolean.toString(dev))
            .withProperty("app.dev.allowed-origins", LOCAL);
        env.setActiveProfiles(profiles);
        return new LocalDevelopment(env);
    }
    @Test void explicitLocalProfileAllowsOnlyConfiguredPrivateOrigin() {
        var p=policy(true,"dev");
        assertTrue(p.allows(URI.create(LOCAL+"/api/v2/tokens")));
        for(String value: new String[]{"http://192.168.50.4:8889", "http://192.168.50.5:8888", "http://example.test:8888", "http://8.8.8.8:8888", LOCAL+"?a=b", "http://user:pass@192.168.50.4:8888"})
            assertFalse(p.allows(URI.create(value)));
    }
    @Test void falseFlagMissingProfileAndProductionKeepHttpDisabled() {
        assertFalse(policy(false,"dev").allows(URI.create(LOCAL)));
        assertFalse(policy(true).allows(URI.create(LOCAL)));
        assertFalse(policy(true,"prod").allows(URI.create(LOCAL)));
        assertFalse(policy(true,"dev","prod").allows(URI.create(LOCAL)));
        assertFalse(policy(true,"dev","production").allows(URI.create(LOCAL)));
    }
    @Test void clientHeadersNeverEnableDevelopmentAndPublicPeersAreRejected() {
        var request=new MockHttpServletRequest("POST","/api/v2/tokens");
        request.setServerName("192.168.50.4"); request.setServerPort(8888);
        request.setLocalAddr("192.168.50.4"); request.setRemoteAddr("192.168.50.6");
        request.addHeader("dev","true"); request.addHeader("X-Forwarded-Proto","https");
        assertFalse(policy(false,"dev").allows(request));
        assertTrue(policy(true,"dev").allows(request));
        request.setRemoteAddr("8.8.8.8"); assertFalse(policy(true,"dev").allows(request));
    }
    @Test void noOriginsFailsClosedAndPublicOriginsCannotBeConfigured() {
        var env=new MockEnvironment().withProperty("app.dev","true"); env.setActiveProfiles("local");
        assertFalse(new LocalDevelopment(env).allows(URI.create(LOCAL)));
        env.setProperty("app.dev.allowed-origins","http://example.test");
        assertThrows(IllegalStateException.class,()->new LocalDevelopment(env));
    }
}
