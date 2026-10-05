package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.dto.model.Conversa;
import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatconfianca.dto.model.RefUnidade;
import com.confApi.chatconfianca.dto.enums.RemetenteTipo;
import com.confApi.chatconfianca.dto.request.PerguntarConfiaRequest;
import com.confApi.chatconfianca.dto.response.ChatConfiancaIaResponse;
import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.chatconfianca.intencao.ChatIntencaoClassificacao;
import com.confApi.chatconfianca.intencao.ChatMemoriaRecuperacaoShadowAuditService;
import com.confApi.chatconfianca.intencao.ChatIntencaoShadowService;
import com.confApi.chatconfianca.intencao.ChatIntencaoShadowProperties;
import com.confApi.chatconfianca.intencao.ChatConfiancaDecisaoIaService;
import com.confApi.chatconfianca.intencao.ChatConfiancaDecisaoIa;
import com.confApi.chatconfianca.intencao.ChatIaDecisaoAuditService;
import com.confApi.chatconfianca.intencao.ChatIntencaoRuntimeDto;
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

import java.math.BigDecimal;
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
    private ChatIntencaoShadowService chatIntencaoShadowService;
    private ChatIntencaoShadowProperties decisionProperties;
    private ChatMemoriaRecuperacaoShadowAuditService chatMemoriaRecuperacaoAuditService;
    private ChatIaDecisaoAuditService chatIaDecisaoAuditService;
    private ChatConfiancaIaService service;

    @BeforeEach
    void setUp() {
        chatConfiancaService = mock(ChatConfiancaService.class);
        chatService = mock(ChatService.class);
        profiles = mock(ProfilePromptRegistry.class);
        mapper = new ObjectMapper().findAndRegisterModules();
        chatIntencaoShadowService = mock(ChatIntencaoShadowService.class);
        chatMemoriaRecuperacaoAuditService =
                mock(ChatMemoriaRecuperacaoShadowAuditService.class);
        chatIaDecisaoAuditService = mock(ChatIaDecisaoAuditService.class);
        when(chatIntencaoShadowService.classificar(any(), any(), any()))
                .thenReturn(ChatIntencaoClassificacao.status("DESABILITADO"));
        decisionProperties = new ChatIntencaoShadowProperties();
        ChatConfiancaDecisaoIaService decisaoIaService =
                new ChatConfiancaDecisaoIaService(
                        chatIntencaoShadowService, chatService, decisionProperties);
        service = new ChatConfiancaIaService(
                chatConfiancaService, chatService, profiles, mapper,
                chatIntencaoShadowService, decisaoIaService,
                chatMemoriaRecuperacaoAuditService,
                chatIaDecisaoAuditService);
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
    void remarcacaoNaoEncontradaDeveSomenteInformarSemAcaoMesmoComHistoricoAnterior() throws Exception {
        validarBloqueioRemarcacaoNoTurno("NAO_ENCONTRADA",
                "Nao encontrei a reserva ZZZ999. Confira o localizador e tente novamente.");
    }

    @Test
    void erroDeConsultaDaRemarcacaoDeveInformarFalhaSemDizerQueReservaNaoExiste() throws Exception {
        validarBloqueioRemarcacaoNoTurno("ERRO_CONSULTA",
                "Nao foi possivel consultar a reserva ZZZ999 agora. Tente novamente.");
    }

    @Test
    void bloqueioDeTurnoAnteriorNaoDeveImpedirSeletorDaReservaAtualValidada() throws Exception {
        PerguntarConfiaRequest request = request("Simular remarcacao ABC123");
        prepararContexto(request);
        Mensagem anterior = new Mensagem();
        anterior.setRemetenteTipo(RemetenteTipo.BOT);
        anterior.setConteudo("Nao encontrei a reserva ZZZ999.");
        anterior.setConteudoJson(mapper.writeValueAsString(Map.of(
                "tipoConsulta", "validacao_remarcacao", "statusConsulta", "NAO_ENCONTRADA",
                "localizadorConsultado", "ZZZ999", "mensagem", "Nao encontrei a reserva ZZZ999.",
                "actions", List.of())));
        when(chatConfiancaService.listarMensagens(10L, 7, false, false)).thenReturn(List.of(anterior));
        ChatActionDTO acao = acaoSimularRemarcacao("ABC123");
        prepararRespostaIa(request, List.of(acao), "simular_remarcacao");
        ChatMessageDTO dadoAtual = new ChatMessageDTO("system",
                "{\"tipoConsulta\":\"seletor_remarcacao\",\"localizador\":\"ABC123\"}");
        when(chatService.actionApis(anyList(), any())).thenAnswer(invocation -> {
            List<ChatMessageDTO> mensagens = invocation.getArgument(0);
            mensagens.add(dadoAtual);
            return List.of("simular_remarcacao");
        });
        when(chatService.respostaBloqueioRemarcacao(anyList(), anyList())).thenAnswer(invocation -> {
            List<ChatMessageDTO> dadosDoTurno = invocation.getArgument(0);
            assertEquals(List.of(dadoAtual), dadosDoTurno,
                    "Validacao deve ignorar ausencia de outra reserva em turnos anteriores.");
            return null;
        });

        ChatConfiancaIaResponse response = service.perguntar(request);

        assertEquals("Reserva carregada.", response.getResposta());
        assertEquals(List.of(acao), response.getActions());
        assertEquals("simular_remarcacao", response.getAcaoSolicitada());
        verify(chatService).chat(any(), anyList(), isNull());
        verify(chatService).respostaBloqueioRemarcacao(anyList(), anyList());
    }

    private void validarBloqueioRemarcacaoNoTurno(String status, String mensagem) throws Exception {
        PerguntarConfiaRequest request = request("Simular remarcacao ZZZ999");
        prepararContexto(request);
        ChatActionDTO acaoAntiga = acaoSimularRemarcacao("ABC123");
        Mensagem anterior = new Mensagem();
        anterior.setRemetenteTipo(RemetenteTipo.BOT);
        anterior.setConteudo("Use o seletor para simular a remarcacao ABC123.");
        anterior.setConteudoJson(mapper.writeValueAsString(Map.of(
                "actions", List.of(acaoAntiga), "acaoSolicitada", "simular_remarcacao")));
        when(chatConfiancaService.listarMensagens(10L, 7, false, false)).thenReturn(List.of(anterior));
        when(profiles.systemPrompt(anyString(), anyLong(), anyLong())).thenReturn("prompt");
        ChatMessageDTO dadoAtual = new ChatMessageDTO("system", mapper.writeValueAsString(Map.of(
                "tipoConsulta", "validacao_remarcacao", "statusConsulta", status,
                "localizadorConsultado", "ZZZ999", "mensagem", mensagem, "acoesDisponiveis", List.of())));
        when(chatService.actionApis(anyList(), any())).thenAnswer(invocation -> {
            List<ChatMessageDTO> mensagens = invocation.getArgument(0);
            mensagens.add(dadoAtual);
            return List.of("simular_remarcacao");
        });
        when(chatService.respostaBloqueioRemarcacao(anyList(), anyList())).thenAnswer(invocation -> {
            List<ChatMessageDTO> dadosDoTurno = invocation.getArgument(0);
            assertEquals(List.of(dadoAtual), dadosDoTurno,
                    "Somente a consulta atual pode decidir o bloqueio e suas acoes.");
            return new ChatResponseDTO(null, mensagem, List.of(), null,
                    invocation.getArgument(1), List.copyOf(dadosDoTurno), List.of());
        });
        when(chatService.extrairAcoesDisponiveis(anyList())).thenReturn(List.of(acaoAntiga));
        when(chatService.identificarAcaoSolicitadaDeterministica(request.getMensagem()))
                .thenReturn("simular_remarcacao");
        when(chatConfiancaService.registrarMensagemBot(eq(10L), anyString(), anyString()))
                .thenReturn(new Mensagem());

        ChatConfiancaIaResponse response = service.perguntar(request);

        assertEquals(mensagem, response.getResposta());
        assertTrue(response.getActions().isEmpty());
        assertNull(response.getAcaoSolicitada());
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatConfiancaService).registrarMensagemBot(eq(10L), eq(mensagem), jsonCaptor.capture());
        JsonNode persistido = mapper.readTree(jsonCaptor.getValue());
        assertTrue(persistido.path("actions").isArray());
        assertTrue(persistido.path("actions").isEmpty());
        assertTrue(persistido.path("acaoSolicitada").isNull());
        verify(chatService).respostaBloqueioRemarcacao(anyList(), anyList());
        verify(chatService, never()).chat(any(), anyList(), any());
    }

    private ChatActionDTO acaoSimularRemarcacao(String localizador) {
        return new ChatActionDTO("simular_remarcacao", "Simular remarcacao", "Preparar simulacao",
                localizador, null, false, true, false, "Simular remarcacao " + localizador);
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

    @Test
    void deveSugerirFinanceiroParaBspSemFixarDepartamentoDaConversa() throws Exception {
        PerguntarConfiaRequest request = request("Quero consultar o calendario BSP");
        prepararContexto(request);
        RefUnidade unidade = new RefUnidade();
        unidade.setCodgUnidade(7);
        unidade.setNomeUnidade("Unidade Cuiabá");
        SessaoChatResponse sessao = new SessaoChatResponse();
        sessao.setUnidade(unidade);
        when(chatConfiancaService.montarSessao(7, 321)).thenReturn(sessao);
        ChatIntencaoClassificacao sombra = ChatIntencaoClassificacao.status("CLASSIFICADA");
        sombra.setCodigo("financeiro.calendario_bsp");
        sombra.setIntencaoId(15L);
        sombra.setNome("Calendario BSP");
        sombra.setScore(new BigDecimal("25.000"));
        sombra.setSegundoScore(BigDecimal.ZERO.setScale(3));
        sombra.setConfianca(96);
        sombra.getTermosPositivos().addAll(List.of("calendario BSP", "BSP"));
        when(chatIntencaoShadowService.classificar(eq(request.getMensagem()), any(), any()))
                .thenReturn(sombra);
        DepartamentoUnidade financeiro = new DepartamentoUnidade();
        financeiro.setId(31L);
        financeiro.setNomeExibicao("Financeiro");
        DepartamentoUnidade atendimento = new DepartamentoUnidade();
        atendimento.setId(32L);
        atendimento.setNomeExibicao("Atendimento");
        when(chatConfiancaService.listarDepartamentosRoteamentoPorUsuario(7, 321))
                .thenReturn(List.of(financeiro, atendimento));
        prepararRespostaIa(request, List.of(), null);

        ChatConfiancaIaResponse response = service.perguntar(request);

        assertEquals(31L, response.getDepartamentoSugerido().getId());
        assertTrue(response.getDepartamentoSugeridoConfianca() >= 80);
        assertEquals("financeiro", response.getIntencao());
        assertEquals(20L, response.getConversa().getDepartamentoUnidadeId());
        JsonNode metadados = mapper.readTree(response.getConversa().getMetadadosJson());
        assertEquals(31L, metadados.path("departamentoSugeridoId").asLong());
        assertTrue(metadados.path("departamentoSugeridoConfianca").asInt() >= 80);
        JsonNode classificacao = metadados.path("classificacaoIntencaoShadow");
        assertEquals("SHADOW", classificacao.path("modo").asText());
        assertEquals(15L, classificacao.path("intencaoId").asLong());
        assertEquals("financeiro.calendario_bsp", classificacao.path("intencao").asText());
        assertEquals(96, classificacao.path("confianca").asInt());
        verify(chatIntencaoShadowService).classificar(
                eq(request.getMensagem()), eq(7), eq("Unidade Cuiabá"));
        verify(chatIntencaoShadowService).registrarComparacao(
                eq(10L), eq(30L), eq("financeiro"), eq(sombra));
        verify(chatMemoriaRecuperacaoAuditService).registrar(
                eq(10L), eq(30L), eq("Unidade Cuiabá"), eq(sombra));
    }

    @Test
    void naoDeveEscolherDepartamentoQuandoMensagemNaoTemConfianca() throws Exception {
        PerguntarConfiaRequest request = request("Bom dia, preciso de uma orientacao");
        prepararContexto(request);
        DepartamentoUnidade financeiro = new DepartamentoUnidade();
        financeiro.setId(31L);
        financeiro.setNomeExibicao("Financeiro");
        DepartamentoUnidade atendimento = new DepartamentoUnidade();
        atendimento.setId(32L);
        atendimento.setNomeExibicao("Atendimento");
        when(chatConfiancaService.listarDepartamentosRoteamentoPorUsuario(7, 321))
                .thenReturn(List.of(financeiro, atendimento));
        prepararRespostaIa(request, List.of(), null);

        ChatConfiancaIaResponse response = service.perguntar(request);

        assertNull(response.getDepartamentoSugerido());
        assertNull(response.getDepartamentoSugeridoConfianca());
    }

    @Test
    void decisaoUnificadaDeFaturasUsaConsultaValidadaSemSegundaRespostaLivre()
            throws Exception {
        decisionProperties.setUnifiedDecisionEnabled(true);
        decisionProperties.setUnifiedDecisionCanaryEnabled(false);
        PerguntarConfiaRequest request = request("Quero consultar minhas faturas");
        prepararContexto(request);
        ChatIntencaoClassificacao classificacao = ChatIntencaoClassificacao.status("CLASSIFICADA");
        classificacao.setIntencaoId(40L);
        classificacao.setCodigo("financeiro.faturas");
        classificacao.setNome("Consulta de faturas");
        classificacao.setConfianca(94);
        ChatIntencaoRuntimeDto.Memoria memoria = new ChatIntencaoRuntimeDto.Memoria();
        memoria.setCodgMemoria(70);
        memoria.setTexto("As faturas devem ser apresentadas somente para a agencia autenticada.");
        classificacao.setMemoriasDetalhadas(List.of(memoria));
        classificacao.setMemoriasRecuperadas(List.of(70));
        when(chatIntencaoShadowService.classificar(eq(request.getMensagem()), any(), any()))
                .thenReturn(classificacao);
        when(chatService.responderFaturas(any(), any(), eq(false))).thenReturn(new ChatResponseDTO(
                null,"Faturas consultadas.",List.of(),null,List.of("faturas"),List.of(new ChatMessageDTO("system",
                    "{\"schema\":\"chat.faturas.v1\",\"statusConsulta\":\"DADOS_CONSULTADOS\",\"filtros\":{}}"))));
        when(chatConfiancaService.registrarMensagemBot(eq(10L),anyString(),anyString())).thenReturn(new Mensagem());

        ChatConfiancaIaResponse response = service.perguntar(request);

        assertEquals("financeiro.faturas", response.getIntencao());
        verify(chatService,never()).chat(any(),anyList(),any());
        verify(chatService,never()).actionApis(anyList(),any(),anyString());
        verify(chatService).responderFaturas(any(),any(),eq(false));
        assertEquals("Faturas consultadas.",response.getResposta());
        JsonNode metadados = mapper.readTree(response.getConversa().getMetadadosJson());
        assertTrue(metadados.path("decisaoIa").path("aplicada").asBoolean());
        assertEquals("faturas", metadados.path("decisaoIa").path("acao").asText());
        assertTrue(metadados.path("decisaoIa").path("memoriaIds").isEmpty());
        verify(chatIaDecisaoAuditService).registrar(
                eq(10L), eq(30L), isNull(), eq("Confianca"),
                any(ChatConfiancaDecisaoIa.class), eq(false), eq(false),
                isNull(), anyLong());
    }

    @Test
    void canarioInstitucionalDeveAplicarMemoriaERegistrarElegibilidade() throws Exception {
        decisionProperties.setUnifiedDecisionEnabled(true);
        decisionProperties.setUnifiedDecisionCanaryEnabled(true);
        decisionProperties.setUnifiedDecisionCanaryIntentionPrefixes(List.of("institucional."));
        PerguntarConfiaRequest request = request("Qual e o horario de atendimento?");
        prepararContexto(request);
        ChatIntencaoClassificacao classificacao = ChatIntencaoClassificacao.status("CLASSIFICADA");
        classificacao.setIntencaoId(50L);
        classificacao.setCodigo("institucional.horario_atendimento");
        classificacao.setNome("Horario de atendimento");
        classificacao.setConfianca(96);
        ChatIntencaoRuntimeDto.Memoria memoria = new ChatIntencaoRuntimeDto.Memoria();
        memoria.setCodgMemoria(7);
        memoria.setTexto("Atendimento de segunda a sexta-feira.");
        classificacao.setMemoriasDetalhadas(List.of(memoria));
        classificacao.setMemoriasRecuperadas(List.of(7));
        when(chatIntencaoShadowService.classificar(eq(request.getMensagem()), any(), any()))
                .thenReturn(classificacao);
        prepararRespostaIa(request, List.of(), null);

        ChatConfiancaIaResponse response = service.perguntar(request);

        assertEquals("institucional.horario_atendimento", response.getIntencao());
        ArgumentCaptor<ChatRequestDTO> chatRequest = ArgumentCaptor.forClass(ChatRequestDTO.class);
        verify(chatService).chat(chatRequest.capture(), anyList(), isNull());
        assertTrue(chatRequest.getValue().messages().stream().anyMatch(item ->
                item.content().contains("Atendimento de segunda a sexta-feira.")));
        verify(chatService, never()).actionApis(anyList(), any());
        verify(chatService, never()).actionApis(anyList(), any(), anyString());
        JsonNode decisao = mapper.readTree(response.getConversa().getMetadadosJson())
                .path("decisaoIa");
        assertTrue(decisao.path("canarioHabilitado").asBoolean());
        assertTrue(decisao.path("canarioElegivel").asBoolean());
        assertTrue(decisao.path("aplicada").asBoolean());
        assertEquals("UNIFICADA", decisao.path("modo").asText());
        assertEquals("institucional.", decisao.path("escopoCanario").get(0).asText());
        assertEquals(7, decisao.path("memoriaIds").get(0).asInt());
    }

    @Test
    void dataCurtaComCardVigenteDeveOrientarEtapaSemConsultarBspNemReserva() throws Exception {
        PerguntarConfiaRequest request = request("20 set 2026");
        prepararContexto(request);
        Mensagem card = new Mensagem();
        card.setRemetenteTipo(RemetenteTipo.BOT);
        card.setEnviadaEm(java.time.LocalDateTime.now().minusMinutes(1));
        card.setConteudoJson(mapper.writeValueAsString(Map.of("remarcacao", Map.of(
                "conversaId", 10L, "status", "AGUARDANDO_CRITERIOS",
                "expiraEm", java.time.LocalDateTime.now().plusMinutes(15).toString()))));
        when(chatConfiancaService.listarMensagens(10L, 7, false, false)).thenReturn(List.of(card));
        ChatConfiancaIaResponse response = service.perguntar(request);
        assertTrue(response.getResposta().contains("Nova data"));
        assertEquals("aereo_simular_remarcacao", response.getIntencao());
        assertTrue(response.getActions().isEmpty());
        verify(chatService, never()).chat(any(), any(), any());
        verify(chatService, never()).actionApis(anyList(), any());
    }

    @Test
    void localizadorIsoladoAposSeletorDeveCompletarSomenteOTextoOperacional() throws Exception {
        PerguntarConfiaRequest request = request("ABC123");
        prepararContexto(request);
        prepararRespostaIa(request, List.of(), null);
        Mensagem card = new Mensagem();
        card.setRemetenteTipo(RemetenteTipo.BOT);
        card.setEnviadaEm(java.time.LocalDateTime.now().minusMinutes(1));
        card.setConteudoJson("{\"actions\":[{\"code\":\"selecionar_reserva_remarcacao\"}]}");
        when(chatConfiancaService.listarMensagens(10L, 7, false, false)).thenReturn(List.of(card));
        service.perguntar(request);
        ArgumentCaptor<com.confApi.chatgpt.dto.ConversationRequestDTO> consulta =
                ArgumentCaptor.forClass(com.confApi.chatgpt.dto.ConversationRequestDTO.class);
        verify(chatService).actionApis(anyList(), consulta.capture());
        assertEquals("Simular remarcacao da reserva ABC123", consulta.getValue().input());
        verify(chatConfiancaService).registrarMensagemUsuarioAssistida(10L, 7, "ABC123");
    }

    @Test
    void promessaSemConsultaNaoDevePedirAoUsuarioParaAguardar() throws Exception {
        PerguntarConfiaRequest request = request("Buscar pacote para janeiro");
        prepararContexto(request);
        prepararRespostaIa(request, List.of(), null);
        when(chatService.chat(any(), anyList(), isNull())).thenReturn(new ChatResponseDTO(
                "resp", "Vou buscar os pacotes. Aguarde um momento.", List.of(), null,
                List.of("pacotes"), List.of(), List.of()));
        ChatConfiancaIaResponse response = service.perguntar(request);
        assertTrue(response.getResposta().contains("Nao ha uma consulta em andamento"));
        ArgumentCaptor<ChatConfiancaDecisaoIa> decisao = ArgumentCaptor.forClass(ChatConfiancaDecisaoIa.class);
        verify(chatIaDecisaoAuditService).registrar(eq(10L), eq(30L), isNull(), eq("Confianca"),
                decisao.capture(), org.mockito.ArgumentMatchers.anyBoolean(), eq(false), isNull(), anyLong());
        assertEquals("FALLBACK", decisao.getValue().getStatusResultado());
    }

    @Test
    void falhaNaoDeveRecuperarAcoesDeReservaAntiga() throws Exception {
        PerguntarConfiaRequest request = request("Quero consultar as duvidas frequentes");
        prepararContexto(request);
        prepararRespostaIa(request, List.of(acaoSimularRemarcacao("OLD123")), null);
        when(chatService.actionApis(anyList(), any())).thenThrow(new NullPointerException("dado ausente"));
        ChatConfiancaIaResponse response = service.perguntar(request);
        assertTrue(response.getActions().isEmpty());
        verify(chatService, never()).extrairAcoesDisponiveis(anyList());
    }

    @Test
    void checkinLegadoDeveUsarResumoDeterministicoSemModelo() throws Exception {
        PerguntarConfiaRequest request = request("Quais meus proximos check-ins?");
        prepararContexto(request);
        when(chatService.identificarKeywordOperacionalDeterministica(request.getMensagem())).thenReturn("checkin");
        when(chatService.responderCheckinsProximos(any())).thenReturn(new ChatResponseDTO(null,
                "Consulta de embarques: 22/09/2026 a 25/09/2026.", List.of(), null, List.of("checkin"),
                List.of(new ChatMessageDTO("system", "Dado do sistema: {\"statusConsulta\":\"DADOS_CONSULTADOS\"}")), List.of()));
        assertTrue(service.perguntar(request).getResposta().contains("22/09/2026"));
        verify(chatService, never()).chat(any(), any(), any());
        verify(chatService, never()).actionApis(anyList(), any());
    }

    @Test
    void checkinLegadoComFalhaNaoDeveSerAuditadoComoSucesso() {
        PerguntarConfiaRequest request = request("Quais meus proximos check-ins?");
        prepararContexto(request);
        when(chatService.identificarKeywordOperacionalDeterministica(request.getMensagem())).thenReturn("checkin");
        when(chatService.responderCheckinsProximos(any())).thenReturn(new ChatResponseDTO(null,
                "Nao foi possivel consultar os embarques agora.", List.of(), null, List.of("checkin"),
                List.of(new ChatMessageDTO("system", "Dado do sistema: {\"statusConsulta\":\"ERRO_INTEGRACAO\"}")), List.of()));
        assertTrue(service.perguntar(request).getResposta().contains("Nao foi possivel consultar"));
        ArgumentCaptor<ChatConfiancaDecisaoIa> decisao = ArgumentCaptor.forClass(ChatConfiancaDecisaoIa.class);
        verify(chatIaDecisaoAuditService).registrar(eq(10L), eq(30L), isNull(), eq("Confianca"),
                decisao.capture(), org.mockito.ArgumentMatchers.anyBoolean(), eq(false), isNull(), anyLong());
        assertEquals("ERRO", decisao.getValue().getStatusResultado());
        assertEquals("CHECKIN_INDISPONIVEL", decisao.getValue().getErroCodigo());
    }

    @Test
    void legadoFinanceiroContinuaFiltrosSomenteDaSessaoENaoChamaLlm() throws Exception {
        PerguntarConfiaRequest request=request("e as pagas?");prepararContexto(request);
        var sessao=new SessaoChatResponse();var agencia=new com.confApi.chatconfianca.dto.model.RefAgencia();
        agencia.setCodgAgencia(321);agencia.setCodgSistemaBackoffice("987");sessao.setAgencia(agencia);
        when(chatConfiancaService.montarSessao(7,321)).thenReturn(sessao);
        Map<String,String> filtros=Map.of("faturaPagamento","ABERTO","faturaTipoData","DATA_EMISSAO","faturaInicio","2026-09-01","faturaFim","2026-09-30");
        String contexto=mapper.writeValueAsString(Map.of("consultaFaturas",Map.of("conversa",10,"agencia",321,"usuario",7,
                "atualizadoEm",System.currentTimeMillis(),"boletos",false,"parametros",filtros)));
        Mensagem bot=new Mensagem();bot.setConversaId(10L);bot.setRemetenteTipo(RemetenteTipo.BOT);bot.setConteudoJson(contexto);
        when(chatConfiancaService.listarMensagens(10L,7,false,false)).thenReturn(List.of(bot));
        when(chatService.responderFaturas(any(),any(),eq(false))).thenAnswer(inv->{
            com.confApi.chatgpt.dto.ConversationRequestDTO req=inv.getArgument(0);Map<String,String> p=inv.getArgument(1);
            assertEquals(321L,req.codgAgencia());assertEquals(7L,req.codgUsuario());assertEquals("987",req.idErp());
            assertEquals("PAGO",p.get("faturaPagamento"));assertEquals("2026-09-01",p.get("faturaInicio"));
            String json=mapper.writeValueAsString(Map.of("schema","chat.faturas.v1","statusConsulta","SEM_RESULTADO","filtros",p,
                    "contexto",Map.of("agencia",321,"usuario",7,"atualizadoEm",System.currentTimeMillis(),"parametros",p)));
            return new ChatResponseDTO(null,"Nenhuma fatura para esses filtros.",List.of(),null,List.of("faturas"),List.of(new ChatMessageDTO("system",json)));
        });
        when(chatConfiancaService.registrarMensagemBot(eq(10L),anyString(),anyString())).thenReturn(new Mensagem());
        var r=service.perguntar(request);assertEquals("Nenhuma fatura para esses filtros.",r.getResposta());
        verify(chatService,never()).chat(any(),anyList(),any());verify(chatService).responderFaturas(any(),any(),eq(false));
        ArgumentCaptor<String> metadados=ArgumentCaptor.forClass(String.class);
        verify(chatConfiancaService).registrarMensagemBot(eq(10L),anyString(),metadados.capture());
        var persisted=mapper.readTree(metadados.getValue()).path("consultaFaturas");
        assertEquals(10L,persisted.path("conversa").asLong());assertEquals("PAGO",persisted.path("parametros").path("faturaPagamento").asText());
        ArgumentCaptor<ChatConfiancaDecisaoIa> audit=ArgumentCaptor.forClass(ChatConfiancaDecisaoIa.class);
        verify(chatIaDecisaoAuditService).registrar(eq(10L),eq(30L),any(),any(),audit.capture(),eq(false),eq(false),any(),anyLong());
        assertEquals("FALLBACK",audit.getValue().getStatusResultado());
    }

    @Test
    void legadoFinanceiroDistingueErroDeConsultaVazia() throws Exception {
        var request=request("faturas");prepararContexto(request);
        when(chatService.responderFaturas(any(),any(),eq(false))).thenReturn(new ChatResponseDTO(null,
                "Consulta indisponível, tente novamente.",List.of(),null,List.of("faturas"),List.of(new ChatMessageDTO("system",
                "{\"schema\":\"chat.faturas.v1\",\"statusConsulta\":\"ERRO_INTEGRACAO\",\"filtros\":{}}"))));
        when(chatConfiancaService.registrarMensagemBot(eq(10L),anyString(),anyString())).thenReturn(new Mensagem());
        service.perguntar(request);
        ArgumentCaptor<ChatConfiancaDecisaoIa> audit=ArgumentCaptor.forClass(ChatConfiancaDecisaoIa.class);
        verify(chatIaDecisaoAuditService).registrar(eq(10L),eq(30L),any(),any(),audit.capture(),eq(false),eq(false),any(),anyLong());
        assertEquals("ERRO",audit.getValue().getStatusResultado());assertEquals("FATURAS_INDISPONIVEIS",audit.getValue().getErroCodigo());
        verify(chatService,never()).chat(any(),anyList(),any());
    }

    @Test
    void pesquisaEstruturadaNaoIgnoraEncaminhamentoComDepartamentoConfirmado() throws Exception {
        var request=request("pesquisar voos e falar com atendente");request.setEncaminharAtendente(true);request.setDepartamentoUnidadeId(20L);
        prepararContexto(request);prepararRespostaIa(request,List.of(),null);
        when(chatService.chat(any(),anyList(),isNull())).thenReturn(new ChatResponseDTO(null,"{\"tipo\":\"aereo\",\"status\":\"OK\",\"origem\":\"CGB\",\"destino\":\"BSB\",\"dataIda\":\"2027-01-10\"}",List.of(),null,List.of(),List.of()));
        var encaminhada=new Conversa();encaminhada.setId(10L);encaminhada.setDepartamentoUnidadeId(20L);
        encaminhada.setStatus(com.confApi.chatconfianca.dto.enums.StatusConversa.AGUARDANDO_ATENDENTE);
        when(chatConfiancaService.encaminharConversaParaAtendente(eq(10L),eq(7),eq(20L),anyString())).thenReturn(encaminhada);
        var response=service.perguntar(request);
        assertTrue(response.isAtendenteSolicitado());assertEquals(encaminhada,response.getConversa());
        assertTrue(response.getMensagemAtendente().contains("na fila da equipe escolhida"));
        verify(chatConfiancaService).encaminharConversaParaAtendente(eq(10L),eq(7),eq(20L),anyString());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "quero simular a remarcacao do localizador LA9578682RZQD",
            "Quero simular a remarcacao da reserva LA9578682RZQD",
            "Quero simular uma remarcação"})
    void solAbreSeletorNoLegadoSemDependerDaOpenAi(String mensagem) throws Exception {
        var request = request(mensagem);
        prepararContexto(request);
        var sessao = new SessaoChatResponse();
        var agencia = new com.confApi.chatconfianca.dto.model.RefAgencia();
        agencia.setCodgAgencia(321); agencia.setCodgSistemaBackoffice("987"); sessao.setAgencia(agencia);
        when(chatConfiancaService.montarSessao(7, 321)).thenReturn(sessao);
        when(profiles.systemPrompt(anyString(), anyLong(), anyLong())).thenReturn("prompt");
        when(chatConfiancaService.registrarMensagemBot(eq(10L), anyString(), anyString())).thenReturn(new Mensagem());

        var openAi = mock(okhttp3.OkHttpClient.class);
        var call = mock(okhttp3.Call.class);
        when(openAi.newCall(any())).thenReturn(call);
        when(call.execute()).thenThrow(new java.io.IOException("falha simulada do provedor"));
        var props = new com.confApi.chatgpt.config.OpenAIProperties();
        props.setChatModel("gpt-6.1-sol"); props.setBaseUrl("https://provider.invalid"); props.setApiKey("unused-test");
        var aereo = mock(com.confApi.aereo.AereoClient.class);
        var reserva = new com.confApi.aereo.dto.Reserva(); reserva.setLocalizador("LA9578682RZQD"); reserva.setStatus("EMITIDA");
        var consulta = new com.confApi.aereo.dto.ConsultarLocalizadorResponse(); consulta.setReservas(List.of(reserva));
        when(aereo.carregarReservaEstrita(any())).thenReturn(consulta);
        var realChat = new ChatService(openAi, props, mock(com.confApi.chatgpt.tools.ToolRouter.class),
                mock(com.confApi.db.confManager.chatMemoria.ChatMemoriaService.class),
                mock(com.confApi.hub.limites.LimitesService.class), mock(com.confApi.db.confManager.faturas.FaturasService.class),
                mock(com.confApi.db.wooba.checkin.CheckinService.class), mock(com.confApi.db.confManager.familia.FamiliaService.class),
                mock(com.confApi.db.confManager.alertaTarifa.AlertaTarifaService.class), mock(ChatConfiancaReservaAereaService.class),
                aereo, mock(com.confApi.aereo.AereoRegrasReservaService.class));
        service = new ChatConfiancaIaService(chatConfiancaService, realChat, profiles, mapper, chatIntencaoShadowService,
                new ChatConfiancaDecisaoIaService(chatIntencaoShadowService, realChat, decisionProperties),
                chatMemoriaRecuperacaoAuditService, chatIaDecisaoAuditService);

        var response = service.perguntar(request);
        assertEquals(1, response.getActions().size());
        assertTrue(response.getResposta().contains("Nenhuma alteracao ou cobranca"));
        assertTrue(!response.isSugerirAtendente());
        if (mensagem.contains("LA9578682RZQD")) {
            assertEquals("simular_remarcacao", response.getAcaoSolicitada());
            assertEquals("LA9578682RZQD", response.getActions().get(0).localizador());
            var captura = ArgumentCaptor.forClass(com.confApi.aereo.dto.ConsultarLocalizadorRequest.class);
            verify(aereo).carregarReservaEstrita(captura.capture());
            assertEquals("321", captura.getValue().getAgencia().getCodgAgencia());
        } else {
            assertEquals("selecionar_reserva_remarcacao", response.getActions().get(0).code());
            assertNull(response.getAcaoSolicitada()); verifyNoInteractions(aereo);
        }
        var conteudo = ArgumentCaptor.forClass(String.class);
        verify(chatConfiancaService).registrarMensagemBot(eq(10L), anyString(), conteudo.capture());
        assertEquals(1, mapper.readTree(conteudo.getValue()).path("actions").size());
        verifyNoInteractions(openAi, call);
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

        SessaoChatResponse sessao = new SessaoChatResponse();
        when(chatConfiancaService.montarSessao(7, 321)).thenReturn(sessao);
        when(chatConfiancaService.buscarConversaNaSessao(
                eq(10L), eq(7), any(SessaoChatResponse.class))).thenReturn(conversa);
        when(chatConfiancaService.listarMensagens(10L, 7, false, false))
                .thenReturn(new ArrayList<>());
        when(chatConfiancaService.listarDepartamentosRoteamentoPorUsuario(7, 321))
                .thenReturn(List.of(departamento));
        Mensagem mensagemUsuario = new Mensagem();
        mensagemUsuario.setId(30L);
        when(chatConfiancaService.registrarMensagemUsuarioAssistida(
                10L, 7, request.getMensagem()))
                .thenReturn(mensagemUsuario);
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
