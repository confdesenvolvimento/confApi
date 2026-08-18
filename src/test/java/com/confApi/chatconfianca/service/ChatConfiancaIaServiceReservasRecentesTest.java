package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.dto.model.Conversa;
import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatconfianca.dto.enums.RemetenteTipo;
import com.confApi.chatconfianca.dto.request.PerguntarConfiaRequest;
import com.confApi.chatconfianca.dto.response.ChatConfiancaIaResponse;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.chatgpt.dto.ChatActionDTO;
import com.confApi.chatgpt.dto.ChatMessageDTO;
import com.confApi.chatgpt.dto.ChatRequestDTO;
import com.confApi.chatgpt.dto.ChatResponseDTO;
import com.confApi.chatgpt.dto.ToolCallDTO;
import com.confApi.chatgpt.profile.ProfilePromptRegistry;
import com.confApi.chatgpt.service.ChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    void devePersistirMelhoresTarifasEAcoesNaMensagemBot() throws Exception {
        PerguntarConfiaRequest request = request(
                "Qual o dia mais barato de CGB para MCO?");
        prepararContexto(request);

        ChatActionDTO pesquisar = new ChatActionDTO(
                "pesquisar_voos",
                "Pesquisar Executiva - 12/02",
                "R$ 3.500,60 em 12/02/2027",
                "?origem=CGB&destino=MCO&dataIda=2027-02-12&cabine=C",
                false,
                false,
                false,
                "Pesquisar voo de CGB para MCO em 2027-02-12 na cabine Executiva");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schema", "chat.melhores-tarifas-aereas.v1");
        payload.put("tipo", "melhores_tarifas_aereas");
        payload.put("status", "OK");
        payload.put("moeda", "BRL");
        payload.put("origem", "CGB");
        payload.put("destino", "MCO");
        payload.put("actions", List.of(pesquisar));

        when(profiles.systemPrompt(anyString(), anyLong(), anyLong())).thenReturn("prompt");
        when(chatService.actionApis(anyList(), any())).thenReturn(List.of());
        when(chatService.extrairAcoesDisponiveis(anyList())).thenReturn(List.of());
        when(chatService.isConsultaMelhorTarifaAerea(
                eq(request.getMensagem()), anyList(), eq(false)))
                .thenReturn(true);
        when(chatService.chat(any(), anyList(), isNull())).thenReturn(new ChatResponseDTO(
                "resp-tarifa",
                "A menor tarifa de CGB para MCO é em 12/02/2027: R$ 3.500,60.",
                List.of(new ToolCallDTO("search_cheapest_airfares", payload)),
                null,
                List.of(),
                List.of(),
                List.of(pesquisar)));
        when(chatConfiancaService.registrarMensagemBot(eq(10L), anyString(), anyString()))
                .thenReturn(new Mensagem());

        ChatConfiancaIaResponse response = service.perguntar(request);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatConfiancaService).registrarMensagemBot(
                eq(10L), eq(response.getResposta()), jsonCaptor.capture());
        JsonNode conteudoJson = mapper.readTree(jsonCaptor.getValue());
        JsonNode tarifas = conteudoJson.path("melhoresTarifasAereas");
        assertEquals("chat.melhores-tarifas-aereas.v1", tarifas.path("schema").asText());
        assertEquals("BRL", tarifas.path("moeda").asText());
        assertEquals("CGB", tarifas.path("origem").asText());
        assertEquals("pesquisar_voos", conteudoJson.path("actions").get(0)
                .path("code").asText());
        assertEquals(1, response.getActions().size());
    }

    @Test
    void devePersistirPayloadProprioDeTarifasIdaVolta() throws Exception {
        PerguntarConfiaRequest request = request(
                "Qual a menor tarifa ida e volta de CGB para MCO?");
        prepararContexto(request);
        ChatActionDTO pesquisar = new ChatActionDTO(
                "pesquisar_voos", "Pesquisar estas datas - 10/09 a 17/09",
                "R$ 2.000,00 de total combinado",
                "?origem=CGB&destino=MCO&dataIda=2026-09-10&dataVolta=2026-09-17&qtdADT=1&qtdCHD=0&qtdINF=0",
                false, false, false,
                "Pesquise ida e volta de CGB para MCO nessas datas.");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schema", "chat.melhores-tarifas-aereas-ida-volta.v1");
        payload.put("tipo", "melhores_tarifas_aereas_ida_volta");
        payload.put("status", "OK");
        payload.put("origem", "CGB");
        payload.put("destino", "MCO");
        payload.put("actions", List.of(pesquisar));

        when(profiles.systemPrompt(anyString(), anyLong(), anyLong())).thenReturn("prompt");
        when(chatService.actionApis(anyList(), any())).thenReturn(List.of());
        when(chatService.extrairAcoesDisponiveis(anyList())).thenReturn(List.of());
        when(chatService.isConsultaMelhorTarifaAereaIdaVolta(
                eq(request.getMensagem()), anyList(), eq(false), isNull()))
                .thenReturn(true);
        when(chatService.chat(any(), anyList(), isNull())).thenReturn(new ChatResponseDTO(
                "resp-rt", "O menor total combinado e R$ 2.000,00.",
                List.of(new ToolCallDTO("search_cheapest_roundtrip_airfares", payload)),
                null, List.of(), List.of(), List.of(pesquisar)));
        when(chatConfiancaService.registrarMensagemBot(eq(10L), anyString(), anyString()))
                .thenReturn(new Mensagem());

        service.perguntar(request);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatConfiancaService).registrarMensagemBot(
                eq(10L), eq("O menor total combinado e R$ 2.000,00."),
                jsonCaptor.capture());
        JsonNode json = mapper.readTree(jsonCaptor.getValue());
        assertEquals("chat.melhores-tarifas-aereas-ida-volta.v1",
                json.path("melhoresTarifasAereasIdaVolta").path("schema").asText());
        assertTrue(json.path("melhoresTarifasAereas").isMissingNode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void deveReaproveitarSomenteAllowlistDoContextoIdaVolta() throws Exception {
        PerguntarConfiaRequest request = request("E em fevereiro na executiva?");
        prepararContexto(request);
        Mensagem anterior = new Mensagem();
        anterior.setRemetenteTipo(RemetenteTipo.BOT);
        anterior.setConteudo("Para 1 adulto, ida e volta: total combinado de R$ 2.000,00.");
        anterior.setConteudoJson("""
                {"melhoresTarifasAereasIdaVolta":{
                  "schema":"chat.melhores-tarifas-aereas-ida-volta.v1",
                  "origem":"CGB","destino":"MCO","cabine":"Y",
                  "dataIdaInicio":"2027-02-01","dataIdaFim":"2027-02-28",
                  "dataVoltaInicio":"2027-03-01","dataVoltaFim":"2027-03-31",
                  "duracaoMinimaDias":5,"duracaoMaximaDias":12,
                  "politicaCompanhia":"comparar","modoResposta":"cabines",
                  "quantidadeAplicada":4,"melhorGeral":{"total":2000.00},
                  "actions":[{"code":"pesquisar_voos"}]
                }}
                """);
        when(chatConfiancaService.listarMensagens(10L, 7, false, false))
                .thenReturn(List.of(anterior));
        when(profiles.systemPrompt(anyString(), anyLong(), anyLong())).thenReturn("prompt");
        when(chatService.actionApis(anyList(), any())).thenReturn(List.of());
        when(chatService.extrairAcoesDisponiveis(anyList())).thenReturn(List.of());
        when(chatService.isConsultaMelhorTarifaAereaIdaVolta(
                eq(request.getMensagem()), anyList(), eq(true), eq("ida_volta")))
                .thenReturn(true);
        when(chatService.chat(any(), anyList(), isNull())).thenReturn(new ChatResponseDTO(
                "resp-contexto-rt", "Resposta atualizada.", new ArrayList<>(), null,
                List.of(), List.of(), List.of()));
        when(chatConfiancaService.registrarMensagemBot(eq(10L), anyString(), anyString()))
                .thenReturn(new Mensagem());

        service.perguntar(request);

        ArgumentCaptor<ChatRequestDTO> captor = ArgumentCaptor.forClass(ChatRequestDTO.class);
        verify(chatService).chat(captor.capture(), anyList(), isNull());
        Map<String, Object> contexto = (Map<String, Object>) captor.getValue().metadata()
                .get("contextoLocalMelhoresTarifasAereas");
        assertEquals("ida_volta", contexto.get("tipoViagem"));
        assertEquals("CGB", contexto.get("origem"));
        assertEquals("MCO", contexto.get("destino"));
        assertEquals("2027-02-01", contexto.get("dataIdaInicio"));
        assertEquals("2027-03-31", contexto.get("dataVoltaFim"));
        assertEquals(5, contexto.get("duracaoMinimaDias"));
        assertEquals(12, contexto.get("duracaoMaximaDias"));
        assertEquals("comparar", contexto.get("politicaCompanhia"));
        assertTrue(!contexto.containsKey("modoResposta"));
        assertTrue(!contexto.containsKey("melhorGeral") && !contexto.containsKey("actions"));
        assertEquals("search_cheapest_roundtrip_airfares",
                captor.getValue().tools().get(0).name());
    }

    @Test
    @SuppressWarnings("unchecked")
    void deveReaproveitarSomenteFiltrosValidadosDaUltimaConsultaDeTarifas() throws Exception {
        PerguntarConfiaRequest request = request("E na executiva?");
        prepararContexto(request);
        Mensagem anterior = new Mensagem();
        anterior.setRemetenteTipo(RemetenteTipo.BOT);
        anterior.setConteudo("A menor tarifa de CGB para BSB é R$ 900,00.");
        anterior.setConteudoJson("""
                {"melhoresTarifasAereas":{
                  "schema":"chat.melhores-tarifas-aereas.v1",
                  "origem":"CGB","destino":"BSB","cabine":"Y",
                  "periodoInicio":"2027-01-01","periodoFim":"2027-01-31",
                  "modoResposta":"alternativas","quantidadeAplicada":5,
                  "melhorGeral":{"total":900.00},
                  "actions":[{"code":"pesquisar_voos"}]
                }}
                """);
        when(chatConfiancaService.listarMensagens(10L, 7, false, false))
                .thenReturn(List.of(anterior));
        when(profiles.systemPrompt(anyString(), anyLong(), anyLong())).thenReturn("prompt");
        when(chatService.actionApis(anyList(), any())).thenReturn(List.of());
        when(chatService.extrairAcoesDisponiveis(anyList())).thenReturn(List.of());
        when(chatService.isConsultaMelhorTarifaAerea(
                eq(request.getMensagem()), anyList(), eq(true)))
                .thenReturn(true);
        when(chatService.chat(any(), anyList(), isNull())).thenReturn(new ChatResponseDTO(
                "resp-contexto", "Resposta atualizada.", new ArrayList<>(), null,
                List.of(), List.of(), List.of()));
        when(chatConfiancaService.registrarMensagemBot(eq(10L), anyString(), anyString()))
                .thenReturn(new Mensagem());

        service.perguntar(request);

        ArgumentCaptor<ChatRequestDTO> requestCaptor =
                ArgumentCaptor.forClass(ChatRequestDTO.class);
        verify(chatService).chat(requestCaptor.capture(), anyList(), isNull());
        Map<String, Object> metadata = requestCaptor.getValue().metadata();
        Map<String, Object> contexto = (Map<String, Object>) metadata.get(
                "contextoLocalMelhoresTarifasAereas");
        assertEquals("CGB", contexto.get("origem"));
        assertEquals("BSB", contexto.get("destino"));
        assertEquals("Y", contexto.get("cabine"));
        assertEquals("2027-01-01", contexto.get("periodoInicio"));
        assertEquals("2027-01-31", contexto.get("periodoFim"));
        assertEquals("alternativas", contexto.get("modoResposta"));
        assertEquals(5, contexto.get("limiteAlternativas"));
        assertTrue(!contexto.containsKey("melhorGeral") && !contexto.containsKey("actions"));
        assertEquals("search_cheapest_airfares",
                requestCaptor.getValue().tools().get(0).name());
        verify(chatService).isConsultaMelhorTarifaAerea(
                eq(request.getMensagem()), anyList(), eq(true));
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
