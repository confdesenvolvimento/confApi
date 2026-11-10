package com.confApi.carros;

import com.confApi.carros.dto.CarroCompraModel;
import com.confApi.carros.dto.CarroReservaOperacaoResponseDTO;
import com.confApi.db.confManager.carro.CarroReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CarroReservaIpControllerTest {
    private CarroReservaService service;
    private MockMvc mvc;
    private final ObjectMapper json = new ObjectMapper();
    private static final String RECUSA = "A loja não está aberta no horário selecionado.";

    @BeforeEach void setup() {
        service = mock(CarroReservaService.class);
        var controller = new CarrosController(mock(CarrosService.class));
        ReflectionTestUtils.setField(controller, "carroReservaService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(service.reservar(any())).thenReturn(CarroReservaOperacaoResponseDTO.erro(RECUSA));
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"  "})
    void completaIpAusenteAntesDaReservaEPreservaAMensagem(String ip) throws Exception {
        var body = json.createObjectNode();
        body.putObject("reservaCarro").put("token", "selecao").put("ipEmissao", ip);
        mvc.perform(post("/v1/carro/reservar").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(body)).header("X-Forwarded-For", "198.51.100.99")
                .with(req -> { req.setRemoteAddr("192.0.2.10"); return req; }))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.mensagem").value(RECUSA));
        var capture = ArgumentCaptor.forClass(CarroCompraModel.class);
        verify(service).reservar(capture.capture());
        assertEquals("192.0.2.10", capture.getValue().getReservaCarro().getIpEmissao());
        assertEquals("selecao", capture.getValue().getReservaCarro().getToken());
    }

    @Test void preservaIpOriginalIncluidoPeloGateway() throws Exception {
        mvc.perform(post("/v1/carro/reservar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reservaCarro\":{\"ipEmissao\":\"192.0.2.10\"}}")
                .with(req -> { req.setRemoteAddr("127.0.0.1"); return req; }))
                .andExpect(status().isOk());
        var capture = ArgumentCaptor.forClass(CarroCompraModel.class);
        verify(service).reservar(capture.capture());
        assertEquals("192.0.2.10", capture.getValue().getReservaCarro().getIpEmissao());
    }

    @Test void requestSemReservaContinuaNaValidacaoExistente() throws Exception {
        mvc.perform(post("/v1/carro/reservar").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        var capture = ArgumentCaptor.forClass(CarroCompraModel.class);
        verify(service).reservar(capture.capture());
        assertNull(capture.getValue().getReservaCarro());
    }
}
