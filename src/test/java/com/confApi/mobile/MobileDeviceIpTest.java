package com.confApi.mobile;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.ForwardedHeaderFilter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MobileDeviceIpTest {
    @ParameterizedTest @ValueSource(strings={"198.51.100.42","2001:db8::42"})
    void registrationForwardsObservedOriginInsteadOfAppClaims(String origin) throws Exception {
        var upstream=mock(MobileUpstream.class);
        var sessions=new MobileSessions(upstream);
        String device="00000000-0000-4000-8000-000000000002";
        when(upstream.call(eq("manager"),eq("usuario/auth"),eq("POST"),any()))
                .thenReturn(MobileUpstream.JSON.valueToTree(Map.of("id",42,"login","fixture")));
        when(upstream.call("manager","dispositivo/login/fixture?codigoDispositivo="+device,"GET",null))
                .thenReturn(MobileUpstream.JSON.valueToTree(Map.of("autorizado",true)));
        when(upstream.registerDevice(any(),eq(origin))).thenReturn(MobileUpstream.JSON.valueToTree(Map.of("autorizado",false)));
        String access="Bearer "+sessions.start("fixture","password",device,"192.0.2.9").get("accessToken");
        var api=new MobileApi(sessions,upstream,new MobileChatDelegations());
        var mvc=MockMvcBuilders.standaloneSetup(api).build();
        mvc.perform(post("/api/mobile/services/manager/dispositivo/novo")
                .header("Authorization",access).header("X-Auth-Client-IP","203.0.113.99")
                .header("X-Forwarded-For","203.0.113.98").header("Forwarded","for=203.0.113.97")
                .with(r->{r.setRemoteAddr(origin);return r;})
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"forged\",\"ipCadastro\":\"203.0.113.96\",\"metadados\":{\"ipCadastro\":\"203.0.113.95\"}}"))
                .andExpect(status().isAccepted());
        ReflectionTestUtils.setField(api,"clientIp",new MobileClientIp("10.0.0.5/32"));
        var proxied=MockMvcBuilders.standaloneSetup(api).addFilters(new ForwardedHeaderFilter()).build();
        proxied.perform(post("/api/mobile/services/manager/dispositivo/novo")
                .header("Authorization",access).header("X-Auth-Client-IP","203.0.113.99")
                .header("X-Forwarded-For","203.0.113.98, "+origin)
                .with(r->{r.setRemoteAddr("10.0.0.5");return r;})
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isAccepted());
        var body=org.mockito.ArgumentCaptor.forClass(JsonNode.class);
        verify(upstream,times(2)).registerDevice(body.capture(),eq(origin));
        assertEquals("fixture",body.getValue().path("login").asText());
        assertEquals(device,body.getValue().path("codigoDispositivo").asText());
        assertFalse(body.getValue().has("ipCadastro"));
    }

    @Test void forwardedFilterCannotMakeAnUntrustedHeaderAuthoritative() throws Exception {
        var request=new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.42");
        request.addHeader("X-Forwarded-For","203.0.113.99");
        new ForwardedHeaderFilter().doFilter(request,new MockHttpServletResponse(),(req,res)->{
            assertEquals("203.0.113.99",req.getRemoteAddr());
            assertEquals("198.51.100.42",new MobileClientIp("10.0.0.5/32").resolve((javax.servlet.http.HttpServletRequest)req));
        });
    }

    @Test void trustedProxyChainStopsAtNearestUntrustedPeerEvenAfterSpringWrapping() throws Exception {
        var request=new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For","203.0.113.99, 198.51.100.42, 10.0.0.6");
        new ForwardedHeaderFilter().doFilter(request,new MockHttpServletResponse(),(req,res)->
                assertEquals("198.51.100.42",new MobileClientIp("10.0.0.5/32,10.0.0.6/32").resolve((javax.servlet.http.HttpServletRequest)req)));
    }

    @Test void ipv6ProxyAndIpv6ClientArePreserved() {
        var request=new MockHttpServletRequest();
        request.setRemoteAddr("2001:db8:1::5");
        request.addHeader("X-Forwarded-For","2001:db8:2::42");
        assertEquals("2001:db8:2::42",new MobileClientIp("2001:db8:1::/64").resolve(request));
    }

    @Test void trustedProxyWithoutOriginOrWithInvalidOriginFailsInsteadOfSavingProxy() {
        for(String value:List.of("","hostname.example","999.1.1.1","198.51.100.42:123","[2001:db8::42]","198.51.100.42,","10.0.0.5")) {
            var request=new MockHttpServletRequest();request.setRemoteAddr("10.0.0.5");
            if(!value.isEmpty())request.addHeader("X-Forwarded-For",value);
            assertEquals(400,assertThrows(MobileFailure.class,()->new MobileClientIp("10.0.0.5/32").resolve(request)).status);
        }
    }

    @Test void realHttpRequestIncludesOriginAndServiceCredentialWithoutForwardingCallerHeaders() throws Exception {
        var received=new AtomicReference<Map<String,String>>();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/dispositivo/novo",exchange->{
            received.set(Map.of("ip",exchange.getRequestHeaders().getFirst("X-Auth-Client-IP"),
                    "auth",exchange.getRequestHeaders().getFirst("Authorization"),
                    "forwarded",Objects.toString(exchange.getRequestHeaders().getFirst("X-Forwarded-For"),""),
                    "body",new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8)));
            byte[] result="{\"autorizado\":false}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type","application/json");
            exchange.sendResponseHeaders(202,result.length);
            try(var output=exchange.getResponseBody()){output.write(result);}
        });
        server.start();
        try {
            var upstream=new MobileUpstream(new MobileSettings(MobileUpstream.JSON.createObjectNode())) {
                @Override protected String technicalToken(){return "synthetic-service-token";}
                @Override synchronized String destination(String service){return "http://127.0.0.1:"+server.getAddress().getPort()+"/";}
            };
            var body=MobileUpstream.JSON.valueToTree(Map.of("login","fixture","codigoDispositivo","test-device"));
            assertFalse(upstream.registerDevice(body,"2001:db8::42").path("autorizado").asBoolean());
            assertEquals("2001:db8::42",received.get().get("ip"));
            assertEquals("Bearer synthetic-service-token",received.get().get("auth"));
            assertEquals("",received.get().get("forwarded"));
            assertEquals(body,MobileUpstream.JSON.readTree(received.get().get("body")));
        } finally { server.stop(0); }
    }
}
