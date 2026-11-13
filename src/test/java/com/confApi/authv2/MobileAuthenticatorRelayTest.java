package com.confApi.authv2;

import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.ResponseEntity;
import javax.servlet.Filter;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MobileAuthenticatorRelayTest {
    static class Relay extends MobileAuthenticatorRelay {
        String path,method; byte[] body; Map<String,String> headers;int calls;
        @Override protected String gatewayToken(){return "Bearer synthetic-confapi-service-identity";}
        @Override protected ResponseEntity<?> forward(String path,String method,byte[] body,Map<String,String> headers){this.path=path;this.method=method;this.body=body;this.headers=headers;calls++;return ResponseEntity.accepted().body(Map.of("state","PROVISORIA"));}
    }
    @Configuration @EnableWebMvc @EnableWebSecurity @Import({MobileAuthenticatorSecurity.class,V2Security.class,V2Errors.class})
    static class Config { @Bean Relay relay(){return new Relay();} }
    AnnotationConfigWebApplicationContext context;MockMvc mvc;Relay relay;
    @BeforeEach void setup(){System.setProperty("AUTH_V2_ENABLED","true");context=new AnnotationConfigWebApplicationContext();context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();relay=context.getBean(Relay.class);mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean("springSecurityFilterChain",Filter.class)).build();}
    @AfterEach void close(){context.close();System.clearProperty("AUTH_V2_ENABLED");}
    @Test void publicLoginWithWarContextUsesOnlyTheConfiguredGatewayIdentity()throws Exception {
        mvc.perform(post("/ConfApi"+MobileAuthenticatorRelay.ROOT+"/sessoes").contextPath("/ConfApi").secure(true).header("X-Auth-Gateway","Bearer attacker").header("Host","attacker.example").header("Authorization","Bearer technical-legacy").contentType("application/json").content("{\"login\":\"synthetic\",\"password\":\"synthetic\",\"publicJwk\":\"synthetic\"}"))
                .andExpect(status().isAccepted()).andExpect(header().string("Cache-Control","no-store"));
        assertEquals(MobileAuthenticatorRelay.ROOT+"/sessoes",relay.path);
        assertEquals(Map.of("X-Auth-Gateway","Bearer synthetic-confapi-service-identity","X-Auth-Client-Key",V2.hash("127.0.0.1"),"X-Auth-Client-IP","127.0.0.1"),relay.headers);
    }
    @Test void enrollmentPreservesProofAndIdempotencyWithoutChangingMobileIdentity()throws Exception {
        String token="x".repeat(43);
        mvc.perform(post(MobileAuthenticatorRelay.ROOT+"/cadastros").secure(true).header("Authorization","DPoP "+token).header("DPoP","synthetic-proof").header("Idempotency-Key","synthetic-idempotency").contentType("application/json"))
                .andExpect(status().isAccepted());
        assertEquals("DPoP "+token,relay.headers.get("Authorization"));assertEquals("synthetic-proof",relay.headers.get("DPoP"));assertEquals("synthetic-idempotency",relay.headers.get("Idempotency-Key"));
    }
    @Test void technicalTokenCannotBeUsedAsMobileSession()throws Exception {
        var response=mvc.perform(get(MobileAuthenticatorRelay.ROOT+"/perfil").secure(true).header("Authorization","Bearer old-token")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.correlationId").isNotEmpty()).andReturn().getResponse();
        assertEquals(response.getHeader("X-Correlation-ID"),V2.JSON.readTree(response.getContentAsString()).path("correlationId").asText());assertEquals(0,relay.calls);
    }
    @Test void noGenericProxyNoQueryAndNoCleartextFallback()throws Exception {
        mvc.perform(get(MobileAuthenticatorRelay.ROOT+"/arbitrary").secure(true)).andExpect(status().isNotFound());
        mvc.perform(post(MobileAuthenticatorRelay.ROOT+"/sessoes").secure(false)).andExpect(status().isBadRequest());
        mvc.perform(post(MobileAuthenticatorRelay.ROOT+"/sessoes?password=forbidden").secure(true)).andExpect(status().isBadRequest());
        mvc.perform(post("/internal/v2/autenticacao/primaria").secure(true)).andExpect(status().isUnauthorized());assertEquals(0,relay.calls);
    }
    @Test void disabledPilotDoesNotForwardRequests()throws Exception {
        System.setProperty("AUTH_V2_ENABLED","false");mvc.perform(post(MobileAuthenticatorRelay.ROOT+"/sessoes").secure(true)).andExpect(status().isServiceUnavailable());assertEquals(0,relay.calls);
    }
    @Test void oversizedBodyAndInvalidIdempotencyAreRejected()throws Exception {
        mvc.perform(post(MobileAuthenticatorRelay.ROOT+"/sessoes").secure(true).contentType("application/json").content("x".repeat(16385))).andExpect(status().isPayloadTooLarge());
        mvc.perform(post(MobileAuthenticatorRelay.ROOT+"/cadastros").secure(true).header("Authorization","DPoP "+"x".repeat(43)).header("DPoP","proof")).andExpect(status().isBadRequest());assertEquals(0,relay.calls);
    }

    @Test void claimedClientIpAndForwardedHeadersCannotReplaceObservedPeer()throws Exception {
        mvc.perform(post(MobileAuthenticatorRelay.ROOT+"/cadastros").secure(true).with(request->{request.setRemoteAddr("2001:db8::12");return request;})
          .header("X-Auth-Client-IP","198.51.100.99").header("X-Forwarded-For","198.51.100.98").header("Forwarded","for=198.51.100.97")
          .header("Authorization","DPoP "+"x".repeat(43)).header("DPoP","proof").header("Idempotency-Key","synthetic-idempotency").contentType("application/json").content("{\"ipCadastro\":\"198.51.100.96\"}"))
          .andExpect(status().isAccepted());
        assertEquals("2001:db8::12",relay.headers.get("X-Auth-Client-IP"));assertEquals(V2.hash("2001:db8::12"),relay.headers.get("X-Auth-Client-Key"));assertFalse(relay.headers.containsKey("X-Forwarded-For"));assertFalse(relay.headers.containsKey("Forwarded"));
    }

}
