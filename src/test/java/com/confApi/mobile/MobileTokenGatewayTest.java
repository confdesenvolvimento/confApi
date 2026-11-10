package com.confApi.mobile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MobileTokenGatewayTest {
    MobileUpstream upstream; MockMvc mvc; String access;
    String device="00000000-0000-4000-8000-000000000001";
    @BeforeEach void setup() {
        var settings=new MobileSettings(MobileUpstream.JSON.valueToTree(Map.of(
            "cserviceLogin","integration","cservicePassword","private-password",
            "cserviceTokenResponseKey","000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f")));
        upstream=spy(new MobileUpstream(settings));
        doReturn(MobileUpstream.JSON.valueToTree(Map.of("login","fixture","id",42,"tokenAcesso","0004TESTE")))
            .when(upstream).call(eq("manager"),eq("usuario/auth"),eq("POST"),any());
        doReturn(MobileUpstream.JSON.valueToTree(Map.of("autorizado",true,"codigoDispositivo",device)))
            .when(upstream).call("manager","dispositivo/login/fixture?codigoDispositivo="+device,"GET",null);
        var sessions=new MobileSessions(upstream);
        access="Bearer "+sessions.start("fixture","password",device,"127.0.0.1").get("accessToken");
        mvc=MockMvcBuilders.standaloneSetup(new MobileApi(sessions,upstream,new MobileChatDelegations())).build();
    }
    @ParameterizedTest @ValueSource(strings={"api/token/gerartoken/0004TESTE/1","api/token/mobile/gerartoken/0004TESTE/1"})
    void authorizedGenerationReturnsCodeWithoutSharingKey(String path) throws Exception {
        String body="{\"valor\":\"EBESExQVFhcYGRobTc6sJL/zQ6VQQSQPzDgIzeqzK7w=\",\"trackingToken\":\"tracking\",\"status\":\"ATIVO\"}";
        doReturn(new MobileUpstream.Response(200,"application/json",new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))))
            .when(upstream).forward(eq("cservice"),eq(path),isNull(),eq("POST"),any(),any());
        mvc.perform(post("/api/mobile/services/cservice/"+path).header("Authorization",access)
            .contentType(MediaType.APPLICATION_JSON).content("{\"usuarioLoginAcesso\":\"fixture\"}"))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.valor").value("0042")).andExpect(jsonPath("$.trackingToken").value("tracking"))
            .andExpect(jsonPath("$.cserviceTokenResponseKey").doesNotExist()).andExpect(jsonPath("$.usuarioSenha").doesNotExist());
        verify(upstream).forward(eq("cservice"),eq(path),isNull(),eq("POST"),argThat(bytes->{
            String input=new String(bytes,StandardCharsets.UTF_8);
            return input.contains("private-password")&&input.contains("fixture");
        }),any());
    }
    @Test void anotherUsersTokenCannotReachTheService() throws Exception {
        mvc.perform(post("/api/mobile/services/cservice/api/token/mobile/gerartoken/0004OTHER/1")
            .header("Authorization",access).contentType(MediaType.APPLICATION_JSON).content("{\"usuarioLoginAcesso\":\"fixture\"}"))
            .andExpect(status().isForbidden());
        verify(upstream,never()).forward(any(),any(),any(),any(),any(),any());
    }
    @Test void responseKeyIsRemovedFromErrorMessages() {
        var body=upstream.publicError(400,"application/json",("{\"message\":\"000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f\"}").getBytes(StandardCharsets.UTF_8));
        org.junit.jupiter.api.Assertions.assertFalse(body.has("message"));
    }
}
