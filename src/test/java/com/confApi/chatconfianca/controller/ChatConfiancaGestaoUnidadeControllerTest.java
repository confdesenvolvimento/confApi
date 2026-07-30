package com.confApi.chatconfianca.controller;

import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.chatconfianca.dto.request.DepartamentoUnidadeConfiguracaoMassaRequest;
import com.confApi.chatconfianca.dto.request.DepartamentoUnidadeConfiguracaoRequest;
import com.confApi.chatconfianca.dto.request.DepartamentoUnidadeVinculosRequest;
import com.confApi.chatconfianca.dto.response.DepartamentoUnidadeConfiguracaoMassaResponse;
import com.confApi.chatconfianca.service.ChatConfiancaGestaoUnidadeService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatConfiancaGestaoUnidadeControllerTest {

    @Mock
    private ChatConfiancaGestaoUnidadeService service;

    private MockMvc mockMvc;

    @BeforeEach
    void prepararController() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ChatConfiancaGestaoUnidadeController(service))
                .build();
    }

    @Test
    void recebeSelecaoDoPickListNoEndpointDeVinculos() throws Exception {
        DepartamentoUnidade vinculo = new DepartamentoUnidade();
        vinculo.setId(11L);
        vinculo.setDepartamentoId(4L);
        vinculo.setCodgUnidade(1);
        when(service.salvarVinculosDepartamentoUnidades(
                eq(4389), any(DepartamentoUnidadeVinculosRequest.class)))
                .thenReturn(List.of(vinculo));

        mockMvc.perform(post("/v1/chat-confianca/gestao-unidade/departamento-unidades/vinculos")
                        .param("codgUsuario", "4389")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departamentoId\":4,\"codigosUnidade\":[1,2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11));

        ArgumentCaptor<DepartamentoUnidadeVinculosRequest> captor =
                ArgumentCaptor.forClass(DepartamentoUnidadeVinculosRequest.class);
        verify(service).salvarVinculosDepartamentoUnidades(eq(4389), captor.capture());
        assertEquals(4L, captor.getValue().getDepartamentoId());
        assertEquals(List.of(1, 2), captor.getValue().getCodigosUnidade());
    }

    @Test
    void recebeConfiguracaoIndividualNoEndpointDedicado() throws Exception {
        DepartamentoUnidade vinculo = new DepartamentoUnidade();
        vinculo.setId(11L);
        vinculo.setNomeExibicao("Atendimento CGB");
        when(service.salvarConfiguracaoDepartamentoUnidade(
                eq(4389), eq(11L), any(DepartamentoUnidadeConfiguracaoRequest.class)))
                .thenReturn(vinculo);

        mockMvc.perform(post("/v1/chat-confianca/gestao-unidade/departamento-unidades/11/configuracao")
                        .param("codgUsuario", "4389")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeExibicao\":\"Atendimento CGB\","
                                + "\"distribuicao\":\"MENOR_FILA\","
                                + "\"limiteChatsPorAtendente\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.nomeExibicao").value("Atendimento CGB"));

        ArgumentCaptor<DepartamentoUnidadeConfiguracaoRequest> captor =
                ArgumentCaptor.forClass(DepartamentoUnidadeConfiguracaoRequest.class);
        verify(service).salvarConfiguracaoDepartamentoUnidade(eq(4389), eq(11L), captor.capture());
        assertEquals("Atendimento CGB", captor.getValue().getNomeExibicao());
        assertEquals(5, captor.getValue().getLimiteChatsPorAtendente());
    }

    @Test
    void recebeConfiguracaoMassaNoEndpointDedicado() throws Exception {
        DepartamentoUnidadeConfiguracaoMassaResponse resposta =
                new DepartamentoUnidadeConfiguracaoMassaResponse();
        resposta.setTotalUnidadesAtualizadas(57);
        when(service.salvarConfiguracaoMassaDepartamentoUnidades(
                eq(4389), eq(4L), any(DepartamentoUnidadeConfiguracaoMassaRequest.class)))
                .thenReturn(resposta);

        mockMvc.perform(post("/v1/chat-confianca/gestao-unidade/departamentos/4/unidades/configuracao")
                        .param("codgUsuario", "4389")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alterarDistribuicao\":true,"
                                + "\"distribuicao\":\"MENOR_FILA\","
                                + "\"alterarMensagemForaHorario\":true,"
                                + "\"mensagemForaHorario\":\"Retornaremos em breve.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUnidadesAtualizadas").value(57));

        ArgumentCaptor<DepartamentoUnidadeConfiguracaoMassaRequest> captor =
                ArgumentCaptor.forClass(DepartamentoUnidadeConfiguracaoMassaRequest.class);
        verify(service).salvarConfiguracaoMassaDepartamentoUnidades(eq(4389), eq(4L), captor.capture());
        assertEquals(true, captor.getValue().getAlterarDistribuicao());
        assertEquals(true, captor.getValue().getAlterarMensagemForaHorario());
        assertEquals("Retornaremos em breve.", captor.getValue().getMensagemForaHorario());
    }

    @Test
    void naoExpoeEdicaoIndividualLegadaDeDepartamentoUnidade() throws Exception {
        mockMvc.perform(post("/v1/chat-confianca/gestao-unidade/departamento-unidades")
                        .param("codgUsuario", "4389")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/v1/chat-confianca/gestao-unidade/departamento-unidades/11")
                        .param("codgUsuario", "4389"))
                .andExpect(status().isNotFound());
    }
}
