package com.confApi.chatconfianca.configuracao.ti;

import com.confApi.chatconfianca.dto.enums.*;
import com.confApi.chatconfianca.dto.model.*;
import com.confApi.chatconfianca.dto.request.PerguntarConfiaRequest;
import com.confApi.chatconfianca.dto.response.*;
import com.confApi.chatconfianca.intencao.*;
import com.confApi.chatconfianca.service.*;
import com.confApi.chatconfianca.v2.*;
import com.confApi.chatgpt.dto.*;
import com.confApi.chatgpt.profile.ProfilePromptRegistry;
import com.confApi.chatgpt.service.ChatService;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real coordinator and TI service; all external boundaries mocked. */
class ChatIaTiIntegracaoTest {
    ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    ChatConfiancaService persistence;ChatService chat;ChatConfiancaIaService coordinator;
    ChatConfiancaDecisaoIaService decision;ChatConfiancaDecisaoIa decisao;
    ChatIaTiProperties properties;ChatIaTiConteudoClient content;ChatV2Planner planner;ChatV2Executor executor;
    Conversa conversa;SessaoChatResponse sessao;ChatIaDecisaoAuditService audit;
    @BeforeEach void setup() throws Exception {
        persistence=mock(ChatConfiancaService.class);chat=mock(ChatService.class);decision=mock(ChatConfiancaDecisaoIaService.class);
        var profile=mock(ProfilePromptRegistry.class);when(profile.systemPrompt(anyString(),anyLong(),anyLong())).thenReturn("PROMPT_ATUAL");
        audit=mock(ChatIaDecisaoAuditService.class);planner=mock(ChatV2Planner.class);executor=mock(ChatV2Executor.class);
        coordinator=new ChatConfiancaIaService(persistence,chat,profile,mapper,mock(ChatIntencaoShadowService.class),decision,
            mock(ChatMemoriaRecuperacaoShadowAuditService.class),audit,planner,executor);
        properties=new ChatIaTiProperties();properties.setEnabled(true);properties.setSamplePercent(100);
        properties.setUsuarios(Set.of(7));properties.setAgencias(Set.of(321));
        content=mock(ChatIaTiConteudoClient.class);when(content.carregar()).thenReturn(ChatIaTiTestData.documento());
        coordinator.setRespostaTi(new ChatIaTiRespostaService(properties,content,chat,mapper));
        conversa=new Conversa();conversa.setId(774L);conversa.setStatus(StatusConversa.NOVA);conversa.setDepartamentoUnidadeId(999L);
        sessao=new SessaoChatResponse();var a=new RefAgencia();a.setCodgAgencia(321);sessao.setAgencia(a);
        var u=new RefUnidade();u.setCodgUnidade(1);u.setNomeUnidade("CGB");sessao.setUnidade(u);
        when(persistence.montarSessao(7,321)).thenReturn(sessao);
        when(persistence.buscarConversaNaSessao(774L,7,sessao)).thenReturn(conversa);
        when(persistence.listarDepartamentosRoteamentoPorUsuario(7,321)).thenReturn(List.of());
        when(persistence.listarMensagens(774L,7,false,false)).thenReturn(List.of());
        when(persistence.registrarMensagemUsuarioAssistida(eq(774L),eq(7),anyString())).thenAnswer(inv->{
            var m=new Mensagem();m.setId(3947L);m.setConteudo(inv.getArgument(2));return m;
        });
        when(persistence.atualizarMetadadosConversaAssistida(eq(774L),anyString())).thenAnswer(inv->{
            conversa.setMetadadosJson(inv.getArgument(1));return conversa;
        });
        when(persistence.registrarMensagemBot(eq(774L),anyString(),anyString())).thenAnswer(inv->{
            var m=new Mensagem();m.setId(3948L);m.setConteudo(inv.getArgument(1));m.setConteudoJson(inv.getArgument(2));return m;
        });
        decisao=decisao("institucional.suporte_ti");
        when(decision.decidir(anyString(),isNull(),anyList(),any(),anyString())).thenAnswer(inv->decisao);
        when(chat.responderTiSomenteTexto(anyList())).thenReturn(new ChatResponseDTO("mock","Contato de TI.",List.of(),null,List.of(),List.of(),List.of()));
        when(chat.chat(any(),any(),any())).thenReturn(new ChatResponseDTO("legado","Resposta atual.",List.of(),null,List.of(),List.of(),List.of()));
    }
    ChatConfiancaDecisaoIa decisao(String codigo) {
        var d=new ChatConfiancaDecisaoIa();d.setModo("LEGADO_FALLBACK");d.setIntencao("orientacao_geral");
        var c=ChatIntencaoClassificacao.status("CLASSIFICADA");c.setCodigo(codigo);c.setConfianca(85);d.setClassificacaoCatalogo(c);
        return d;
    }
    PerguntarConfiaRequest request(String texto) {
        var r=new PerguntarConfiaRequest();r.setConversaId(774L);r.setCodgUsuario(7);r.setCodgAgenciaSessao(321);r.setMensagem(texto);return r;
    }
    @Test void aplicaTiPersisteVersoesSemExecutarV1OuV2NemEncaminhar() throws Exception {
        var r=coordinator.perguntar(request("Qual contato de TI?"));
        assertTrue(r.getResposta().startsWith("Contato de TI."));assertNull(r.getDepartamentoSugerido());
        assertEquals("institucional.suporte_ti",r.getIntencao());assertTrue(r.getActions().isEmpty());
        var meta=mapper.readTree(r.getMensagemBot().getConteudoJson()).path("confiaTiResposta");
        assertTrue(meta.path("aplicada").asBoolean());assertEquals(10,meta.path("memoriaId").asInt());
        assertEquals(1,meta.path("perfilVersao").asInt());assertEquals(2,meta.path("conhecimentoVersao").asInt());
        assertFalse(r.getMensagemBot().getConteudoJson().contains("FONTE_PRIVADA"));
        assertFalse(conversa.getMetadadosJson().contains("PERFIL_PRIVADO"));
        verify(chat).responderTiSomenteTexto(anyList());verify(chat,never()).chat(any(),any(),any());
        verify(chat,never()).actionApis(anyList(),any());verifyNoInteractions(executor);
        verify(persistence,never()).encaminharConversaParaAtendente(anyLong(),anyInt(),any(),anyString());
        assertEquals("CADASTRO_TI",decisao.getModo());assertEquals("SUCESSO",decisao.getStatusResultado());
    }
    @Test void falhaDeGeracaoExecutaCaminhoAtualUmaVez() throws Exception {
        // This test covers the legacy lane; the V2 lane is tested independently below.
        when(chat.responderTiSomenteTexto(anyList())).thenThrow(new IOException("NAO_LOGAR"));
        var r=coordinator.perguntar(request("Qual contato de TI?"));assertEquals("Resposta atual.",r.getResposta());
        assertFalse(mapper.readTree(r.getMensagemBot().getConteudoJson()).path("confiaTiResposta").path("aplicada").asBoolean());
        assertEquals("LEGADO_FALLBACK",decisao.getModo());verify(chat).responderTiSomenteTexto(anyList());
        verify(chat).chat(any(),any(),any());verify(chat).actionApis(anyList(),any());verifyNoInteractions(executor);
    }
    @Test void desligadoMantemCaminhoAtualSemConsultaNova() throws Exception {
        properties.setEnabled(false);var r=coordinator.perguntar(request("Qual contato de TI?"));
        assertEquals("Resposta atual.",r.getResposta());verifyNoInteractions(content);
        verify(chat,never()).responderTiSomenteTexto(anyList());verify(chat).chat(any(),any(),any());
        assertFalse(mapper.readTree(r.getMensagemBot().getConteudoJson()).has("confiaTiResposta"));
    }
    @Test void trocaParaBspNaoHerdeAplicaTiAnterior() throws Exception {
        coordinator.perguntar(request("Qual contato de TI?"));assertTrue(conversa.getMetadadosJson().contains("confiaTiResposta"));
        decisao=decisao("financeiro.calendario_bsp");
        var r=coordinator.perguntar(request("Quero consultar o calendário BSP."));
        assertEquals("Resposta atual.",r.getResposta());verify(content,times(1)).carregar();
        assertFalse(mapper.readTree(conversa.getMetadadosJson()).has("confiaTiResposta"));
        assertFalse(mapper.readTree(r.getMensagemBot().getConteudoJson()).has("confiaTiResposta"));
        verify(chat,times(1)).responderTiSomenteTexto(anyList());verify(chat,times(1)).chat(any(),any(),any());
    }
    @Test void historicoFinanceiroNaoVaiAoPromptTi() throws Exception {
        var anterior=new Mensagem();anterior.setRemetenteTipo(RemetenteTipo.BOT);anterior.setConteudo("SALDO_PRIVADO");
        when(persistence.listarMensagens(774L,7,false,false)).thenReturn(List.of(anterior));
        coordinator.perguntar(request("Qual contato de TI?"));
        ArgumentCaptor<List<ChatMessageDTO>> captor=ArgumentCaptor.forClass(List.class);verify(chat).responderTiSomenteTexto(captor.capture());
        assertFalse(captor.getValue().stream().anyMatch(m->m.content().contains("SALDO_PRIVADO")));
    }
    @Test void tiJaEscolhidaPeloV2UsaCadastroSemExecutarDuasRespostas() throws Exception {
        var plano=ChatV2Plan.of(ChatV2Capability.TI);
        when(planner.planejar(anyString(),any(),anyList(),anyInt(),anyInt())).thenReturn(plano);
        decisao.setAplicada(true);decisao.setModo("V2");decisao.setIntencao(ChatIaTiRespostaService.INTENCAO);
        when(decision.decidirV2(any(),anyString(),isNull(),anyList(),any(),anyString())).thenReturn(decisao);
        var r=coordinator.perguntar(request("Qual contato de TI?"));
        assertTrue(r.getResposta().startsWith("Contato de TI."));
        assertEquals("RESPONDIDO_CADASTRO_TI",plano.getResultado());
        verifyNoInteractions(executor);verify(chat,never()).chat(any(),any(),any());
        verify(chat).responderTiSomenteTexto(anyList());
    }
    @Test void falhaNoPilotoPreservaExecutorV2JaSelecionado() throws Exception {
        var plano=ChatV2Plan.of(ChatV2Capability.TI);
        when(planner.planejar(anyString(),any(),anyList(),anyInt(),anyInt())).thenReturn(plano);
        decisao.setAplicada(true);decisao.setModo("V2");decisao.setIntencao(ChatIaTiRespostaService.INTENCAO);
        when(decision.decidirV2(any(),anyString(),isNull(),anyList(),any(),anyString())).thenReturn(decisao);
        when(content.carregar()).thenReturn(null);
        when(executor.executar(eq(plano),any(),eq(decisao))).thenReturn(
            new ChatResponseDTO("v2","Resposta V2 atual.",List.of(),null,List.of(),List.of(),List.of()));
        var r=coordinator.perguntar(request("Qual contato de TI?"));
        assertEquals("Resposta V2 atual.",r.getResposta());assertEquals("V2",decisao.getModo());
        verify(executor).executar(eq(plano),any(),eq(decisao));
        verify(chat,never()).responderTiSomenteTexto(anyList());verify(chat,never()).chat(any(),any(),any());
    }
    @Test void leituraDeConfiguracaoInvalidaPreservaRespostaAtual() throws Exception {
        when(content.carregar()).thenReturn(null);var r=coordinator.perguntar(request("Qual contato de TI?"));
        assertEquals("Resposta atual.",r.getResposta());verify(chat,never()).responderTiSomenteTexto(anyList());
        verify(chat,times(1)).chat(any(),any(),any());
    }
}
