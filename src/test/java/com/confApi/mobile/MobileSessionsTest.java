package com.confApi.mobile;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MobileSessionsTest {
    final String device="00000000-0000-4000-8000-000000000001";
    MobileUpstream upstream;MobileSessions sessions;
    JsonNode profile;
    @BeforeEach void setUp() throws Exception {
        upstream=mock(MobileUpstream.class);sessions=new MobileSessions(upstream);
        profile=MobileUpstream.JSON.readTree("{\"id\":42,\"status\":1,\"login\":\"fixture\",\"celular\":\"65999990000\",\"email\":\"fixture@example.test\",\"senhaTemporaria\":1,\"chaveToken\":\"private-upstream-token\",\"senha\":\"private-password\"}");
        when(upstream.call(eq("manager"),eq("usuario/auth"),eq("POST"),any())).thenReturn(profile);
        when(upstream.call("manager","wooba/turUsuarios/fixture","POST",null)).thenReturn(profile);
        when(upstream.call("manager","usuario/findByLogin/fixture","GET",null)).thenReturn(MobileUpstream.JSON.readTree("{\"codgUsuario\":42,\"status\":1,\"codigoWooba\":420,\"idToken\":\"own-key\"}"));
    }
    Map<String,Object> start(boolean password){return sessions.start("fixture",password?"user-password":null,device,"127.0.0.1");}
    MobileSessions.Session session(Map<String,Object> issued){return sessions.require("Bearer "+issued.get("accessToken"));}
    String send(MobileSessions.Session session){sessions.sendCode(session,"sms");var code=ArgumentCaptor.forClass(String.class);verify(upstream).deliver(any(),eq("sms"),code.capture());return code.getValue();}
    @Test void loginReturnsOnlyIndividualOpaqueTokens() {
        var result=start(true);assertTrue(result.get("accessToken").toString().matches("[A-Za-z0-9_-]{43}"));
        assertNotEquals(result.get("accessToken"),result.get("refreshToken"));
        assertFalse(result.toString().contains("private-"));assertTrue(session(result).primary);
    }
    @Test void identifierSessionDoesNotExposeProfileOrAuthorizeBusiness() {
        var result=start(false);var s=session(result);JsonNode publicProfile=(JsonNode)result.get("profile");
        assertEquals("0000",publicProfile.path("celular").asText());assertFalse(publicProfile.has("id"));assertFalse(publicProfile.has("chaveToken"));
        assertThrows(MobileFailure.class,()->sessions.requireActive(s));assertThrows(MobileFailure.class,()->sessions.register(s,profile));
    }
    @Test void invalidPasswordNeverCreatesSession(){when(upstream.call(eq("manager"),eq("usuario/auth"),eq("POST"),any())).thenReturn(MobileUpstream.JSON.createObjectNode());assertThrows(MobileFailure.class,()->start(true));}
    @Test void technicalJwtIsNotAMobileSession(){assertThrows(MobileFailure.class,()->sessions.require("Bearer header.payload.signature"));}
    @Test void refreshRotatesAndReuseRevokesFamily(){var original=start(true);var s=session(original);var renewed=sessions.renew(original.get("refreshToken").toString());assertThrows(MobileFailure.class,()->session(original));assertSame(s,session(renewed));assertThrows(MobileFailure.class,()->sessions.renew(original.get("refreshToken").toString()));assertThrows(MobileFailure.class,()->session(renewed));}
    @Test void logoutRevokesAccessAndRefresh(){var result=start(true);sessions.close(session(result));assertThrows(MobileFailure.class,()->session(result));assertThrows(MobileFailure.class,()->sessions.renew(result.get("refreshToken").toString()));}
    @Test void expiredAccessIsRejected(){var result=start(true);session(result).accessUntil=Instant.EPOCH;assertThrows(MobileFailure.class,()->session(result));}
    @Test void correctSmsAuthenticatesOnce(){var s=session(start(false));String code=send(s);assertTrue(code.matches("[0-9]{6}"));var p=sessions.validateCode(s,code);assertTrue(s.primary&&s.contact);assertEquals(42,p.path("id").asInt());assertEquals("own-key",p.path("tokenAcesso").asText());assertThrows(MobileFailure.class,()->sessions.validateCode(s,code));}
    @Test void wrongExpiredOrExhaustedSmsDoesNotAuthenticate(){var s=session(start(false));String code=send(s);String wrong=code.equals("000000")?"000001":"000000";for(int i=0;i<5;i++)assertThrows(MobileFailure.class,()->sessions.validateCode(s,wrong));assertThrows(MobileFailure.class,()->sessions.validateCode(s,code));assertFalse(s.primary);s.attempts=0;s.otpUntil=Instant.EPOCH;assertThrows(MobileFailure.class,()->sessions.validateCode(s,code));}
    @Test void failingAccountCheckDoesNotUpgradeIdentifierSession(){var s=session(start(false));String code=send(s);when(upstream.call("manager","usuario/findByLogin/fixture","GET",null)).thenThrow(new MobileFailure(503,"UNAVAILABLE"));assertThrows(MobileFailure.class,()->sessions.validateCode(s,code));assertFalse(s.primary||s.contact);}
    @Test void resendIsThrottled(){var s=session(start(false));send(s);assertThrows(MobileFailure.class,()->sessions.sendCode(s,"sms"));verify(upstream,times(1)).deliver(any(),anyString(),anyString());}
    @Test void pendingDeviceBlocksBusinessEvenAfterPassword(){var s=session(start(true));when(upstream.call("manager","dispositivo/login/fixture?codigoDispositivo="+device,"GET",null)).thenReturn(MobileUpstream.JSON.valueToTree(Map.of("autorizado",false,"situacao","EM_AUTORIZACAO")));assertThrows(MobileFailure.class,()->sessions.requireActive(s));assertThrows(MobileFailure.class,()->sessions.register(s,profile));}
    @Test void onlyApprovedMatchingDeviceCanAccess(){var s=session(start(true));when(upstream.call("manager","dispositivo/login/fixture?codigoDispositivo="+device,"GET",null)).thenReturn(MobileUpstream.JSON.valueToTree(Map.of("autorizado",true,"codigoDispositivo",device)));assertDoesNotThrow(()->sessions.requireActive(s));when(upstream.call("manager","dispositivo/login/fixture?codigoDispositivo="+device,"GET",null)).thenReturn(MobileUpstream.JSON.valueToTree(Map.of("autorizado",true,"codigoDispositivo","another")));assertThrows(MobileFailure.class,()->sessions.requireActive(s));}
    @Test void registrationBindsIdentityAndDeviceOnServer() {
        var s=session(start(false));sessions.validateCode(s,send(s));
        sessions.register(s,MobileUpstream.JSON.valueToTree(Map.of("login","someone-else","codigoDispositivo","forged","metadados",Map.of("osName","iOS"))));
        var body=ArgumentCaptor.forClass(JsonNode.class);verify(upstream).call(eq("manager"),eq("dispositivo/novo"),eq("POST"),body.capture());
        assertEquals("fixture",body.getValue().path("login").asText());assertEquals(device,body.getValue().path("codigoDispositivo").asText());assertEquals("iOS",body.getValue().path("metadados").path("osName").asText());
    }
    @Test void sessionKeepsExistingTwentyDayLoginWithShortAccessTokens(){
        var s=session(start(true));var now=Instant.now();
        assertTrue(s.until.isAfter(now.plus(java.time.Duration.ofDays(19))));
        assertTrue(s.until.isBefore(now.plus(java.time.Duration.ofDays(20)).plusSeconds(1)));
        assertTrue(s.accessUntil.isBefore(now.plusSeconds(901)));
    }
    @Test void emailCopyUsesTheSameServerGeneratedValidationCode(){
        var s=session(start(false));sessions.sendCode(s,"sms",true);
        var code=ArgumentCaptor.forClass(String.class);verify(upstream).deliverLoginCode(eq(s.profile),code.capture());
        sessions.validateCode(s,code.getValue());assertTrue(s.contact);
    }
    @Test void identifierKeepsEmailPreferenceWithoutExposingOrActivatingDevice(){
        var s=session(start(false));
        when(upstream.call("manager","dispositivo/login/fixture","GET",null)).thenReturn(MobileUpstream.JSON.valueToTree(Map.of("enviarEmail",1,"codigoDispositivo","private-device","tokenExpo","private-push")));
        var result=sessions.device(s);
        assertEquals(1,result.path("enviarEmail").asInt());assertFalse(result.path("autorizado").asBoolean());
        assertFalse(result.toString().contains("private-"));
        verify(upstream,never()).call(eq("manager"),contains("?codigoDispositivo="),anyString(),any());
    }
}
