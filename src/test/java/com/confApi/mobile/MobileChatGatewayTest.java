package com.confApi.mobile;

import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.io.ByteArrayInputStream;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MobileChatGatewayTest {
    MobileUpstream upstream; MobileSessions sessions; MobileChatDelegations grants;
    MockMvc mvc; String access;
    String device="00000000-0000-4000-8000-000000000001";
    @BeforeEach void setup() {
        upstream=mock(MobileUpstream.class); sessions=new MobileSessions(upstream); grants=new MobileChatDelegations();
        when(upstream.call(eq("manager"),eq("usuario/auth"),eq("POST"),any()))
                .thenReturn(MobileUpstream.JSON.valueToTree(Map.of("id",42)));
        when(upstream.call("manager","dispositivo/login/fixture?codigoDispositivo="+device,"GET",null))
                .thenReturn(MobileUpstream.JSON.valueToTree(Map.of("autorizado",true,"codigoDispositivo",device)));
        access="Bearer "+sessions.start("fixture","password",device,"127.0.0.1").get("accessToken");
        mvc=MockMvcBuilders.standaloneSetup(new MobileApi(sessions,upstream,grants)).build();
    }
    @Test void gatewayCreatesItsOwnGrantFromVerifiedSessionAndReturnsResponse() throws Exception {
        when(upstream.forwardWithIdentity(eq("v1/chat-confianca/sessao/42"),isNull(),eq("GET"),any(),isNull(),anyString()))
                .thenAnswer(call -> {
                    String grant=call.getArgument(5); assertNotEquals("client-forged-grant",grant);
                    var request=new MockHttpServletRequest("GET","/v1/chat-confianca/sessao/42");
                    request.addHeader(MobileChatDelegations.HEADER,grant);
                    new MobileChatIdentityFilter(grants).doFilter(request,new MockHttpServletResponse(),(req,res)->
                            assertEquals("fixture",SecurityContextHolder.getContext().getAuthentication().getName()));
                    return new MobileUpstream.Response(200,"application/json",new ByteArrayInputStream("{}".getBytes()));
                });
        mvc.perform(get("/api/mobile/services/confapi/v1/chat-confianca/sessao/42")
                        .header("Authorization",access).header(MobileChatDelegations.HEADER,"client-forged-grant"))
                .andExpect(status().isOk()).andExpect(content().json("{}"));
        verify(upstream).forwardWithIdentity(anyString(),isNull(),eq("GET"),any(),isNull(),anyString());
    }
    @Test void otherUserInPathQueryOrBodyNeverReceivesADelegation() throws Exception {
        mvc.perform(get("/api/mobile/services/confapi/v1/chat-confianca/sessao/99").header("Authorization",access)).andExpect(status().isForbidden());
        mvc.perform(get("/api/mobile/services/confapi/v1/chat-confianca/conversas/abc?codgUsuario=99").header("Authorization",access)).andExpect(status().isForbidden());
        mvc.perform(post("/api/mobile/services/confapi/v1/chat-confianca/mensagens").header("Authorization",access)
                .contentType(MediaType.APPLICATION_JSON).content("{\"codgUsuario\":99}" )).andExpect(status().isForbidden());
        verify(upstream,never()).forwardWithIdentity(any(),any(),any(),any(),any(),any());
    }
    @Test void pendingDeviceCannotDelegateChatIdentity() throws Exception {
        when(upstream.call("manager","dispositivo/login/fixture?codigoDispositivo="+device,"GET",null))
                .thenReturn(MobileUpstream.JSON.valueToTree(Map.of("autorizado",false)));
        mvc.perform(get("/api/mobile/services/confapi/v1/chat-confianca/sessao/42").header("Authorization",access)).andExpect(status().isForbidden());
        verify(upstream,never()).forwardWithIdentity(any(),any(),any(),any(),any(),any());
    }
}
