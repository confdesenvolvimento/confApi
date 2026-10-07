package com.confApi.chatconfianca.v2;
import com.confApi.chatconfianca.dto.model.Conversa;
import com.confApi.chatconfianca.intencao.ChatConfiancaDecisaoIa;
import com.confApi.chatgpt.dto.*;
import com.confApi.chatgpt.service.ChatService;
import com.confApi.chatgpt.tools.ToolRouter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ChatV2FaturasTest {
    ObjectMapper mapper=new ObjectMapper();ChatV2Properties props=new ChatV2Properties();
    ChatV2SemanticClient semantic=mock(ChatV2SemanticClient.class);ChatService chat=mock(ChatService.class);
    ToolRouter tools=mock(ToolRouter.class);ChatV2Planner planner;ChatV2Executor executor;
    @BeforeEach void setup(){props.setEnabled(true);props.setTrafficPercent(100);props.setSemanticEnabled(false);
        planner=new ChatV2Planner(props,semantic,mapper);executor=new ChatV2Executor(chat,tools,mapper);}
    ChatV2Plan plan(String text,Conversa c){return planner.planejar(text,c,List.of(),10,20);}
    Map<String,String> filtros(){return Map.of("faturaPagamento","ABERTO","faturaTipoData","DATA_EMISSAO","faturaInicio","2026-09-01","faturaFim","2026-09-30");}
    Conversa contexto()throws Exception {
        ChatV2Plan p=ChatV2Plan.of(ChatV2Capability.FATURAS);p.setParametros(new LinkedHashMap<>(filtros()));
        p.setAgencia(10);p.setUsuario(20);p.setAtualizadoEm(System.currentTimeMillis());
        Conversa c=new Conversa();c.setMetadadosJson(mapper.writeValueAsString(Map.of("confiaV2",p)));return c;
    }
    ConversationRequestDTO session(String text){return new ConversationRequestDTO("confia","CGB","123",10L,20L,text,List.of(),null,false,List.of());}
    ChatResponseDTO resposta(String status)throws Exception {
        String json=mapper.writeValueAsString(Map.of("schema","chat.faturas.v1","statusConsulta",status,"filtros",filtros(),"faturas",List.of()));
        return new ChatResponseDTO(null,"Resposta financeira",List.of(),null,List.of("faturas"),List.of(new ChatMessageDTO("system",json)));
    }
    @Test void pagoSemPeriodoPedeDadosAntesDaConsulta() {
        var p=plan("Minhas faturas pagas",null);assertEquals(ChatV2Capability.FATURAS,p.capability());assertNotNull(p.getPergunta());
        executor.executar(p,session("Minhas faturas pagas"),new ChatConfiancaDecisaoIa());assertEquals("AGUARDANDO_DADOS",p.getResultado());verifyNoInteractions(chat,tools);
    }
    @Test void periodoSemBaseNaoViraDataDePagamento() {
        var p=plan("faturas pagas em setembro de 2026",null);
        assertNotNull(p.getPergunta());assertFalse(p.getParametros().containsKey("faturaTipoData"));
        assertEquals("2026-09-01",p.getParametros().get("faturaInicio"));
    }
    @Test void perguntaCurtaMantemPeriodoESessao()throws Exception {
        var p=plan("e as pagas?",contexto());assertEquals(ChatV2Capability.FATURAS,p.capability());assertTrue(p.isContinuar());
        assertEquals("PAGO",p.getParametros().get("faturaPagamento"));assertEquals("2026-09-01",p.getParametros().get("faturaInicio"));assertNull(p.getPergunta());
        verifyNoInteractions(semantic);
    }
    @Test void financeiroLimpaFiltrosDeViagemEIdentidade() {
        var p=ChatV2Plan.of(ChatV2Capability.FATURAS);p.setParametros(new LinkedHashMap<>(Map.of("localizador","ABC123","origem","CGB","agencia","999","faturaPagamento","PAGO")));
        ChatV2Planner.restringirParametrosAoAssunto(p,"faturas");assertEquals(Map.of("faturaPagamento","PAGO"),p.getParametros());
        p.setIntencao(ChatV2Capability.CHECKIN.code);ChatV2Planner.restringirParametrosAoAssunto(p,"checkins");assertTrue(p.getParametros().isEmpty());
    }
    @Test void mudaAssuntoSemManterFinanceiro()throws Exception {
        var p=plan("quais meus proximos checkins?",contexto());assertEquals(ChatV2Capability.CHECKIN,p.capability());assertFalse(p.getParametros().containsKey("faturaPagamento"));
    }
    @Test void historicoFinanceiroNaoEhVendas() {assertEquals(ChatV2Capability.FATURAS,plan("histórico financeiro",null).capability());}
    @Test void anoFinanceiroNoPassadoNaoEhAdiado() {
        var p=plan("faturas pagas emitidas em janeiro de 2025",null);assertEquals("2025-01-01",p.getParametros().get("faturaInicio"));assertNull(p.getPergunta());
    }
    @Test void outroUsuarioNaoReutilizaFiltros()throws Exception {
        var p=planner.planejar("e as pagas?",contexto(),List.of(),10,21);assertNotEquals(ChatV2Capability.FATURAS,p.capability());
    }
    @ParameterizedTest @ValueSource(strings={"DADOS_CONSULTADOS","SEM_RESULTADO","ERRO_INTEGRACAO","CONSULTA_BLOQUEADA","AGUARDANDO_DADOS"})
    void executorPreservaResultadoSemConsultarLlm(String status)throws Exception {
        var p=ChatV2Plan.of(ChatV2Capability.FATURAS);p.setParametros(new LinkedHashMap<>(filtros()));
        when(chat.responderFaturas(any(),anyMap(),eq(false))).thenAnswer(call->{
            ConversationRequestDTO req=call.getArgument(0);assertEquals(10L,req.codgAgencia());assertEquals(20L,req.codgUsuario());assertEquals("123",req.idErp());return resposta(status);});
        var d=new ChatConfiancaDecisaoIa();var r=executor.executar(p,session("faturas"),d);
        assertEquals("Resposta financeira",r.content());assertEquals(status,p.getResultado());
        assertEquals("ERRO_INTEGRACAO".equals(status)?"ERRO":Set.of("SEM_RESULTADO","CONSULTA_BLOQUEADA").contains(status)?"FALLBACK":"SUCESSO",d.getStatusResultado());
        verify(chat,never()).chat(any(),any(),any());verify(chat,never()).actionApis(anyList(),any(),anyString(),anyBoolean());verifyNoInteractions(tools);
    }
    @Test void executorSemPayloadNaoConfundeErroComVazio() {
        var p=ChatV2Plan.of(ChatV2Capability.FATURAS);executor.executar(p,session("faturas"),new ChatConfiancaDecisaoIa());assertEquals("ERRO_INTEGRACAO",p.getResultado());
    }
    @ParameterizedTest @ValueSource(strings={"pagar fatura 123","cancelar boleto","dar baixa na fatura","quitar boleto"})
    void operacaoFinanceiraNaoExecutavelNaoViraConsultaOuReserva(String texto) {
        var p=plan(texto,null);assertEquals(ChatV2Capability.AJUDA,p.capability());assertNotNull(p.getPergunta());
        executor.executar(p,session(texto),new ChatConfiancaDecisaoIa());verifyNoInteractions(chat,tools);
    }
    @Test void contatoDeFaturaNaoConsultaFinanceiro() {
        var p=plan("Qual contato do financeiro para faturas?",null);assertEquals(ChatV2Capability.CONTATOS,p.capability());assertEquals("financeiro",p.getParametros().get("setor"));
    }
    @Test void pdfAposBoletoTrocaCapacidadeEDocumento()throws Exception {
        var c=contexto();var n=mapper.readTree(c.getMetadadosJson());
        ((com.fasterxml.jackson.databind.node.ObjectNode)n.path("confiaV2")).put("intencao",ChatV2Capability.BOLETOS.code);
        c.setMetadadosJson(mapper.writeValueAsString(n));
        var p=plan("me mande o PDF",c);assertEquals(ChatV2Capability.FATURAS,p.capability());assertEquals("PDF",p.getParametros().get("faturaDocumento"));
        p=plan("boletos",contexto());assertEquals(ChatV2Capability.BOLETOS,p.capability());
    }
    @Test void ondeBaixarOrientaDocumentoSemInventarLink()throws Exception {
        var p=plan("onde baixar?",contexto());assertEquals("PDF",p.getParametros().get("faturaDocumento"));
    }
    @Test void humanoInterrompeContinuidadeFinanceiraV2()throws Exception {
        var h=new com.confApi.chatconfianca.dto.model.Mensagem();h.setRemetenteTipo(com.confApi.chatconfianca.dto.enums.RemetenteTipo.USUARIO);h.setRemetenteCodgUsuario(99);
        var p=planner.planejar("e as pagas?",contexto(),List.of(h),10,20);assertNotEquals(ChatV2Capability.FATURAS,p.capability());
    }
    @Test void botFinanceiroComPerguntaPendentePermiteContinuidade()throws Exception {
        var h=new com.confApi.chatconfianca.dto.model.Mensagem();h.setRemetenteTipo(com.confApi.chatconfianca.dto.enums.RemetenteTipo.BOT);h.setConteudoJson(contexto().getMetadadosJson());
        var p=planner.planejar("e as pagas?",contexto(),List.of(h),10,20);assertEquals(ChatV2Capability.FATURAS,p.capability());
    }
}
