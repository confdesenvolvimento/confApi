package com.confApi.chatconfianca.v2;

import com.confApi.chatconfianca.dto.enums.RemetenteTipo;
import com.confApi.chatconfianca.dto.model.Conversa;
import com.confApi.chatconfianca.dto.model.Mensagem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ChatV2PlannerTest {
    ChatV2Properties props; ChatV2SemanticClient semantic; ChatV2Planner planner;
    ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    @BeforeEach void setup(){props=new ChatV2Properties();props.setEnabled(true);props.setTrafficPercent(100);props.setSemanticEnabled(false);
        semantic=mock(ChatV2SemanticClient.class);planner=new ChatV2Planner(props,semantic,mapper);}
    ChatV2Plan plan(String msg,Conversa c){return planner.planejar(msg,c,List.of(),10,20);}
    Conversa conversation(ChatV2Capability cap,Map<String,String> params)throws Exception {
        ChatV2Plan state=ChatV2Plan.of(cap);state.setAgencia(10);state.setUsuario(20);state.setAtualizadoEm(System.currentTimeMillis());state.setParametros(params);
        Conversa c=new Conversa();c.setMetadadosJson(mapper.writeValueAsString(Map.of("confiaV2",state)));return c;
    }
    @ParameterizedTest @CsvSource({
        "me mostre meus limites,financeiro.limites",
        "Quero consultar minhas faturas,financeiro.faturas",
        "boletos,financeiro.boletos",
        "Quero consultar o calendario BSP,financeiro.calendario_bsp",
        "Liste minhas ultimas reservas,aereo.reservas_recentes",
        "ABRIR RESERVA ABC123,aereo.reserva_detalhes",
        "remarcar loc ABC123,aereo.simular_remarcacao",
        "reemissão,aereo.simular_remarcacao",
        "pesquise os voos,aereo.busca_voos",
        "Mostre alternativas de ida e volta de CGR para GIG limite 5,aereo.melhor_tarifa_ida_volta",
        "me mostre meus alertas,aereo.alertas_tarifa",
        "proximos embarques,aereo.checkin",
        "familias da latam,aereo.familias_tarifarias",
        "buscar hoteis,hotel.busca_hospedagem",
        "monte pacote saindo de Manaus para Fortaleza,pacote.melhor_oferta",
        "Qual o horario de funcionamento,institucional.horario_atendimento",
        "Qual o telefone da Confianca,institucional.contatos_departamentos",
        "atendimento emergencial,institucional.atendimento_emergencial",
        "erro no portal,institucional.suporte_ti",
        "formas de pagamento,financeiro.forma_pagamento",
        "boa tarde,conversa.saudacao",
        "quero falar com atendente,conversa.atendimento_humano"
    }) void frasesReaisECapacidades(String message,String code){assertEquals(code,plan(message,null).getIntencao());verifyNoInteractions(semantic);}
    @Test void mantemModalidadeSemMisturarAgencia()throws Exception {
        Conversa c=conversation(ChatV2Capability.LIMITES,Map.of());
        assertEquals("financeiro.limites",plan("e no faturado?",c).getIntencao());
        assertEquals("FATURADO",plan("e no faturado?",c).getParametros().get("modalidade"));
        assertEquals("orientacao_geral",planner.planejar("e no faturado?",c,List.of(),11,20).getIntencao());
        assertEquals("orientacao_geral",planner.planejar("e no faturado?",c,List.of(),10,21).getIntencao());
    }
    @Test void mudaAssuntoSemHerdarLocalizador()throws Exception {
        ChatV2Plan p=plan("Agora quero minhas faturas",conversation(ChatV2Capability.RESERVA,Map.of("localizador","ABC123")));
        assertEquals(ChatV2Capability.FATURAS,p.capability());assertFalse(p.getParametros().containsKey("localizador"));
    }
    @Test void completaLocalizadorParaRegras()throws Exception {
        ChatV2Plan p=plan("ABC123",conversation(ChatV2Capability.REGRAS,Map.of()));
        assertEquals(ChatV2Capability.REGRAS,p.capability());assertEquals("ABC123",p.getParametros().get("localizador"));
    }
    @Test void dataCompletaBspSemVirarAereo()throws Exception {
        ChatV2Plan p=plan("emissao 08-09",conversation(ChatV2Capability.BSP,Map.of()));
        assertEquals(ChatV2Capability.BSP,p.capability());assertTrue(p.getParametros().get("dataEmissao").endsWith("-09-08"));
    }
    @Test void contatoTiContinuaContato()throws Exception {
        ChatV2Plan p=plan("e do setor de t.i?",conversation(ChatV2Capability.CONTATOS,Map.of()));
        assertEquals(ChatV2Capability.CONTATOS,p.capability());assertEquals("ti",p.getParametros().get("setor"));
    }
    @Test void respeitaDesligamentoECoorte(){props.setEnabled(false);assertNull(plan("faturas",null));props.setEnabled(true);props.setTrafficPercent(0);assertNull(plan("faturas",null));props.setTrafficPercent(100);props.setAgencias(List.of(11));assertNull(plan("faturas",null));verifyNoInteractions(semantic);}
    @Test void semanticaPodeEscolherSomenteDadosDoPlano()throws Exception {
        props.setSemanticEnabled(true);ChatV2Plan extracted=ChatV2Plan.of(ChatV2Capability.TARIFA_VOLTA);
        extracted.setParametros(new LinkedHashMap<>(Map.of("origem","MAO","destino","FOR","mesIda","2027-01")));
        when(semantic.decidir(anyString(),any(),any())).thenReturn(extracted);
        assertEquals("FOR",plan("promocao saindo de Manaus para Fortaleza em janeiro",null).getParametros().get("destino"));
    }
    @Test void naoAceitaOrigemInventada()throws Exception {
        props.setSemanticEnabled(true);ChatV2Plan extracted=ChatV2Plan.of(ChatV2Capability.TARIFA_IDA);
        extracted.setParametros(new LinkedHashMap<>(Map.of("origem","CUI","destino","GRU")));
        when(semantic.decidir(anyString(),any(),any())).thenReturn(extracted);
        assertFalse(plan("melhor tarifa para Sao Paulo GRU",null).getParametros().containsKey("origem"));
        extracted.getParametros().put("origem","CGB");
        assertEquals("CGB",plan("melhor tarifa CGB para GRU",null).getParametros().get("origem"));
    }
    @Test void falhaSemanticaUsaFallbackExplicito()throws Exception {
        props.setSemanticEnabled(true);when(semantic.decidir(anyString(),any(),any())).thenThrow(new IOException("provider"));
        ChatV2Plan p=plan("quanto eu vendi no ano?",null);assertTrue(p.isLegado());assertEquals("PLANEJADOR_INDISPONIVEL",p.getErro());
    }
    @Test void falhaNaoReexecutaPesquisaComParametrosAntigos()throws Exception {
        props.setSemanticEnabled(true);when(semantic.decidir(anyString(),any(),any())).thenThrow(new IOException());
        ChatV2Plan p=plan("pesquise ida e volta em outro periodo",conversation(ChatV2Capability.TARIFA_VOLTA,Map.of("origem","CGB","destino","GRU")));
        assertNotNull(p.getPergunta());
    }
    @Test void acaoPersistidaNaoReclassificaLimiteCinco()throws Exception {
        props.setSemanticEnabled(true);String prompt="Mostre alternativas de ida e volta de CGR para GIG limite 5";
        Mensagem bot=new Mensagem();bot.setRemetenteTipo(RemetenteTipo.BOT);
        bot.setConteudoJson(mapper.writeValueAsString(Map.of("confiaV2",Map.of("parametros",Map.of("origem","CGR","destino","GIG")),"actions",List.of(Map.of("code","ver_alternativas_tarifas_ida_volta","prompt",prompt)))));
        ChatV2Plan p=planner.planejar(prompt,conversation(ChatV2Capability.TARIFA_VOLTA,Map.of("origem","CGR","destino","GIG")),List.of(bot),10,20);
        assertEquals("V2_ACAO_PERSISTIDA",p.getFonte());assertEquals("alternativas",p.getParametros().get("modoResposta"));verifyNoInteractions(semantic);
    }
    @Test void escopoControladoRetornaLegado(){props.setIntencoes(List.of("financeiro.faturas"));assertTrue(plan("meus limites",null).isLegado());}
    @Test void semanticaRemoveFiltrosAntigosExplicitamente()throws Exception {
        props.setSemanticEnabled(true);
        ChatV2Plan novo=ChatV2Plan.of(ChatV2Capability.TARIFA_VOLTA);novo.setContinuar(true);
        novo.setParametros(new LinkedHashMap<>(Map.of("origem","CGR","destino","GIG","mesIda","2027-05")));
        when(semantic.decidir(anyString(),any(),any())).thenReturn(novo);
        ChatV2Plan p=plan("agora somente em maio",conversation(ChatV2Capability.TARIFA_VOLTA,Map.of("origem","CGR","destino","GIG","dataIda","2027-04-05")));
        assertFalse(p.getParametros().containsKey("dataIda"));assertEquals("2027-05",p.getParametros().get("mesIda"));
    }
    @Test void fraseAmbiguaNaoExecutaSoPorKeyword()throws Exception {
        props.setSemanticEnabled(true);ChatV2Plan esclarecimento=ChatV2Plan.of(ChatV2Capability.AJUDA);
        esclarecimento.setPergunta("Deseja consultar o boleto ou falar com o financeiro?");
        when(semantic.decidir(anyString(),any(),any())).thenReturn(esclarecimento);
        assertEquals(ChatV2Capability.AJUDA,plan("quero cancelar o boleto",null).capability());
    }
    @Test void palavraComSeisLetrasNaoReaproveitaFinanceiro()throws Exception {
        assertEquals(ChatV2Capability.AJUDA,plan("vendas",conversation(ChatV2Capability.LIMITES,Map.of())).capability());
    }
    @Test void catalogoTemCodigosUnicosEExecutoresFechados(){assertEquals(28,ChatV2Capability.codes().size());assertEquals(28,new HashSet<>(ChatV2Capability.codes()).size());for(var c:ChatV2Capability.values())if(c.tool!=null)assertEquals(c.tool,ChatV2Arguments.tool(c).name());}
}
