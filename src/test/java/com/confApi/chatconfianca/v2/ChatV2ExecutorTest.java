package com.confApi.chatconfianca.v2;

import com.confApi.chatconfianca.intencao.*;
import com.confApi.chatgpt.dto.*;
import com.confApi.chatgpt.service.ChatService;
import com.confApi.chatgpt.tools.ToolRouter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ChatV2ExecutorTest {
    ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();ChatService chat;ToolRouter router;ChatV2Executor exec;
    ConversationRequestDTO session=new ConversationRequestDTO("confia","CGB","ERP-10",10L,20L,"pedido",new ArrayList<>(),null,false,new ArrayList<>());
    @BeforeEach void setup(){chat=mock(ChatService.class);router=mock(ToolRouter.class);exec=new ChatV2Executor(chat,router,mapper);}
    ChatConfiancaDecisaoIa decision(){return new ChatConfiancaDecisaoIa();}
    @Test void financeiroUsaExecutorExistenteComIdentidadeDaSessao()throws Exception {
        ChatV2Plan p=ChatV2Plan.of(ChatV2Capability.LIMITES);p.getParametros().put("agencia","999");
        when(chat.actionApis(anyList(),any(),eq("limites"),eq(true))).thenAnswer(inv->{List<ChatMessageDTO> msgs=inv.getArgument(0);msgs.add(new ChatMessageDTO("system","Limites da agencia"));ConversationRequestDTO req=inv.getArgument(1);assertEquals(10L,req.codgAgencia());assertEquals("ERP-10",req.idErp());return List.of("limites");});
        when(chat.extrairAcoesDisponiveis(anyList())).thenReturn(List.of());
        when(chat.chat(any(),any(),any())).thenReturn(new ChatResponseDTO(null,"Dados consultados",List.of(),null,List.of(),List.of()));
        exec.executar(p,session,decision());verify(chat,never()).actionApis(anyList(),any());verifyNoInteractions(router);assertEquals("DADOS_CONSULTADOS",p.getResultado());
    }
    @Test void nuncaExecutaAcaoSensivelApenasCarregaReserva()throws Exception {
        ChatV2Plan p=ChatV2Plan.of(ChatV2Capability.ACOES_RESERVA);p.getParametros().put("localizador","ABC123");
        exec.executar(p,session,decision());verify(chat).actionApis(anyList(),any(),eq("reserva_aerea_detalhes"),eq(true));verifyNoInteractions(router);
    }
    @Test void perguntaPendenteNaoConsultaNada(){ChatV2Plan p=ChatV2Plan.of(ChatV2Capability.VOOS);p.setPergunta("Qual origem?");assertEquals("Qual origem?",exec.executar(p,session,decision()).content());verifyNoInteractions(chat,router);assertEquals("AGUARDANDO_DADOS",p.getResultado());}
    @Test void pesquisaEhExecutadaUmaVezSemSegundoClassificador(){ChatV2Plan p=ChatV2Plan.of(ChatV2Capability.TARIFA_VOLTA);p.setParametros(new LinkedHashMap<>(Map.of("origem","CGB","destino","GRU")));when(router.execute(eq("search_cheapest_roundtrip_airfares"),anyMap())).thenReturn(Map.of("status","OK","mensagem","Ofertas consultadas"));exec.executar(p,session,decision());verify(router,times(1)).execute(eq("search_cheapest_roundtrip_airfares"),anyMap());verifyNoInteractions(chat);}
    @Test void cacheVazioNaoEhSucessoDeNegocio(){ChatV2Plan p=ChatV2Plan.of(ChatV2Capability.PACOTE);p.setParametros(new LinkedHashMap<>(Map.of("origem","MAO","destino","FOR")));when(router.execute(anyString(),anyMap())).thenReturn(Map.of("status","SEM_DADOS","mensagem","Sem pacote no cache."));var d=decision();String out=exec.executar(p,session,d).content();assertEquals("CACHE_SEM_DADOS",p.getResultado());assertEquals("FALLBACK",d.getStatusResultado());assertTrue(out.contains("nao confirma ausencia"));}
    @Test void dataInvertidaNaoChegaAoExecutor(){ChatV2Plan p=ChatV2Plan.of(ChatV2Capability.TARIFA_VOLTA);p.setParametros(new LinkedHashMap<>(Map.of("origem","CGR","destino","GIG","dataIdaInicio","2027-04-11","dataIdaFim","2027-04-05")));exec.executar(p,session,decision());assertEquals("AGUARDANDO_DADOS",p.getResultado());verifyNoInteractions(router,chat);}
    @Test void bspUsaIntervaloInclusivoSemGeracaoLivre(){ChatV2Plan p=ChatV2Plan.of(ChatV2Capability.BSP);p.getParametros().put("dataEmissao","2026-09-08");var d=decision();ChatIntencaoRuntimeDto.Memoria m=new ChatIntencaoRuntimeDto.Memoria();m.setCodgMemoria(11);m.setTexto("{\"periodos\":[{\"data_inicio\":\"2026-09-01\",\"data_fim\":\"2026-09-08\",\"vencimento_bsp\":\"2026-09-14\",\"vencimento_gol_azul_terrestre\":\"2026-09-17\",\"vencimento_gr\":\"2026-10-06\"},{\"data_inicio\":\"2026-09-09\",\"data_fim\":\"2026-09-15\",\"vencimento_bsp\":\"2026-09-21\"}]}");d.setMemorias(List.of(m));String text=exec.executar(p,session,d).content();assertTrue(text.contains("2026-09-14"));assertFalse(text.contains("2026-09-21"));verifyNoInteractions(router,chat);}
    @Test void semMemoriaNaoCarregaBaseGeral(){var p=ChatV2Plan.of(ChatV2Capability.PAGAMENTO);exec.executar(p,session,decision());assertEquals("SEM_CONHECIMENTO",p.getResultado());verifyNoInteractions(chat,router);}
    @Test void contatosRegularesNaoUsamTelefoneEmergencial(){var p=ChatV2Plan.of(ChatV2Capability.HORARIO);var d=decision();ChatIntencaoRuntimeDto.Memoria m=new ChatIntencaoRuntimeDto.Memoria();m.setCodgMemoria(7);m.setTexto("{\"nucleoAtendimento\":{\"telefone\":\"REGULAR\",\"horario\":{\"segunda_sexta\":\"09h as 18h\"}},\"atendimentoEmergencial\":{\"telefone\":\"PLANTAO\"}}");d.setMemorias(List.of(m));String text=exec.executar(p,session,d).content();assertTrue(text.contains("REGULAR"));assertFalse(text.contains("PLANTAO"));}
    @Test void localizadorPendenteMantemIntencao(){var p=ChatV2Plan.of(ChatV2Capability.REGRAS);exec.executar(p,session,decision());assertEquals("Qual e o localizador da reserva?",p.getPergunta());verifyNoInteractions(chat,router);}
    @Test void falhaDaIntegracaoNaoTentaOutroExecutor(){var p=ChatV2Plan.of(ChatV2Capability.RESERVAS);when(chat.responderListagemReservasRecentes(any())).thenThrow(new IllegalStateException());var d=decision();exec.executar(p,session,d);assertEquals("ERRO_INTEGRACAO",p.getResultado());assertEquals("ERRO",d.getStatusResultado());verify(chat,times(1)).responderListagemReservasRecentes(any());verifyNoInteractions(router);}
    @Test void dataExataNaoPodeConflitarComIntervalo(){var p=ChatV2Plan.of(ChatV2Capability.TARIFA_VOLTA);p.setParametros(Map.of("origem","CGR","destino","RIO","dataIda","2027-04-05","dataIdaFim","2027-04-06"));assertThrows(IllegalArgumentException.class,()->ChatV2Arguments.validar(p,mapper));}
    @Test void erroEstruturadoDeReservasNaoContaComoSucesso() {
        var p=ChatV2Plan.of(ChatV2Capability.RESERVAS);var d=decision();
        when(chat.responderListagemReservasRecentes(any())).thenReturn(new ChatResponseDTO(null,null,List.of(),null,List.of(),
                List.of(new ChatMessageDTO("system","Dado: {\"reservasRecentes\":{\"status\":\"ERRO\",\"reservas\":[]}}\nOriente tentar novamente."))));
        exec.executar(p,session,d);assertEquals("ERRO_INTEGRACAO",p.getResultado());assertEquals("ERRO",d.getStatusResultado());
    }
    @Test void listaVaziaDeReservasEhSemResultado() {
        var p=ChatV2Plan.of(ChatV2Capability.RESERVAS);
        when(chat.responderListagemReservasRecentes(any())).thenReturn(new ChatResponseDTO(null,null,List.of(),null,List.of(),
                List.of(new ChatMessageDTO("system","Dado: {\"reservasRecentes\":{\"status\":\"OK\",\"reservas\":[]}}"))));
        exec.executar(p,session,decision());assertEquals("SEM_RESULTADO",p.getResultado());
    }
    @Test void hotelRejeitaIdadesEOcupacaoInconsistentes() {
        var p=ChatV2Plan.of(ChatV2Capability.HOTEL);
        p.setParametros(new LinkedHashMap<>(Map.of("destino","Recife","checkin","2027-04-05","checkout","2027-04-06","totalHospedes","3",
                "quartosJson","[{\"adultos\":2,\"criancas\":1,\"idadesCriancas\":[]}]")));
        exec.executar(p,session,decision());assertEquals("AGUARDANDO_DADOS",p.getResultado());verifyNoInteractions(router,chat);
    }
}
