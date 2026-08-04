package com.confApi.chatconfianca.controller;

import com.confApi.chatconfianca.dto.remarcacao.ReservasEmitidasRemarcacaoResponse;
import com.confApi.chatconfianca.service.ChatConfiancaRemarcacaoService;
import com.confApi.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatConfiancaRemarcacaoControllerTest {
    @Mock
    private ChatConfiancaRemarcacaoService service;

    private MockMvc mockMvc;
    private Authentication clientePayaraAutenticado;

    @BeforeEach
    void setUp() {
        clientePayaraAutenticado = new UsernamePasswordAuthenticationToken(
                "api.confplus", null, List.of());
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ChatConfiancaRemarcacaoController(service, "api.confplus"))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void deveReceberFiltrosDoPayaraSemExporFiltroDeAgencia() throws Exception {
        ReservasEmitidasRemarcacaoResponse.Item item = new ReservasEmitidasRemarcacaoResponse.Item();
        item.setReservaId(501);
        item.setLocalizador("ABC123");
        item.setStatus(3);
        ReservasEmitidasRemarcacaoResponse.Voo voo =
                new ReservasEmitidasRemarcacaoResponse.Voo();
        voo.setCompanhiaIata("G3");
        voo.setNumeroVoo("1615");
        voo.setOrigem("CGB");
        voo.setDestino("GRU");
        voo.setDataHoraPartida(LocalDateTime.of(2026, 8, 22, 10, 10));
        voo.setDataHoraChegada(LocalDateTime.of(2026, 8, 22, 13, 30));
        item.setVoos(List.of(voo));
        ReservasEmitidasRemarcacaoResponse.Bilhete bilhete =
                new ReservasEmitidasRemarcacaoResponse.Bilhete();
        bilhete.setNumero("1271234567890");
        bilhete.setPassageiroNome("Maria da Silva");
        item.setBilhetes(List.of(bilhete));
        ReservasEmitidasRemarcacaoResponse response = new ReservasEmitidasRemarcacaoResponse();
        response.setItems(List.of(item));
        response.setPage(2);
        response.setSize(25);
        response.setTotalElements(1L);
        response.setTotalPages(1);

        when(service.listarReservasEmitidas(
                20L,
                101,
                "ABC 123",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                2,
                25)).thenReturn(response);

        mockMvc.perform(get("/v1/chat-confianca/remarcacoes/reservas-emitidas")
                        .param("conversaId", "20")
                        .param("codgUsuario", "101")
                        .param("busca", "ABC 123")
                        .param("dataEmissaoInicio", "2026-07-01")
                        .param("dataEmissaoFim", "2026-07-31")
                        .param("page", "2")
                        .param("size", "25")
                        .principal(clientePayaraAutenticado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].reservaId").value(501))
                .andExpect(jsonPath("$.items[0].localizador").value("ABC123"))
                .andExpect(jsonPath("$.items[0].voos[0].companhiaIata").value("G3"))
                .andExpect(jsonPath("$.items[0].voos[0].numeroVoo").value("1615"))
                .andExpect(jsonPath("$.items[0].voos[0].origem").value("CGB"))
                .andExpect(jsonPath("$.items[0].voos[0].destino").value("GRU"))
                .andExpect(jsonPath("$.items[0].bilhetes[0].numero").value("1271234567890"))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(25));

        verify(service).listarReservasEmitidas(
                20L,
                101,
                "ABC 123",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                2,
                25);
    }

    @Test
    void deveRejeitarClienteDiferenteDoPayaraEmTodosOsEndpointsAntesDoService() throws Exception {
        Authentication clienteNaoAutorizado = new UsernamePasswordAuthenticationToken(
                "usuario.teste", null, List.of());
        List<RequestBuilder> requisicoes = List.of(
                get("/v1/chat-confianca/remarcacoes/reservas-emitidas")
                        .param("conversaId", "20")
                        .param("codgUsuario", "202")
                        .principal(clienteNaoAutorizado),
                post("/v1/chat-confianca/remarcacoes/iniciar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conversaId\":20,\"codgUsuario\":202,\"reservaId\":501,\"localizador\":\"ABC123\"}")
                        .principal(clienteNaoAutorizado),
                post("/v1/chat-confianca/remarcacoes/70/trecho")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codgUsuario\":202,\"trechoIndice\":0}")
                        .principal(clienteNaoAutorizado),
                post("/v1/chat-confianca/remarcacoes/70/passageiros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codgUsuario\":202,\"escopo\":\"TODOS\"}")
                        .principal(clienteNaoAutorizado),
                post("/v1/chat-confianca/remarcacoes/70/pesquisar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codgUsuario\":202,\"data\":\"2026-08-01\"}")
                        .principal(clienteNaoAutorizado),
                post("/v1/chat-confianca/remarcacoes/70/simular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codgUsuario\":202,\"opcaoIndice\":0,\"familiaIndice\":0}")
                        .principal(clienteNaoAutorizado),
                post("/v1/chat-confianca/remarcacoes/70/forma-pagamento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codgUsuario\":202,\"codigo\":2}")
                        .principal(clienteNaoAutorizado),
                post("/v1/chat-confianca/remarcacoes/70/encaminhar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codgUsuario\":202}")
                        .principal(clienteNaoAutorizado),
                get("/v1/chat-confianca/remarcacoes/70")
                        .param("codgUsuario", "202")
                        .principal(clienteNaoAutorizado));

        for (RequestBuilder requisicao : requisicoes) {
            mockMvc.perform(requisicao).andExpect(status().isForbidden());
        }

        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarAutenticacaoAusenteAntesDoService() throws Exception {
        mockMvc.perform(get("/v1/chat-confianca/remarcacoes/reservas-emitidas")
                        .param("conversaId", "20")
                        .param("codgUsuario", "101"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void deveAceitarUsuarioFinalDiferenteDoPrincipalTecnicoEDelegarValidacaoAoService() throws Exception {
        ReservasEmitidasRemarcacaoResponse response = new ReservasEmitidasRemarcacaoResponse();
        when(service.listarReservasEmitidas(20L, 202, null, null, null, 0, 10))
                .thenReturn(response);

        mockMvc.perform(get("/v1/chat-confianca/remarcacoes/reservas-emitidas")
                        .param("conversaId", "20")
                        .param("codgUsuario", "202")
                        .principal(clientePayaraAutenticado))
                .andExpect(status().isOk());

        verify(service).listarReservasEmitidas(20L, 202, null, null, null, 0, 10);
    }
}
