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

    @Test void remarcacaoNaoEncontradaApenasInformaSemPrepararAcaoOuConsultarLlm() throws Exception {
        ChatV2Plan plan = ChatV2Plan.of(ChatV2Capability.REMARCACAO);
        plan.getParametros().put("localizador", "ZZZ999");
        ChatMessageDTO dado = bloqueioRemarcacao("NAO_ENCONTRADA",
                "Nao encontrei a reserva ZZZ999. Confira o localizador e tente novamente.");
        configurarBloqueio(dado);
        when(chat.extrairAcoesDisponiveis(anyList())).thenReturn(List.of(acaoSimular("ABC123")));
        ChatConfiancaDecisaoIa decisao = decision();

        ChatResponseDTO response = exec.executar(plan, sessaoRemarcacao("ZZZ999"), decisao);

        assertEquals("Nao encontrei a reserva ZZZ999. Confira o localizador e tente novamente.", response.content());
        assertEquals("SEM_RESULTADO", plan.getResultado());
        assertEquals("FALLBACK", decisao.getStatusResultado());
        assertTrue(response.actions().isEmpty());
        assertTrue(response.toolCalls().isEmpty());
        assertEquals(List.of(dado), response.history());
        verify(chat, never()).chat(any(), anyList(), any());
        verifyNoInteractions(router);
    }

    @Test void falhaAoValidarLocalizadorNaoAfirmaReservaInexistenteNemPreparaSimulacao() throws Exception {
        ChatV2Plan plan = ChatV2Plan.of(ChatV2Capability.REMARCACAO);
        plan.getParametros().put("localizador", "ZZZ999");
        ChatMessageDTO dado = bloqueioRemarcacao("ERRO_CONSULTA",
                "Nao foi possivel consultar a reserva ZZZ999 agora. Tente novamente.");
        configurarBloqueio(dado);
        ChatConfiancaDecisaoIa decisao = decision();

        ChatResponseDTO response = exec.executar(plan, sessaoRemarcacao("ZZZ999"), decisao);

        assertEquals("Nao foi possivel consultar a reserva ZZZ999 agora. Tente novamente.", response.content());
        assertEquals("ERRO_INTEGRACAO", plan.getResultado());
        assertEquals("ERRO", decisao.getStatusResultado());
        assertTrue(response.actions().isEmpty());
        assertTrue(response.toolCalls().isEmpty());
        verify(chat, never()).chat(any(), anyList(), any());
        verifyNoInteractions(router);
    }

    @Test void localizadorValidadoPreservaSeletorAtualSemExecutarRemarcacao() throws Exception {
        ChatV2Plan plan = ChatV2Plan.of(ChatV2Capability.REMARCACAO);
        plan.getParametros().put("localizador", "ABC123");
        ChatActionDTO acao = acaoSimular("ABC123");
        ChatMessageDTO dado = new ChatMessageDTO("system",
                "{\"tipoConsulta\":\"seletor_remarcacao\",\"localizador\":\"ABC123\"}");
        when(chat.actionApis(anyList(), any(), eq("selecionar_reserva_remarcacao"), eq(true)))
                .thenAnswer(invocation -> {
                    List<ChatMessageDTO> dados = invocation.getArgument(0);
                    dados.add(dado);
                    return List.of("selecionar_reserva_remarcacao");
                });
        when(chat.extrairAcoesDisponiveis(anyList())).thenReturn(List.of(acao));

        ChatResponseDTO response = exec.executar(plan, sessaoRemarcacao("ABC123"), decision());

        assertEquals("ACAO_PREPARADA", plan.getResultado());
        assertTrue(response.content().contains("Use o seletor"));
        assertEquals(List.of(acao), response.actions());
        assertTrue(response.toolCalls().isEmpty());
        verify(chat, never()).chat(any(), anyList(), any());
        verifyNoInteractions(router);
    }

    private ChatMessageDTO bloqueioRemarcacao(String status, String mensagem) throws Exception {
        return new ChatMessageDTO("system", mapper.writeValueAsString(Map.of(
                "tipoConsulta", "validacao_remarcacao", "statusConsulta", status,
                "localizadorConsultado", "ZZZ999", "mensagem", mensagem, "acoesDisponiveis", List.of())));
    }

    private void configurarBloqueio(ChatMessageDTO dado) throws Exception {
        when(chat.actionApis(anyList(), any(), eq("selecionar_reserva_remarcacao"), eq(true)))
                .thenAnswer(invocation -> {
                    List<ChatMessageDTO> dados = invocation.getArgument(0);
                    assertTrue(dados.isEmpty(), "A consulta deve avaliar apenas os dados do turno atual.");
                    dados.add(dado);
                    return List.of("selecionar_reserva_remarcacao");
                });
        when(chat.respostaBloqueioRemarcacao(anyList(), anyList())).thenAnswer(invocation -> {
            List<ChatMessageDTO> dados = invocation.getArgument(0);
            assertEquals(List.of(dado), dados);
            return new ChatResponseDTO(null, mapper.readTree(dado.content()).path("mensagem").asText(),
                    List.of(), null, invocation.getArgument(1), List.copyOf(dados), List.of());
        });
    }

    private ConversationRequestDTO sessaoRemarcacao(String localizador) {
        return new ConversationRequestDTO("confia", "CGB", "ERP-10", 10L, 20L,
                "Quero remarcar a reserva " + localizador,
                List.of(new ChatMessageDTO("system",
                        "{\"tipoConsulta\":\"seletor_remarcacao\",\"localizador\":\"ANT123\","
                                + "\"acoesDisponiveis\":[{\"code\":\"simular_remarcacao\"}]}")),
                null, false, List.of());
    }

    private ChatActionDTO acaoSimular(String localizador) {
        return new ChatActionDTO("simular_remarcacao", "Simular remarcacao", "Preparar simulacao",
                localizador, null, false, true, false, "Simular remarcacao " + localizador);
    }
}
