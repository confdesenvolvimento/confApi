package com.confApi.chatconfianca.v2;

import com.confApi.chatconfianca.dto.model.Conversa;
import com.confApi.chatconfianca.intencao.*;
import com.confApi.chatgpt.dto.*;
import com.confApi.chatgpt.service.ChatService;
import com.confApi.chatgpt.tools.ToolRouter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatV2CasosLocaisRegressionTest {
    ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    ChatService chat;ToolRouter router;ChatV2Executor executor;
    ChatV2Properties properties;ChatV2SemanticClient semantic;ChatV2Planner planner;
    @BeforeEach void setup() {
        chat=mock(ChatService.class);router=mock(ToolRouter.class);executor=new ChatV2Executor(chat,router,mapper);
        properties=new ChatV2Properties();properties.setEnabled(true);properties.setTrafficPercent(100);
        semantic=mock(ChatV2SemanticClient.class);planner=new ChatV2Planner(properties,semantic,mapper);
    }
    Conversa anterior(ChatV2Capability c,Map<String,String> params)throws Exception {
        var p=ChatV2Plan.of(c);p.setAgencia(10);p.setUsuario(20);p.setAtualizadoEm(System.currentTimeMillis());p.setParametros(params);
        var conversa=new Conversa();conversa.setMetadadosJson(mapper.writeValueAsString(Map.of("confiaV2",p)));return conversa;
    }
    ConversationRequestDTO sessao(String pergunta) {
        return new ConversationRequestDTO("confia","CGB","ERP-10",10L,20L,pergunta,new ArrayList<>(),null,false,new ArrayList<>());
    }
    ChatIntencaoRuntimeDto.Memoria memoria(int id,String text) {
        var m=new ChatIntencaoRuntimeDto.Memoria();m.setCodgMemoria(id);m.setTexto(text);return m;
    }
    ChatConfiancaDecisaoIa decisao(ChatIntencaoRuntimeDto.Memoria... memorias) {
        var d=new ChatConfiancaDecisaoIa();d.setMemorias(List.of(memorias));return d;
    }
    String responder(ChatV2Plan p,String mensagem,ChatConfiancaDecisaoIa d) {
        return executor.executar(p,sessao(mensagem),d).content();
    }
    String emergencia()throws Exception {
        return mapper.writeValueAsString(Map.of("atendimentoEmergencial",Map.of(
            "descricao","Plantão de emergência","telefone","PLANTAO_TESTE",
            "whatsapp","WHATSAPP_TESTE","informacoes","O canal atende pedidos \"urgentes\".",
            "horarios",Map.of("segunda_sexta","19h às 08h","sabado","13h às 08h"),
            "equipe",List.of("NOME_INTERNO"),"aviso","Não informe senha ou token por mensagem.")));
    }
    @Test void proximosCheckinsNaoHerdamReservaAnteriorNemChamamSemantica()throws Exception {
        var contaminado=ChatV2Plan.of(ChatV2Capability.CHECKIN);
        contaminado.setParametros(new LinkedHashMap<>(Map.of("localizador","FLSEMF")));
        when(semantic.decidir(anyString(),any(),any())).thenReturn(contaminado);
        var p=planner.planejar("Quais são meus próximos check-ins?",
                anterior(ChatV2Capability.RESERVA,Map.of("localizador","FLSEMF")),List.of(),10,20);
        assertEquals(ChatV2Capability.CHECKIN,p.capability());assertFalse(p.getParametros().containsKey("localizador"));
        assertFalse(p.isContinuar());verifyNoInteractions(semantic);
    }
    @ParameterizedTest @ValueSource(strings={"check-in","check-ins","checkin","checkins","check in","check ins"})
    void reconheceVariantesDeCheckin(String variante) {
        properties.setSemanticEnabled(false);
        var p=planner.planejar("Quais são meus próximos "+variante+"?",null,List.of(),10,20);
        assertEquals(ChatV2Capability.CHECKIN,p.capability());
    }
    @Test void parametrosSemanticosDeReservaNaoEntramEmCheckin()throws Exception {
        var contaminado=ChatV2Plan.of(ChatV2Capability.CHECKIN);
        contaminado.setParametros(Map.of("localizador","FLSEMF"));
        when(semantic.decidir(anyString(),any(),any())).thenReturn(contaminado);
        var p=planner.planejar("O que tenho para embarcar nos próximos dias?",
                anterior(ChatV2Capability.RESERVA,Map.of("localizador","FLSEMF")),List.of(),10,20);
        assertEquals(ChatV2Capability.CHECKIN,p.capability());assertFalse(p.getParametros().containsKey("localizador"));
    }
    @Test void checkinDeHotelNaoViraConsultaAerea() {
        properties.setSemanticEnabled(false);
        var p=planner.planejar("Qual o check-in do hotel?",null,List.of(),10,20);
        assertEquals(ChatV2Capability.HOTEL_DADOS,p.capability());
    }
    @Test void setorFinanceiroExplicitoNaoSePerdeNaSemantica()throws Exception {
        when(semantic.decidir(anyString(),any(),any())).thenReturn(ChatV2Plan.of(ChatV2Capability.CONTATOS));
        var p=planner.planejar("Qual o contato do financeiro?",null,List.of(),10,20);
        assertEquals("financeiro",p.getParametros().get("setor"));
    }
    @Test void setorExplicitoSubstituiSetorAntigoSemRessuscitarParametros()throws Exception {
        var novo=ChatV2Plan.of(ChatV2Capability.CONTATOS);novo.setParametros(Map.of("setor","ti","localizador","FLSEMF"));
        when(semantic.decidir(anyString(),any(),any())).thenReturn(novo);
        var p=planner.planejar("Agora qual o contato do financeiro?",
                anterior(ChatV2Capability.CONTATOS,Map.of("setor","ti")),List.of(),10,20);
        assertEquals("financeiro",p.getParametros().get("setor"));assertFalse(p.getParametros().containsKey("localizador"));
    }
    @Test void executorCheckinNaoEnviaLocalizadorAntigoAoServicoOuModelo()throws Exception {
        var p=ChatV2Plan.of(ChatV2Capability.CHECKIN);p.setParametros(Map.of("localizador","FLSEMF"));
        when(chat.actionApis(anyList(),any(),eq("checkin"),eq(true))).thenAnswer(inv->{
            ConversationRequestDTO req=inv.getArgument(1);
            assertEquals(10L,req.codgAgencia());assertEquals("ERP-10",req.idErp());assertFalse(req.input().contains("FLSEMF"));
            List<ChatMessageDTO> dados=inv.getArgument(0);
            dados.add(new ChatMessageDTO("system","Dado do sistema: {\"reservaCheckInIA\":[{\"localizadorCompanhia\":\"ABC123\"}]}"));
            return List.of("checkin");
        });
        when(chat.chat(any(),any(),any())).thenReturn(new ChatResponseDTO("mock","Embarque ABC123",List.of(),null,List.of(),List.of()));
        assertEquals("Embarque ABC123",responder(p,"Quais são meus próximos check-ins?",decisao()));
        var captor=ArgumentCaptor.forClass(ChatRequestDTO.class);verify(chat).chat(captor.capture(),any(),any());
        assertFalse(mapper.writeValueAsString(captor.getValue()).contains("FLSEMF"));assertFalse(p.getParametros().containsKey("localizador"));
    }
    @Test void checkinVazioNaoPedeAoModeloParaInventarJustificativa()throws Exception {
        var p=ChatV2Plan.of(ChatV2Capability.CHECKIN);p.setParametros(Map.of("localizador","FLSEMF"));
        when(chat.actionApis(anyList(),any(),eq("checkin"),eq(true))).thenAnswer(inv->{
            List<ChatMessageDTO> dados=inv.getArgument(0);
            dados.add(new ChatMessageDTO("system","Dado do sistema: {\"reservaCheckInIA\":[]}"));return List.of("checkin");
        });
        String out=responder(p,"Quais são meus próximos check-ins?",decisao());
        assertEquals("SEM_RESULTADO",p.getResultado());assertTrue(out.contains("embarques"));
        assertFalse(out.contains("FLSEMF"));verify(chat,never()).chat(any(),any(),any());
    }
    @Test void financeiroNaoUsaNucleoGeralQuandoPlanoPerdeSetor() {
        var p=ChatV2Plan.of(ChatV2Capability.CONTATOS);
        var d=decisao(memoria(7,"{\"nucleoAtendimento\":{\"descricao\":\"Núcleo geral\",\"telefone\":\"REGULAR_TESTE\"}}"));
        String out=responder(p,"Qual o contato do financeiro?",d);
        assertEquals("SEM_CONHECIMENTO",p.getResultado());assertFalse(out.contains("REGULAR_TESTE"));
        assertFalse(out.contains("cadastro de conhecimento"));verifyNoInteractions(chat,router);
    }
    @Test void buscaSetorNasMemoriasElegiveisSemRetornarOutroDepartamento() {
        var p=ChatV2Plan.of(ChatV2Capability.CONTATOS);p.setParametros(Map.of("setor","financeiro"));
        var d=decisao(
            memoria(7,"{\"nucleoAtendimento\":{\"telefone\":\"REGULAR_TESTE\"}}"),
            memoria(9,"{\"setores\":[{\"setor\":\"Financeiro\",\"telefone\":\"FINANCEIRO_TESTE\",\"email\":\"financeiro@example.invalid\"}]}"));
        String out=responder(p,"Qual o contato do financeiro?",d);
        assertEquals("RESPONDIDO_COM_FONTE",p.getResultado());assertTrue(out.contains("FINANCEIRO_TESTE"));
        assertFalse(out.contains("REGULAR_TESTE"));assertFalse(out.contains("Fonte: cadastro"));
        assertEquals(List.of(9),d.getMemorias().stream().map(ChatIntencaoRuntimeDto.Memoria::getCodgMemoria).toList());
    }
    @Test void nomeParcialTiNaoSelecionaInstitucional() {
        var p=ChatV2Plan.of(ChatV2Capability.CONTATOS);p.setParametros(Map.of("setor","ti"));
        var d=decisao(memoria(7,"{\"setores\":[{\"setor\":\"Institucional\",\"telefone\":\"ERRADO\"},{\"setor\":\"TI\",\"telefone\":\"TI_CORRETO\"}]}"));
        String out=responder(p,"Qual o contato de TI?",d);assertTrue(out.contains("TI_CORRETO"));assertFalse(out.contains("ERRADO"));
    }
    @ParameterizedTest @ValueSource(ints={0,1,2,3,4})
    void emergenciaDecodificaCamadasSemExporJsonOuId(int camadas)throws Exception {
        String doc=emergencia();for(int i=0;i<camadas;i++)doc=mapper.writeValueAsString(doc);
        var p=ChatV2Plan.of(ChatV2Capability.EMERGENCIA);
        var d=decisao(memoria(8,doc));String out=responder(p,"Preciso do contato do plantão de emergência",d);
        assertEquals("RESPONDIDO_COM_FONTE",p.getResultado());assertTrue(out.contains("PLANTAO_TESTE"));assertTrue(out.contains("WHATSAPP_TESTE"));
        assertTrue(out.contains("Segunda a sexta: 19h às 08h"));assertTrue(out.contains("\"urgentes\""));
        assertFalse(out.contains("NOME_INTERNO"));assertFalse(out.contains("atendimentoEmergencial"));assertFalse(out.contains("Fonte:"));
        assertFalse(out.contains("\\\""));assertFalse(out.contains("{"));assertEquals(8,d.getMemorias().get(0).getCodgMemoria());
    }
    @Test void aceitaEscapeLegadoSemWrapperSemSubstituirAspasDoConteudo()throws Exception {
        String quoted=mapper.writeValueAsString(emergencia());
        String escaped=quoted.substring(1,quoted.length()-1);
        var p=ChatV2Plan.of(ChatV2Capability.EMERGENCIA);
        String out=responder(p,"Preciso do plantão",decisao(memoria(8,escaped)));
        assertEquals("RESPONDIDO_COM_FONTE",p.getResultado());assertTrue(out.contains("\"urgentes\""));assertFalse(out.contains("\\\""));
    }
    @Test void jsonMalformadoOuComSobraNaoEExibidoNemReparadoSilenciosamente()throws Exception {
        for(String raw:List.of(emergencia().substring(0,emergencia().length()-1),emergencia()+" TEXTO_EXTRA",
                "{\"atendimentoEmergencial\":",mapper.writeValueAsString("{\"atendimentoEmergencial\":"))) {
            var p=ChatV2Plan.of(ChatV2Capability.EMERGENCIA);
            String out=responder(p,"Preciso do plantão",decisao(memoria(8,raw)));
            assertEquals("SEM_CONHECIMENTO",p.getResultado());assertFalse(out.contains("{"));assertFalse(out.contains("PLANTAO_TESTE"));
        }
    }
    @Test void horarioRegularContinuaSeparadoDoPlantaoSemRotuloTecnico() {
        var p=ChatV2Plan.of(ChatV2Capability.HORARIO);
        String out=responder(p,"Qual o horário regular?",decisao(memoria(7,
            "{\"nucleoAtendimento\":{\"descricao\":\"Atendimento regular\",\"telefone\":\"REGULAR_TESTE\",\"horario\":{\"sabado\":\"09h às 13h\"}},\"atendimentoEmergencial\":{\"telefone\":\"PLANTAO_TESTE\"}}")));
        assertTrue(out.contains("Sábado: 09h às 13h"));assertFalse(out.contains("PLANTAO_TESTE"));
        assertFalse(out.contains("descricao:"));assertFalse(out.contains("cadastro de conhecimento"));
    }
    @Test void textoEditorialSimplesContinuaDisponivelSemIdInterno() {
        var p=ChatV2Plan.of(ChatV2Capability.PAGAMENTO);
        String out=responder(p,"Quais as formas de pagamento?",decisao(memoria(17,"Consulte as condições de pagamento com o financeiro.")));
        assertEquals("Consulte as condições de pagamento com o financeiro.",out);
    }
    @Test void bspMantemDataDaFonteSemIdentificadorInterno() {
        var p=ChatV2Plan.of(ChatV2Capability.BSP);p.setParametros(Map.of("dataEmissao","2026-09-08"));
        String out=responder(p,"BSP 08/09/2026",decisao(memoria(11,
            "{\"periodos\":[{\"data_inicio\":\"2026-09-01\",\"data_fim\":\"2026-09-08\",\"vencimento_bsp\":\"2026-09-14\"}]}")));
        assertTrue(out.contains("2026-09-14"));assertFalse(out.contains("memoria 11"));assertFalse(out.contains("cadastro"));
    }
}
