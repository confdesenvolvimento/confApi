package com.confApi.mobile;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MobilePersistenceApiCompatibilityTest {
    MobileSessions sessions;MobileUpstream upstream;MobileApi api;MobileSessions.Session actor;
    @BeforeEach void setup() {
        sessions=mock(MobileSessions.class);
        var settings=new MobileSettings(MobileUpstream.JSON.valueToTree(Map.of("services",Map.of("confapi","https://fixture.invalid/confApi4/"))));
        upstream=spy(new MobileUpstream(settings));api=new MobileApi(sessions,upstream,mock(MobileChatDelegations.class));
        actor=new MobileSessions.Session("fixture.login","00000000-0000-4000-8000-000000000001",MobileUpstream.JSON.valueToTree(Map.of("id",42)),true);
        when(sessions.require(any(),any(),any(),any())).thenReturn(actor);
    }
    MockHttpServletRequest request(String method,String path){
        var r=new MockHttpServletRequest(method,"/confApi4/api/mobile/"+path);r.setContextPath("/confApi4");
        r.setServerName("untrusted.invalid");r.addHeader("X-Forwarded-Host","untrusted.invalid");
        r.addHeader("Authorization","Bearer fixture");r.addHeader("DPoP","fixture-proof");return r;
    }
    @Test void oldAppLoginBodyDoesNotRequireAKey() {
        JsonNode body=MobileUpstream.JSON.valueToTree(Map.of("login","fixture.login","password","fixture-password","deviceId",actor.device));
        api.login(body,request("POST","entrar"));
        verify(sessions).start(eq("fixture.login"),eq("fixture-password"),eq(actor.device),anyString(),isNull());
    }
    @Test void newAppKeyIsPassedWithoutChangingTheLoginContract() {
        var body=MobileUpstream.JSON.createObjectNode();body.put("login","fixture.login");body.put("deviceId",actor.device);var key=body.putObject("publicJwk");key.put("kty","EC");
        api.identify(body,request("POST","identificar"));
        verify(sessions).start(eq("fixture.login"),isNull(),eq(actor.device),anyString(),eq(key));
    }
    @Test void publicProofUriNeverUsesUntrustedHostHeaders(){
        api.profile(request("GET","perfil"));
        verify(sessions).require("Bearer fixture","fixture-proof","GET","https://fixture.invalid/confApi4/api/mobile/perfil");
    }
    @Test void canonicalLoginAfterRestartAcceptsTheSameLoginWithDifferentCase()throws Exception {
        var device=MobileUpstream.JSON.valueToTree(Map.of("autorizado",true));when(sessions.device(actor)).thenReturn(device);
        assertEquals(device,api.service("manager",request("GET","services/manager/dispositivo/login/FIXTURE.LOGIN")).getBody());
        var profile=MobileUpstream.JSON.valueToTree(Map.of("codgUsuario",42,"senha","private"));
        doReturn(profile).when(upstream).call("manager","usuario/findByLogin/fixture.login","GET",null);
        var result=(JsonNode)api.service("manager",request("GET","services/manager/usuario/findByLogin/FIXTURE.LOGIN")).getBody();
        assertEquals(42,result.path("codgUsuario").asInt());assertFalse(result.has("senha"));verify(sessions).requireActive(actor);
    }
    @Test void caseInsensitiveLoginMatchingStillRejectsAnotherUser() {
        assertThrows(MobileFailure.class,()->api.service("manager",request("GET","services/manager/dispositivo/login/someone.else")));
        verify(sessions,never()).device(any());
    }
}
