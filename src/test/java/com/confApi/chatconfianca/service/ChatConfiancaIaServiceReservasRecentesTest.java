package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.dto.model.Conversa;
import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatconfianca.dto.request.PerguntarConfiaRequest;
import com.confApi.chatconfianca.dto.response.ChatConfiancaIaResponse;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.chatgpt.dto.ChatActionDTO;
import com.confApi.chatgpt.dto.ChatMessageDTO;
import com.confApi.chatgpt.dto.ChatResponseDTO;
import com.confApi.chatgpt.profile.ProfilePromptRegistry;
import com.confApi.chatgpt.service.ChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChatConfiancaIaServiceReservasRecentesTest {
    private ChatConfiancaService chatConfiancaService;
    private ChatService chatService;
    private ProfilePromptRegistry profiles;
    private ObjectMapper mapper;
    private ChatConfiancaIaService service;

    @BeforeEach
    void setUp() {
        chatConfiancaService = mock(ChatConfiancaService.class);
        chatService = mock(ChatService.class);
        profiles = mock(ProfilePromptRegistry.class);
        mapper = new ObjectMapper().findAndRegisterModules();
        service = new ChatConfiancaIaService(
                chatConfiancaService, chatService, profiles, mapper);
    }

    @Test
    void devePersistirPayloadEstruturadoEAcoesComReservaIdNaMensagemBot() throws Exception {
        PerguntarConfiaRequest request = request("Liste minhas ultimas reservas");
        prepararContexto(request);

        ChatActionDTO abrir = new ChatActionDTO(
                "abrir_reserva",
                "Abrir reserva",
                "Abrir esta reserva",
                "ABC123",
                91,
                false,
                false,
                false,
                "Abrir a reserva ABC123 no sistema.");
        String payload = """
                Dado do sistema (reservas_aereas_recentes_agencia):
                {"schema":"chat.reservas-recentes.v1","reservasRecentes":{"status":"OK","mensagem":"1 reserva recente encontrada.","quantidade":1,"reservas":[{"reservaId":91,"localizador":"ABC123"}]},"actions":[{"code":"abrir_reserva","label":"Abrir reserva","description":"Abrir esta reserva","localizador":"ABC123","reservaId":91,"requiresConfirmation":false,"requiresRules":false,"sensitive":false,"prompt":"Abrir a reserva ABC123 no sistema."}]}
                O painel estruturado exibira os detalhes.
                """;

        when(chatService.isListagemReservasRecentesDeterministica(request.getMensagem()))
                .thenReturn(true);
        when(chatService.responderListagemReservasRecentes(any())).thenReturn(new ChatResponseDTO(
                null, null, new ArrayList<>(), null,
                List.of("ultimas_reservas_aereas"),
                List.of(new ChatMessageDTO("system", payload)),
                List.of(abrir)));
        when(chatConfiancaService.registrarMensagemBot(eq(10L), anyString(), anyString()))
                .thenReturn(new Mensagem());

        ChatConfiancaIaResponse response = service.perguntar(request);

        ArgumentCaptor<String> conteudoCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatConfiancaService).registrarMensagemBot(
                eq(10L), conteudoCaptor.capture(), jsonCaptor.capture());
        JsonNode conteudoJson = mapper.readTree(jsonCaptor.getValue());

        assertEquals("Encontrei 1 reserva recente da sua agencia.", conteudoCaptor.getValue());
        assertEquals("chat.reservas-recentes.v1", conteudoJson.path("schema").asText());
        assertEquals(1, conteudoJson.path("reservasRecentes").path("quantidade").asInt());
        assertEquals(91, conteudoJson.path("reservasRecentes").path("reservas")
                .get(0).path("reservaId").asInt());
        assertTrue(conteudoJson.path("reservasRecentes").path("reservas")
                .get(0).path("actions").isMissingNode());
        assertEquals(91, conteudoJson.path("actions").get(0).path("reservaId").asInt());
        assertTrue(response.getActions().stream()
                .anyMatch(action -> Integer.valueOf(91).equals(action.reservaId())));
        assertTrue(conteudoJson.path("acaoSolicitada").isNull());
        assertNull(response.getAcaoSolicitada());
        verify(chatService, never()).actionApis(anyList(), any());
        verify(chatService, never()).chat(any(), any(), any());
        verify(chatService, never()).identificarTipoConsultaViagem(anyString());
        verifyNoInteractions(profiles);
    }

    @Test
    void deveDefinirEPersistirAcaoSolicitadaSomenteQuandoHaMatching() throws Exception {
        PerguntarConfiaRequest request = request("Abrir reserva ABC123");
        prepararContexto(request);
        ChatActionDTO abrir = new ChatActionDTO(
                "abrir_reserva", "Abrir reserva", "Abrir", "ABC123", null,
                false, false, false, "Abrir a reserva ABC123 no sistema.");
        prepararRespostaIa(request, List.of(abrir), "abrir_reserva");

        ChatConfiancaIaResponse response = service.perguntar(request);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatConfiancaService).registrarMensagemBot(
                eq(10L), eq("Reserva carregada."), jsonCaptor.capture());
        JsonNode conteudoJson = mapper.readTree(jsonCaptor.getValue());
        assertEquals("abrir_reserva", response.getAcaoSolicitada());
        assertEquals("abrir_reserva", conteudoJson.path("acaoSolicitada").asText());
    }

    @Test
    void naoDeveDefinirCancelamentoQuandoAcaoNaoForAplicavel() throws Exception {
        PerguntarConfiaRequest request = request("Cancelar ABC123");
        prepararContexto(request);
        ChatActionDTO abrir = new ChatActionDTO(
                "abrir_reserva", "Abrir reserva", "Abrir", "ABC123", null,
                false, false, false, "Abrir a reserva ABC123 no sistema.");
        prepararRespostaIa(request, List.of(abrir), "preparar_cancelamento");

        ChatConfiancaIaResponse response = service.perguntar(request);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatConfiancaService).registrarMensagemBot(
                eq(10L), eq("Reserva carregada."), jsonCaptor.capture());
        JsonNode conteudoJson = mapper.readTree(jsonCaptor.getValue());
        assertNull(response.getAcaoSolicitada());
        assertTrue(conteudoJson.path("acaoSolicitada").isNull());
        assertTrue(response.getActions().stream().noneMatch(action ->
                "cancelar_reserva".equals(action.code())
                        || "executar_cancelamento".equals(action.code())));
    }

    @Test
    void cancelamentoSolicitadoDeveSerSomentePreparacaoReadOnly() throws Exception {
        PerguntarConfiaRequest request = request("Cancelar ABC123");
        prepararContexto(request);
        ChatActionDTO prepararCancelamento = new ChatActionDTO(
                "preparar_cancelamento", "Cancelar", "Preparar cancelamento", "ABC123", null,
                true, true, true,
                "Nao execute a operacao; consulte regras e peca confirmacao.");
        prepararRespostaIa(request, List.of(prepararCancelamento), "preparar_cancelamento");

        ChatConfiancaIaResponse response = service.perguntar(request);

        assertEquals("preparar_cancelamento", response.getAcaoSolicitada());
        assertTrue(response.getActions().get(0).requiresConfirmation());
        assertTrue(response.getActions().get(0).requiresRules());
        assertTrue(response.getActions().get(0).sensitive());
        assertTrue(response.getActions().stream().noneMatch(action ->
                "cancelar_reserva".equals(action.code())
                        || "executar_cancelamento".equals(action.code())));
    }

    @Test
    void devePersistirFallbackEstruturadoSemChamarModeloQuandoConsultaFalhar() throws Exception {
        PerguntarConfiaRequest request = request("Mostre minhas reservas recentes");
        prepararContexto(request);
        String payload = """
                Dado do sistema (reservas_aereas_recentes_agencia):
                {"schema":"chat.reservas-recentes.v1","reservasRecentes":{"status":"ERRO","mensagem":"Nao foi possivel listar as reservas aereas recentes da agencia agora.","quantidade":0,"reservas":[]},"actions":[]}
                """;

        when(chatService.isListagemReservasRecentesDeterministica(request.getMensagem()))
                .thenReturn(true);
        when(chatService.responderListagemReservasRecentes(any())).thenReturn(new ChatResponseDTO(
                null, null, new ArrayList<>(), null,
                List.of("ultimas_reservas_aereas"),
                List.of(new ChatMessageDTO("system", payload)),
                List.of()));
        when(chatConfiancaService.registrarMensagemBot(eq(10L), anyString(), anyString()))
                .thenReturn(new Mensagem());

        ChatConfiancaIaResponse response = service.perguntar(request);

        ArgumentCaptor<String> conteudoCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatConfiancaService).registrarMensagemBot(
                eq(10L), conteudoCaptor.capture(), jsonCaptor.capture());
        JsonNode conteudoJson = mapper.readTree(jsonCaptor.getValue());

        assertEquals("Nao foi possivel listar as reservas aereas recentes da agencia agora.",
                conteudoCaptor.getValue());
        assertEquals(conteudoCaptor.getValue(), response.getResposta());
        assertEquals("chat.reservas-recentes.v1", conteudoJson.path("schema").asText());
        assertEquals("ERRO", conteudoJson.path("reservasRecentes").path("status").asText());
        assertTrue(conteudoJson.path("actions").isEmpty());
        verify(chatService, never()).actionApis(anyList(), any());
        verify(chatService, never()).chat(any(), any(), any());
        verify(chatService, never()).identificarTipoConsultaViagem(anyString());
        verifyNoInteractions(profiles);
    }

    private PerguntarConfiaRequest request(String mensagem) {
        PerguntarConfiaRequest request = new PerguntarConfiaRequest();
        request.setConversaId(10L);
        request.setCodgUsuario(7);
        request.setCodgAgenciaSessao(321);
        request.setMensagem(mensagem);
        return request;
    }

    private void prepararContexto(PerguntarConfiaRequest request) {
        Conversa conversa = new Conversa();
        conversa.setId(10L);
        conversa.setDepartamentoUnidadeId(20L);
        DepartamentoUnidade departamento = new DepartamentoUnidade();
        departamento.setId(20L);
        departamento.setNomeExibicao("ConfIA Geral");

        when(chatConfiancaService.montarSessao(7, 321)).thenReturn(new SessaoChatResponse());
        when(chatConfiancaService.buscarConversa(10L)).thenReturn(conversa);
        when(chatConfiancaService.listarMensagens(10L, 7, false, false))
                .thenReturn(new ArrayList<>());
        when(chatConfiancaService.listarDepartamentosDisponiveisPorUsuario(7, 321))
                .thenReturn(List.of(departamento));
        when(chatConfiancaService.registrarMensagemUsuarioAssistida(
                10L, 7, request.getMensagem()))
                .thenReturn(new Mensagem());
    }

    private void prepararRespostaIa(PerguntarConfiaRequest request,
                                    List<ChatActionDTO> actions,
                                    String acaoSolicitada) throws Exception {
        when(profiles.systemPrompt(anyString(), anyLong(), anyLong())).thenReturn("prompt");
        when(chatService.actionApis(anyList(), any())).thenReturn(List.of("reserva_aerea_detalhes"));
        when(chatService.extrairAcoesDisponiveis(anyList())).thenReturn(actions);
        when(chatService.chat(any(), anyList(), isNull())).thenReturn(new ChatResponseDTO(
                "resp-1", "Reserva carregada.", new ArrayList<>(), null,
                List.of("reserva_aerea_detalhes"), new ArrayList<>(), List.of()));
        when(chatService.identificarAcaoSolicitadaDeterministica(request.getMensagem()))
                .thenReturn(acaoSolicitada);
        when(chatConfiancaService.registrarMensagemBot(eq(10L), anyString(), anyString()))
                .thenReturn(new Mensagem());
    }
}
