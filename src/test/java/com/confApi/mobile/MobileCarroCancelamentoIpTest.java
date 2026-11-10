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

class MobileCarroCancelamentoIpTest {
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
    void preencheIpDoCancelamentoSemAlterarReservaEMotivo(String remote) throws Exception {
        String response = "[{\"success\":false,\"message\":\"Cancelamento não permitido pela locadora.\"}]";
        when(upstream.forward(eq("confapi"), eq("v1/carro/cancelarReserva"), isNull(), eq("POST"), any(), any()))
                .thenReturn(new MobileUpstream.Response(200, "application/json", new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8))));
        String body = "{\"cancelarReservaCarroRequestDTO\":{\"localizador\":\"CAR123\",\"token\":\"selecao-teste\"},\"usuario\":{\"codgUsuario\":42},\"descricaoMotivoCancelamento\":\"Teste de cancelamento\"}";
        mvc.perform(post("/api/mobile/services/confapi/v1/carro/cancelarReserva").header("Authorization", access)
                .contentType(MediaType.APPLICATION_JSON).content(body)
                .with(req -> { req.setRemoteAddr(remote); return req; }))
                .andExpect(status().isOk()).andExpect(content().json(response));
        var capture = ArgumentCaptor.forClass(byte[].class);
        verify(upstream).forward(eq("confapi"), eq("v1/carro/cancelarReserva"), isNull(), eq("POST"), capture.capture(), any());
        var sent = MobileUpstream.JSON.readTree(capture.getValue());
        assertEquals(remote, sent.path("cancelarReservaCarroRequestDTO").path("ip").asText());
        assertEquals("CAR123", sent.path("cancelarReservaCarroRequestDTO").path("localizador").asText());
        assertEquals("selecao-teste", sent.path("cancelarReservaCarroRequestDTO").path("token").asText());
        assertEquals("Teste de cancelamento", sent.path("descricaoMotivoCancelamento").asText());
        assertEquals(42, sent.path("usuario").path("codgUsuario").asInt());
    }

    @Test void ipInformadoPeloAppOuCabecalhosNaoSubstituiOrigem() throws Exception {
        when(upstream.forward(eq("confapi"), eq("v1/carro/cancelarReserva"), isNull(), eq("POST"), any(), any()))
                .thenReturn(new MobileUpstream.Response(200, "application/json", new ByteArrayInputStream("[]".getBytes(StandardCharsets.UTF_8))));
        mvc.perform(post("/api/mobile/services/confapi/v1/carro/cancelarReserva").header("Authorization", access)
                .header("X-Forwarded-For", "198.51.100.2").header("Forwarded", "for=198.51.100.3")
                .contentType(MediaType.APPLICATION_JSON).content("{\"cancelarReservaCarroRequestDTO\":{\"ip\":\"198.51.100.1\"}}")
                .with(req -> { req.setRemoteAddr("192.0.2.10"); return req; }))
                .andExpect(status().isOk());
        var capture = ArgumentCaptor.forClass(byte[].class);
        verify(upstream).forward(eq("confapi"), eq("v1/carro/cancelarReserva"), isNull(), eq("POST"), capture.capture(), any());
        assertEquals("192.0.2.10", MobileUpstream.JSON.readTree(capture.getValue()).path("cancelarReservaCarroRequestDTO").path("ip").asText());
    }
}
