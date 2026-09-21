package com.confApi.chatconfianca.controller;

import com.confApi.chatconfianca.dto.remarcacao.RemarcacaoRequest;
import com.confApi.chatconfianca.dto.remarcacao.RemarcacaoSimulacaoResponse;
import com.confApi.chatconfianca.dto.remarcacao.ReservasEmitidasRemarcacaoResponse;
import com.confApi.chatconfianca.dto.remarcacao.RemarcacaoRequest;
import com.confApi.chatconfianca.dto.remarcacao.RemarcacaoSimulacaoResponse;
import org.mockito.ArgumentCaptor;
import com.confApi.chatconfianca.service.ChatConfiancaRemarcacaoService;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.endPoints.usuario.UsuarioApi;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatConfiancaRemarcacaoControllerTest {
    @Mock
    private ChatConfiancaRemarcacaoService service;
    @Mock
    private UsuarioApi usuarioApi;

    private MockMvc mockMvc;
    private Authentication clientePayaraAutenticado;

    @BeforeEach
    void setUp() {
        clientePayaraAutenticado = new UsernamePasswordAuthenticationToken(
                "api.confplus", null, List.of());
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ChatConfiancaRemarcacaoController(
                        service, "api.confplus", "api.mobile", usuarioApi))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void deveReceberVoltarComVersaoERetornarEstadoRestaurado() throws Exception {
        RemarcacaoSimulacaoResponse response = new RemarcacaoSimulacaoResponse();
        response.setId(70L);
        response.setVersao(8);
        response.setStatus("AGUARDANDO_CRITERIOS");
        response.setPermiteVoltar(true);
        response.setLabelVoltar("Voltar para os passageiros");
        response.setPreferenciasRestauradas(true);
        when(service.voltar(eq(70L), any())).thenReturn(response);

        mockMvc.perform(post("/v1/chat-confianca/remarcacoes/70/voltar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codgUsuario\":202,\"versaoEsperada\":7}")
                        .principal(clientePayaraAutenticado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value(8))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_CRITERIOS"))
                .andExpect(jsonPath("$.permiteVoltar").value(true))
                .andExpect(jsonPath("$.labelVoltar").value("Voltar para os passageiros"))
                .andExpect(jsonPath("$.preferenciasRestauradas").value(true));
        ArgumentCaptor<RemarcacaoRequest.Voltar> request =
                ArgumentCaptor.forClass(RemarcacaoRequest.Voltar.class);
        verify(service).voltar(eq(70L), request.capture());
        org.junit.jupiter.api.Assertions.assertEquals(202, request.getValue().getCodgUsuario());
        org.junit.jupiter.api.Assertions.assertEquals(7, request.getValue().getVersaoEsperada());
    }

    @Test
    void deveBloquearVoltarSemAutenticacaoAntesDoService() throws Exception {
        mockMvc.perform(post("/v1/chat-confianca/remarcacoes/70/voltar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codgUsuario\":202,\"versaoEsperada\":7}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void deveReceberSelecaoExplicitaIdaVoltaPreservandoContratoLegado() throws Exception {
        RemarcacaoSimulacaoResponse response = new RemarcacaoSimulacaoResponse();
        response.setId(70L);
        response.setVersao(3);
        response.setRemarcacaoConjunta(true);
        when(service.selecionarTrecho(eq(70L), any())).thenReturn(response);
        mockMvc.perform(post("/v1/chat-confianca/remarcacoes/70/trecho")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codgUsuario\":202,\"trechosIndices\":[0,1]}")
                        .principal(clientePayaraAutenticado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remarcacaoConjunta").value(true))
                .andExpect(jsonPath("$.versao").value(3));
        ArgumentCaptor<RemarcacaoRequest.SelecionarTrecho> request =
                ArgumentCaptor.forClass(RemarcacaoRequest.SelecionarTrecho.class);
        verify(service).selecionarTrecho(eq(70L), request.capture());
        org.junit.jupiter.api.Assertions.assertEquals(List.of(0, 1), request.getValue().getTrechosIndices());
        org.junit.jupiter.api.Assertions.assertNull(request.getValue().getTrechoIndice());
        org.junit.jupiter.api.Assertions.assertEquals(202, request.getValue().getCodgUsuario());
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
    void deveRejeitarJwtMobileQuandoCodgUsuarioNaoPertenceAoPrincipal() throws Exception {
        Authentication clienteNaoAutorizado = new UsernamePasswordAuthenticationToken(
                "usuario.teste", null, List.of());
        Usuario usuarioAutenticado = new Usuario();
        usuarioAutenticado.setCodgUsuario(303);
        when(usuarioApi.consultaUsuarioByLogin("usuario.teste")).thenReturn(usuarioAutenticado);
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
                post("/v1/chat-confianca/remarcacoes/70/voltar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codgUsuario\":202,\"versaoEsperada\":7}")
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
    void deveAceitarJwtMobileQuandoCodgUsuarioPertenceAoPrincipal() throws Exception {
        Authentication clienteMobile = new UsernamePasswordAuthenticationToken(
                "usuario.mobile", null, List.of());
        Usuario usuarioAutenticado = new Usuario();
        usuarioAutenticado.setCodgUsuario(202);
        when(usuarioApi.consultaUsuarioByLogin("usuario.mobile")).thenReturn(usuarioAutenticado);
        ReservasEmitidasRemarcacaoResponse response = new ReservasEmitidasRemarcacaoResponse();
        when(service.listarReservasEmitidas(20L, 202, null, null, null, 0, 10))
                .thenReturn(response);

        mockMvc.perform(get("/v1/chat-confianca/remarcacoes/reservas-emitidas")
                        .param("conversaId", "20")
                        .param("codgUsuario", "202")
                        .principal(clienteMobile))
                .andExpect(status().isOk());

        verify(service).listarReservasEmitidas(20L, 202, null, null, null, 0, 10);
    }

    @Test
    void deveAceitarJwtMobileAoIniciarSimulacaoDoProprioUsuario() throws Exception {
        Authentication clienteMobile = new UsernamePasswordAuthenticationToken(
                "usuario.mobile", null, List.of());
        Usuario usuarioAutenticado = new Usuario();
        usuarioAutenticado.setCodgUsuario(202);
        when(usuarioApi.consultaUsuarioByLogin("usuario.mobile")).thenReturn(usuarioAutenticado);
        when(service.iniciar(any(RemarcacaoRequest.Iniciar.class)))
                .thenReturn(new RemarcacaoSimulacaoResponse());

        mockMvc.perform(post("/v1/chat-confianca/remarcacoes/iniciar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conversaId\":20,\"codgUsuario\":202,\"reservaId\":501,\"localizador\":\"ABC123\"}")
                        .principal(clienteMobile))
                .andExpect(status().isOk());

        verify(service).iniciar(any(RemarcacaoRequest.Iniciar.class));
    }

    @Test
    void deveAceitarClienteTecnicoMobileEDelegarUsuarioLogadoAoService() throws Exception {
        Authentication clienteTecnicoMobile = new UsernamePasswordAuthenticationToken(
                "api.mobile", null, List.of());
        when(service.iniciar(any(RemarcacaoRequest.Iniciar.class)))
                .thenReturn(new RemarcacaoSimulacaoResponse());

        mockMvc.perform(post("/v1/chat-confianca/remarcacoes/iniciar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conversaId\":20,\"codgUsuario\":202,\"reservaId\":501,\"localizador\":\"ABC123\"}")
                        .principal(clienteTecnicoMobile))
                .andExpect(status().isOk());

        verify(service).iniciar(any(RemarcacaoRequest.Iniciar.class));
        verifyNoInteractions(usuarioApi);
    }

    @Test
    void deveRejeitarJwtMobileQuandoUsuarioNaoForLocalizado() throws Exception {
        Authentication clienteMobile = new UsernamePasswordAuthenticationToken(
                "usuario.inexistente", null, List.of());
        when(usuarioApi.consultaUsuarioByLogin("usuario.inexistente")).thenReturn(new Usuario());

        mockMvc.perform(get("/v1/chat-confianca/remarcacoes/reservas-emitidas")
                        .param("conversaId", "20")
                        .param("codgUsuario", "202")
                        .principal(clienteMobile))
                .andExpect(status().isForbidden());

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
