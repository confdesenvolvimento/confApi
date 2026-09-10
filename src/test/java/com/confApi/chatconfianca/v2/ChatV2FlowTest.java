package com.confApi.chatconfianca.v2;

import com.confApi.chatconfianca.dto.model.*;
import com.confApi.chatconfianca.dto.request.PerguntarConfiaRequest;
import com.confApi.chatconfianca.dto.response.*;
import com.confApi.chatconfianca.intencao.*;
import com.confApi.chatconfianca.service.*;
import com.confApi.chatgpt.dto.*;
import com.confApi.chatgpt.profile.ProfilePromptRegistry;
import com.confApi.chatgpt.service.ChatService;
import com.confApi.chatgpt.tools.ToolRouter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Real coordinator/decision/executor wiring; only external boundaries are mocked. No database or HTTP. */
class ChatV2FlowTest {
    ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    ChatConfiancaService persistence;ChatService chat;ProfilePromptRegistry profiles;
    ChatIntencaoShadowService shadow;ChatIaDecisaoAuditService audit;ToolRouter router;
    ChatV2SemanticClient semantic;ChatV2Properties props;ChatConfiancaIaService service;
    Conversa conversation;SessaoChatResponse session;
    @BeforeEach void setup() {
        persistence=mock(ChatConfiancaService.class);chat=mock(ChatService.class);
        profiles=mock(ProfilePromptRegistry.class);shadow=mock(ChatIntencaoShadowService.class);
        audit=mock(ChatIaDecisaoAuditService.class);router=mock(ToolRouter.class);semantic=mock(ChatV2SemanticClient.class);
        props=new ChatV2Properties();props.setEnabled(true);props.setTrafficPercent(100);
        props.setSemanticEnabled(false);
        when(shadow.classificar(any(),any(),any())).thenReturn(ChatIntencaoClassificacao.status("SEM_EVIDENCIA"));
        var decision=new ChatConfiancaDecisaoIaService(shadow,chat,new ChatIntencaoShadowProperties());
        service=new ChatConfiancaIaService(persistence,chat,profiles,mapper,shadow,decision,
                mock(ChatMemoriaRecuperacaoShadowAuditService.class),audit,
                new ChatV2Planner(props,semantic,mapper),new ChatV2Executor(chat,router,mapper));
        conversation=new Conversa();conversation.setId(10L);
        session=new SessaoChatResponse();RefAgencia agency=new RefAgencia();
        agency.setCodgAgencia(321);agency.setCodgSistemaBackoffice("ERP-321");session.setAgencia(agency);
        when(persistence.montarSessao(7,321)).thenReturn(session);
        when(persistence.buscarConversaNaSessao(10L,7,session)).thenReturn(conversation);
        when(persistence.listarDepartamentosRoteamentoPorUsuario(7,321)).thenReturn(List.of());
        when(persistence.listarMensagens(10L,7,false,false)).thenReturn(List.of());
        when(persistence.registrarMensagemUsuarioAssistida(eq(10L),eq(7),anyString())).thenReturn(new Mensagem());
        when(persistence.atualizarMetadadosConversaAssistida(eq(10L),anyString())).thenAnswer(inv->{
            conversation.setMetadadosJson(inv.getArgument(1));return conversation;
        });
        when(persistence.registrarMensagemBot(eq(10L),anyString(),anyString())).thenAnswer(inv->{
            Mensagem bot=new Mensagem();bot.setConteudoJson(inv.getArgument(2));return bot;
        });
    }
    PerguntarConfiaRequest request(String text) {
        var r=new PerguntarConfiaRequest();r.setConversaId(10L);r.setCodgUsuario(7);
        r.setCodgAgenciaSessao(321);r.setMensagem(text);return r;
    }
    JsonNode state()throws Exception{return mapper.readTree(conversation.getMetadadosJson()).path("confiaV2");}
    @Test void consultaFinanceiraPersistidaSemReclassificadorNemDepartamento()throws Exception {
        when(chat.actionApis(anyList(),any(),eq("limites"),eq(true))).thenAnswer(inv->{
            List<ChatMessageDTO> msgs=inv.getArgument(0);msgs.add(new ChatMessageDTO("system","Limite consultado."));
            ConversationRequestDTO req=inv.getArgument(1);assertEquals(321L,req.codgAgencia());assertEquals("ERP-321",req.idErp());return List.of("limites");
        });
        when(chat.chat(any(),anyList(),isNull())).thenReturn(new ChatResponseDTO(null,"Limite consultado.",List.of(),null,List.of(),List.of()));
        var r=service.perguntar(request("meus limites"));
        assertEquals("financeiro.limites",r.getIntencao());assertNull(r.getDepartamentoSugerido());
        assertEquals("DADOS_CONSULTADOS",state().path("resultado").asText());
        assertEquals("decisor-v2.0",mapper.readTree(conversation.getMetadadosJson()).path("decisaoIa").path("versao").asText());
        verify(chat,never()).actionApis(anyList(),any());verifyNoInteractions(semantic,router,profiles);
        ArgumentCaptor<ChatRequestDTO> prompt=ArgumentCaptor.forClass(ChatRequestDTO.class);
        verify(chat).chat(prompt.capture(),anyList(),isNull());
        assertTrue(prompt.getValue().tools().isEmpty());assertEquals(true,prompt.getValue().metadata().get("coordenadorV2"));
    }
    @Test void pesquisaPersisteBotMesmoRetornandoJsonDeNavegacao()throws Exception {
        props.setSemanticEnabled(true);
        ChatV2Plan p=ChatV2Plan.of(ChatV2Capability.VOOS);
        p.setParametros(new LinkedHashMap<>(Map.of("origem","CGB","destino","GRU","dataIda",LocalDate.now().plusDays(30).toString())));
        when(semantic.decidir(anyString(),any(),any())).thenReturn(p);
        when(router.execute(eq("search_flights"),anyMap())).thenReturn(Map.of(
                "status","OK","tipo","aereo","origem","CGB","destino","GRU","dataIda",LocalDate.now().plusDays(30).toString()));
        var r=service.perguntar(request("Pesquise voos de CGB para GRU"));
        assertNotNull(r.getMensagemBot());
        JsonNode bot=mapper.readTree(r.getMensagemBot().getConteudoJson());
        assertEquals("chat.pesquisa-viagem.v2",bot.path("schema").asText());
        assertEquals("PESQUISA_PREPARADA",bot.path("confiaV2").path("resultado").asText());
        assertEquals("OK",mapper.readTree(r.getResposta()).path("status").asText());
        verify(router,times(1)).execute(eq("search_flights"),anyMap());
        verify(chat,never()).chat(any(),any(),any());verify(chat,never()).actionApis(anyList(),any(),anyString(),eq(true));
    }
    @Test void localizadorCompletaPerguntaNoTurnoSeguinte()throws Exception {
        service.perguntar(request("regras da reserva"));
        assertEquals("AGUARDANDO_DADOS",state().path("resultado").asText());
        verify(chat,never()).actionApis(anyList(),any(),anyString(),eq(true));
        verify(chat,never()).chat(any(),any(),any());verifyNoInteractions(router,semantic,profiles);
        service.perguntar(request("ABC123"));
        assertEquals("aereo.regra_tarifaria",state().path("intencao").asText());
        assertEquals("ABC123",state().path("parametros").path("localizador").asText());
        verify(chat).actionApis(anyList(),any(),eq("reserva_aerea_regras"),eq(true));
    }
    @Test void conversaNaoAutorizadaNaoConsultaIaOuFerramentas() {
        when(persistence.buscarConversaNaSessao(10L,7,session)).thenThrow(new IllegalArgumentException("Sem acesso"));
        assertThrows(IllegalArgumentException.class,()->service.perguntar(request("meus limites")));
        verifyNoInteractions(semantic,router,chat);
        verify(persistence,never()).registrarMensagemUsuarioAssistida(anyLong(),anyInt(),anyString());
    }
    @Test void handoffPreservaAssuntoSemVincularDepartamentoOuEncaminhar()throws Exception {
        DepartamentoUnidade financeiro=new DepartamentoUnidade();financeiro.setId(99L);financeiro.setNomeExibicao("Financeiro");
        when(persistence.listarDepartamentosRoteamentoPorUsuario(7,321)).thenReturn(List.of(financeiro));
        ChatV2Plan anterior=ChatV2Plan.of(ChatV2Capability.FATURAS);
        anterior.setAgencia(321);anterior.setUsuario(7);anterior.setAtualizadoEm(System.currentTimeMillis());
        conversation.setMetadadosJson(mapper.writeValueAsString(Map.of("confiaV2",anterior)));
        var r=service.perguntar(request("quero falar com atendente"));
        assertEquals(99L,r.getDepartamentoSugerido().getId());assertNull(conversation.getDepartamentoUnidadeId());
        assertEquals("conversa.atendimento_humano",r.getIntencao());
        verify(persistence,never()).encaminharConversaParaAtendente(anyLong(),anyInt(),any(),anyString());
    }
    @Test void falhaDoShadowNaoAlteraRespostaOuDuplicaConsulta()throws Exception {
        var observer=mock(com.confApi.chatconfianca.configuracao.ChatIaConfiguracaoShadowService.class);
        service.setConfiguracaoShadow(observer);
        doThrow(new IllegalStateException("Shadow indisponivel")).when(observer)
                .observar(any(),any(),any(),any(),any(),any(),anyBoolean(),any(),any(),any(),any(),anyBoolean());
        when(chat.actionApis(anyList(),any(),eq("limites"),eq(true))).thenAnswer(inv -> {
            List<ChatMessageDTO> dados=inv.getArgument(0);
            dados.add(new ChatMessageDTO("system","Dados de teste.")); return List.of("limites");
        });
        when(chat.chat(any(),anyList(),isNull())).thenReturn(new ChatResponseDTO(null,"Resposta original",List.of(),null,List.of(),List.of()));
        var r=service.perguntar(request("meus limites"));
        assertEquals("Resposta original",r.getResposta()); assertNotNull(r.getMensagemBot());
        verify(chat,times(1)).actionApis(anyList(),any(),eq("limites"),eq(true));
        verify(observer,times(1)).observar(any(),any(),any(),any(),any(),any(),anyBoolean(),any(),eq(7),eq(321),eq("meus limites"),anyBoolean());
        verifyNoInteractions(semantic,router);
    }
    @Test void legadoContinuaBspNoShadowSemModificarRespostaOuDuplicarIa() throws Exception {
        props.setEnabled(false);
        conversation.setStatus(com.confApi.chatconfianca.dto.enums.StatusConversa.AGUARDANDO_SOLICITANTE);
        // Real assisted conversations retain the technical ConfIA Geral linkage.
        conversation.setDepartamentoUnidadeId(20L);
        conversation.setCodgUnidade(1);
        RefUnidade unidade=new RefUnidade();unidade.setCodgUnidade(1);unidade.setNomeUnidade("CGB");session.setUnidade(unidade);
        var sequencia=new java.util.concurrent.atomic.AtomicLong(3884);
        when(persistence.registrarMensagemUsuarioAssistida(eq(10L),eq(7),anyString())).thenAnswer(inv->{
            var m=new Mensagem();m.setId(sequencia.addAndGet(2));return m;
        });
        var bsp=ChatIntencaoClassificacao.status("CLASSIFICADA");
        bsp.setCodigo("financeiro.calendario_bsp");bsp.setConfianca(85);
        when(shadow.classificar(eq("Quero consultar o calendario BSP"),any(),any())).thenReturn(bsp);
        when(profiles.systemPrompt(anyString(),anyLong(),anyLong())).thenReturn("Perfil de teste");
        when(chat.actionApis(anyList(),any())).thenReturn(List.of());
        when(chat.chat(any(),anyList(),isNull())).thenReturn(
                new ChatResponseDTO(null,"Informe a data de emissao",List.of(),null,List.of(),List.of()),
                new ChatResponseDTO(null,"Resposta atual do BSP",List.of(),null,List.of(),List.of()));
        var http=new org.springframework.web.client.RestTemplate();
        var server=org.springframework.test.web.client.MockRestServiceServer.bindTo(http).build();
        var auth=mock(com.confApi.confApp.ConfAppService.class);
        var token=mock(com.confApi.confApp.ConfAppResp.class);when(token.getToken()).thenReturn("token-de-teste");when(auth.token()).thenReturn(token);
        service.setConfiguracaoShadow(new com.confApi.chatconfianca.configuracao.ChatIaConfiguracaoShadowService(
                http,auth,Runnable::run,mapper,100));
        String url=com.confApi.config.UrlConfig.URL_CONFIANCA_MANAGER;
        try {
            com.confApi.config.UrlConfig.URL_CONFIANCA_MANAGER="http://manager.invalid/";
            server.expect(org.springframework.test.web.client.ExpectedCount.twice(),
                    org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                            "http://manager.invalid/chatIa/runtime/configuracao?intencao=financeiro.calendario_bsp"))
                    .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                            "{\"configuracao\":null,\"etapas\":[],\"conhecimentos\":[]}",org.springframework.http.MediaType.APPLICATION_JSON));
            assertEquals("Informe a data de emissao",service.perguntar(request("Quero consultar o calendario BSP")).getResposta());
            assertEquals("Resposta atual do BSP",service.perguntar(request("10-08-2026")).getResposta());
            server.verify();
            verify(chat,times(2)).chat(any(),anyList(),isNull());
            verify(chat,times(2)).actionApis(anyList(),any());
            verifyNoInteractions(semantic,router);
            assertEquals(20L,conversation.getDepartamentoUnidadeId());
        } finally { com.confApi.config.UrlConfig.URL_CONFIANCA_MANAGER=url; }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(com.confApi.chatconfianca.dto.enums.StatusConversa.class)
    void elegibilidadeShadowUsaStatusNaoVinculoTecnico(com.confApi.chatconfianca.dto.enums.StatusConversa status) throws Exception {
        conversation.setStatus(status); conversation.setDepartamentoUnidadeId(20L);
        var observer=mock(com.confApi.chatconfianca.configuracao.ChatIaConfiguracaoShadowService.class);
        service.setConfiguracaoShadow(observer);
        service.perguntar(request("bom dia"));
        boolean assistida=status==com.confApi.chatconfianca.dto.enums.StatusConversa.NOVA
                || status==com.confApi.chatconfianca.dto.enums.StatusConversa.AGUARDANDO_SOLICITANTE;
        verify(observer).observar(any(),any(),any(),any(),any(),any(),anyBoolean(),any(),eq(7),eq(321),eq("bom dia"),eq(assistida));
        assertEquals(20L,conversation.getDepartamentoUnidadeId());
    }
    @Test void atendenteResponsavelImpedeContinuidadeMesmoAguardandoSolicitante() throws Exception {
        conversation.setStatus(com.confApi.chatconfianca.dto.enums.StatusConversa.AGUARDANDO_SOLICITANTE);
        conversation.setDepartamentoUnidadeId(20L);conversation.setAtendenteResponsavelCodgUsuario(99);
        var observer=mock(com.confApi.chatconfianca.configuracao.ChatIaConfiguracaoShadowService.class);
        service.setConfiguracaoShadow(observer);
        service.perguntar(request("bom dia"));
        verify(observer).observar(any(),any(),any(),any(),any(),any(),anyBoolean(),any(),eq(7),eq(321),eq("bom dia"),eq(false));
    }

    @Test void desativarV2MantemCaminhoAnterior()throws Exception {
        props.setEnabled(false);
        when(profiles.systemPrompt(anyString(),anyLong(),anyLong())).thenReturn("perfil");
        when(chat.actionApis(anyList(),any())).thenReturn(List.of("limites"));
        when(chat.chat(any(),anyList(),isNull())).thenReturn(new ChatResponseDTO(null,"Legado",List.of(),null,List.of("limites"),List.of()));
        service.perguntar(request("meus limites"));
        assertTrue(state().isMissingNode());verify(chat).actionApis(anyList(),any());verifyNoInteractions(semantic,router);
    }
}
