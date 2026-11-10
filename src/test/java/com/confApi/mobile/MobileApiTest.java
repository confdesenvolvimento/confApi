package com.confApi.mobile;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import java.util.Map;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class MobileApiTest {
    @Test void emptyManagerResponseDoesNotBecome500() throws Exception {
        active();
        when(upstream.forward(eq("manager"),eq("reservaAereo/localizador/ABC123"),isNull(),eq("GET"),any(),isNull()))
            .thenReturn(new MobileUpstream.Response(200,"application/octet-stream",new ByteArrayInputStream(new byte[0])));
        mvc.perform(get("/api/mobile/services/manager/reservaAereo/localizador/ABC123").header("Authorization",access))
            .andExpect(status().isOk()).andExpect(content().bytes(new byte[0]));
    }
    @Test void binaryDownloadKeepsBytesAndClosesInput() throws Exception {
        active();byte[] bytes=new byte[]{0,1,2,(byte)255};
        var closed=new java.util.concurrent.atomic.AtomicBoolean();
        var input=new ByteArrayInputStream(bytes){@Override public void close(){closed.set(true);}};
        when(upstream.forwardWithIdentity(eq("v1/chat-confianca/anexos/abc/download"),isNull(),eq("GET"),any(),isNull(),anyString()))
            .thenReturn(new MobileUpstream.Response(200,"application/pdf",input,Map.of("Content-Length","4")));
        mvc.perform(get("/api/mobile/services/confapi/v1/chat-confianca/anexos/abc/download").header("Authorization",access))
            .andExpect(status().isOk()).andExpect(content().contentType("application/pdf")).andExpect(content().bytes(bytes));
        assertTrue(closed.get());
    }
    @Test void successfulEmailWithoutBodyIsForwarded() throws Exception {
        active();
        when(upstream.forward(eq("confapi"),eq("api/geradorPDF/reservaAereoApp"),isNull(),eq("POST"),any(),any()))
            .thenReturn(new MobileUpstream.Response(200,"application/octet-stream",new ByteArrayInputStream(new byte[0])));
        mvc.perform(post("/api/mobile/services/confapi/api/geradorPDF/reservaAereoApp").header("Authorization",access)
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk()).andExpect(content().string(""));
    }
    private void active() {
        when(upstream.call("manager","dispositivo/login/fixture?codigoDispositivo="+device,"GET",null))
            .thenReturn(MobileUpstream.JSON.valueToTree(Map.of("autorizado",true,"codigoDispositivo",device)));
    }
    MobileSessions sessions;MobileUpstream upstream;MockMvc mvc;String access;
    String device="00000000-0000-4000-8000-000000000001";
    @BeforeEach void setup() {
        upstream=mock(MobileUpstream.class);sessions=new MobileSessions(upstream);mvc=MockMvcBuilders.standaloneSetup(new MobileApi(sessions,upstream,new MobileChatDelegations())).build();
        when(upstream.publicError(anyInt(),anyString(),any())).thenCallRealMethod();
        when(upstream.call(eq("manager"),eq("usuario/auth"),eq("POST"),any())).thenReturn(MobileUpstream.JSON.valueToTree(Map.of("login","fixture","id",42,"senha","private-password","chaveToken","private-token")));
        access="Bearer "+sessions.start("fixture","password",device,"127.0.0.1").get("accessToken");
    }
    @Test void unauthenticatedUserCannotCallBusiness() throws Exception {mvc.perform(get("/api/mobile/services/manager/unidade")).andExpect(status().isUnauthorized());verify(upstream,never()).forward(any(),any(),any(),any(),any(),any());}
    @Test void profileNeverReturnsUpstreamPasswordOrJwt() throws Exception {String body=mvc.perform(get("/api/mobile/perfil").header("Authorization",access)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertFalse(body.contains("private-"));assertFalse(body.contains("chaveToken"));}
    @Test void pendingDeviceCannotUseGateway() throws Exception {
        when(upstream.call("manager","dispositivo/login/fixture?codigoDispositivo="+device,"GET",null)).thenReturn(MobileUpstream.JSON.valueToTree(Map.of("autorizado",false)));
        mvc.perform(get("/api/mobile/services/manager/unidade").header("Authorization",access)).andExpect(status().isForbidden());verify(upstream,never()).forward(any(),any(),any(),any(),any(),any());
    }
    @Test void forgedDeviceQueryCannotSwitchBoundDevice() throws Exception {
        when(upstream.call("manager","dispositivo/login/fixture?codigoDispositivo="+device,"GET",null)).thenReturn(MobileUpstream.JSON.valueToTree(Map.of("autorizado",false)));
        mvc.perform(get("/api/mobile/services/manager/dispositivo/login/fixture?codigoDispositivo=forged").header("Authorization",access)).andExpect(status().isOk());
        verify(upstream).call("manager","dispositivo/login/fixture?codigoDispositivo="+device,"GET",null);
    }
    @Test void arbitraryDestinationsAndAuthenticationEndpointsAreNotForwarded() throws Exception {
        mvc.perform(post("/api/mobile/services/hub/user/auth").header("Authorization",access).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isNotFound());
        mvc.perform(get("/api/mobile/services/unknown/user").header("Authorization",access)).andExpect(status().isNotFound());verify(upstream,never()).forward(any(),any(),any(),any(),any(),any());
    }
    @Test void upstreamErrorsCannotEchoTechnicalSecrets() throws Exception {
        when(upstream.call("manager","dispositivo/login/fixture?codigoDispositivo="+device,"GET",null)).thenReturn(MobileUpstream.JSON.valueToTree(Map.of("autorizado",true,"codigoDispositivo",device)));
        when(upstream.forward(eq("manager"),eq("unidade"),isNull(),eq("GET"),any(),isNull())).thenReturn(new MobileUpstream.Response(500,"application/json",new ByteArrayInputStream("private-password private-token".getBytes(StandardCharsets.UTF_8))));
        String body=mvc.perform(get("/api/mobile/services/manager/unidade").header("Authorization",access)).andExpect(status().isBadGateway()).andReturn().getResponse().getContentAsString();assertFalse(body.contains("private-"));
    }
    @Test void notificationCountReachesTheAppWithTheJsonBody() throws Exception {
        when(upstream.call("manager","dispositivo/login/fixture?codigoDispositivo="+device,"GET",null)).thenReturn(MobileUpstream.JSON.valueToTree(Map.of("autorizado",true,"codigoDispositivo",device)));
        when(upstream.forward(eq("manager"),eq("notificacao/login/fixture"),isNull(),eq("GET"),any(),isNull())).thenReturn(new MobileUpstream.Response(200,"application/json",new ByteArrayInputStream("[]".getBytes(StandardCharsets.UTF_8)),Map.of("Contagem","12")));
        mvc.perform(get("/api/mobile/services/manager/notificacao/login/fixture").header("Authorization",access))
            .andExpect(status().isOk()).andExpect(header().string("Contagem","12")).andExpect(content().json("[]"));
    }
    @Test void userLookupKeepsTheOriginalManagerEntityContract() throws Exception {
        when(upstream.call("manager","dispositivo/login/fixture?codigoDispositivo="+device,"GET",null)).thenReturn(MobileUpstream.JSON.valueToTree(Map.of("autorizado",true,"codigoDispositivo",device)));
        when(upstream.call("manager","usuario/findByLogin/fixture","GET",null)).thenReturn(MobileUpstream.JSON.valueToTree(Map.of("codgUsuario",42,"loginUsuario","fixture","nomeCompleto","Fixture","senha","private-password")));
        mvc.perform(get("/api/mobile/services/manager/usuario/findByLogin/fixture").header("Authorization",access)).andExpect(status().isOk())
            .andExpect(jsonPath("$.codgUsuario").value(42)).andExpect(jsonPath("$.loginUsuario").value("fixture")).andExpect(jsonPath("$.senha").doesNotExist());
        mvc.perform(get("/api/mobile/services/manager/usuario/findByLogin/other").header("Authorization",access)).andExpect(status().isNotFound());
    }
    @Test void gatewayKeepsBusinessFailureAndCompatibilityStatus() throws Exception {
        when(upstream.call("manager","dispositivo/login/fixture?codigoDispositivo="+device,"GET",null)).thenReturn(MobileUpstream.JSON.valueToTree(Map.of("autorizado",true,"codigoDispositivo",device)));
        when(upstream.forward(eq("confapi"),eq("v1/hoteis/pesquisas"),isNull(),eq("POST"),any(),any())).thenReturn(
            new MobileUpstream.Response(405,"application/json",new ByteArrayInputStream("{\"message\":\"Use a consulta tradicional\"}".getBytes(StandardCharsets.UTF_8))));
        mvc.perform(post("/api/mobile/services/confapi/v1/hoteis/pesquisas").header("Authorization",access).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.message").value("Use a consulta tradicional"));
    }
}
