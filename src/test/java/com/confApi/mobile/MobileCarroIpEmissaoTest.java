package com.confApi.mobile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MobileCarroIpEmissaoTest {
    private MobileUpstream upstream;
    private MockMvc mvc;
    private String access;

    @BeforeEach void setup() {
        upstream = mock(MobileUpstream.class);
        var sessions = new MobileSessions(upstream);
        mvc = MockMvcBuilders.standaloneSetup(new MobileApi(sessions, upstream, new MobileChatDelegations())).build();
        when(upstream.call(eq("manager"), eq("usuario/auth"), eq("POST"), any()))
                .thenReturn(MobileUpstream.JSON.valueToTree(Map.of("login", "fixture", "id", 42)));
        String device = "00000000-0000-4000-8000-000000000001";
        access = "Bearer " + sessions.start("fixture", "password", device, "127.0.0.1").get("accessToken");
        when(upstream.call("manager", "dispositivo/login/fixture?codigoDispositivo=" + device, "GET", null))
                .thenReturn(MobileUpstream.JSON.valueToTree(Map.of("autorizado", true, "codigoDispositivo", device)));
    }

    @ParameterizedTest @ValueSource(strings = {"192.0.2.10", "2001:db8::10"})
    void enviaIpDaConexaoERepassaRecusaDaLoja(String remote) throws Exception {
        String recusa = "{\"success\":false,\"mensagem\":\"A loja não está aberta no horário selecionado.\"}";
        when(upstream.forward(eq("confapi"), eq("v1/carro/reservar"), isNull(), eq("POST"), any(), any()))
                .thenReturn(new MobileUpstream.Response(200, "application/json", new ByteArrayInputStream(recusa.getBytes(StandardCharsets.UTF_8))));
        String body = "{\"reservaCarro\":{\"token\":\"selecao\",\"idLocalRetirada\":1,\"idLocalEntrega\":2},\"usuario\":{\"codgUsuario\":42}}";
        mvc.perform(post("/api/mobile/services/confapi/v1/carro/reservar").header("Authorization", access)
                .contentType(MediaType.APPLICATION_JSON).content(body)
                .with(req -> { req.setRemoteAddr(remote); return req; }))
                .andExpect(status().isOk()).andExpect(content().json(recusa));
        var capture = ArgumentCaptor.forClass(byte[].class);
        verify(upstream).forward(eq("confapi"), eq("v1/carro/reservar"), isNull(), eq("POST"), capture.capture(), any());
        var sent = MobileUpstream.JSON.readTree(capture.getValue());
        assertEquals(remote, sent.path("reservaCarro").path("ipEmissao").asText());
        assertEquals("selecao", sent.path("reservaCarro").path("token").asText());
        assertEquals(2, sent.path("reservaCarro").path("idLocalEntrega").asInt());
        assertEquals(42, sent.path("usuario").path("codgUsuario").asInt());
    }

    @Test void ipDoAppECabecalhosNaoSubstituemOrigemDaConexao() throws Exception {
        when(upstream.forward(eq("confapi"), eq("v1/carro/reservar"), isNull(), eq("POST"), any(), any()))
                .thenReturn(new MobileUpstream.Response(200, "application/json", new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8))));
        mvc.perform(post("/api/mobile/services/confapi/v1/carro/reservar").header("Authorization", access)
                .header("X-Forwarded-For", "198.51.100.2").header("Forwarded", "for=198.51.100.3")
                .contentType(MediaType.APPLICATION_JSON).content("{\"reservaCarro\":{\"ipEmissao\":\"198.51.100.1\"}}")
                .with(req -> { req.setRemoteAddr("192.0.2.10"); return req; }))
                .andExpect(status().isOk());
        var capture = ArgumentCaptor.forClass(byte[].class);
        verify(upstream).forward(eq("confapi"), eq("v1/carro/reservar"), isNull(), eq("POST"), capture.capture(), any());
        assertEquals("192.0.2.10", MobileUpstream.JSON.readTree(capture.getValue()).path("reservaCarro").path("ipEmissao").asText());
    }
}
