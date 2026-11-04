package com.confApi.aereo.reemissao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import com.confApi.exception.RegraDeNegocioException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DivisaoPassageirosTest {
    final ReemissaoAzulServiceTest f = new ReemissaoAzulServiceTest();
    ObjectNode original, filha, plano; String operacao;
    @BeforeEach void setup() throws Exception {
        f.setup();
        original = ((ObjectNode)f.hub.executar("consultar",f.json("{}"))).deepCopy();
        ObjectNode o = (ObjectNode)original.get("original"); o.withArray("passageiros").addObject().put("chave","pax-2").put("tipo","ADT").put("bebeAssociado",false).putObject("nome").put("first","BIA").put("last","SILVA");
        filha = original.deepCopy(); filha.with("original").put("localizador","DEF456"); filha.with("original").withArray("passageiros").remove(1);
        when(f.hub.executar(eq("consultar"),any())).thenAnswer(i -> "DEF456".equals(((JsonNode)i.getArgument(1)).path("localizador").asText()) ? filha.deepCopy() : original.deepCopy());
        when(f.hub.executar(eq("divisao/validar"),any())).thenReturn(f.json("{\"status\":\"VALIDADA\"}"));
        plano = f.json("{\"assinatura\":\"assinatura-local\",\"valorTransferencia\":115}"); plano.set("originalEsperada",o); plano.putArray("passageiroKeys").add("pax-1");
        when(f.manager.post(endsWith("/divisao/planejar"),any(),eq(JsonNode.class))).thenReturn(plano);
        when(f.manager.post(endsWith("/divisao/autorizar"),any(),eq(JsonNode.class))).thenReturn(f.json("{\"autorizada\":true,\"status\":\"PENDENTE\"}"));
        when(f.manager.post(endsWith("/divisao/sincronizar"),any(),eq(JsonNode.class))).thenReturn(f.json("{\"status\":\"CONCLUIDA\",\"reservaId\":20}"));
        when(f.manager.post(endsWith("/divisao/consultar"),any(),eq(JsonNode.class))).thenAnswer(i -> { ObjectNode e=f.json("{\"status\":\"PENDENTE\"}"); e.set("plano",plano); return e; });
        when(f.manager.get("reservaAereo/20",JsonNode.class)).thenAnswer(i -> { ObjectNode r=f.reserva.deepCopy();r.put("localizador","DEF456");return r; });
        when(f.hub.executar(eq("divisao/dividir"),any())).thenAnswer(i -> prova(((JsonNode)i.getArgument(1)).path("operacaoId").asText()));
        when(f.hub.executar(eq("divisao/recuperar"),any())).thenAnswer(i -> prova(((JsonNode)i.getArgument(1)).path("operacaoId").asText()));
        clearInvocations(f.hub);
    }
    ObjectNode prova(String id) { return f.json("{\"status\":\"DIVIDIDA\",\"fonte\":\"Azul\",\"operacaoId\":\""+id+"\"}"); }
    ObjectNode selecionar() { ObjectNode r=f.iniciar(); assertEquals("PASSAGEIROS",r.path("status").asText());operacao=r.path("id").asText();return f.acao("passageiros",f.json("{\"passageiroKeys\":[\"pax-1\"]}")); }
    @Test void selecaoTotalSegueParaPoliticaSemDividir() {
        assertEquals("PASSAGEIROS",f.iniciar().path("status").asText());
        assertEquals("POLITICA",f.acao("passageiros",f.json("{\"passageiroKeys\":[\"pax-1\",\"pax-2\"]}")).path("status").asText());
        verify(f.hub,never()).executar(startsWith("divisao/"),any());verify(f.manager,never()).post(contains("/divisao/"),any(),eq(JsonNode.class));
    }
    @Test void selecaoParcialExigeConfirmacaoExplicitaEAbreSomenteReservaFilha() {
        assertEquals("DIVISAO_CONFIRMACAO",selecionar().path("status").asText());verify(f.hub,never()).executar(eq("divisao/dividir"),any());
        assertThrows(RegraDeNegocioException.class,()->f.acao("confirmar-divisao",f.json("{\"aceite\":false}")));
        ObjectNode nova=f.acao("confirmar-divisao",f.json("{\"aceite\":true}"));assertEquals("POLITICA",nova.path("status").asText());assertEquals(20,nova.path("reservaId").asInt());
        assertEquals("DEF456",nova.path("localizador").asText());assertEquals(1,nova.path("original").path("passageiros").size());assertTrue(nova.path("divisaoConcluida").asBoolean());assertFalse(nova.has("aceiteEm"));
        var ordem=inOrder(f.manager,f.hub);ordem.verify(f.manager).post(endsWith("/divisao/autorizar"),any(),eq(JsonNode.class));ordem.verify(f.hub).executar(eq("divisao/dividir"),any());ordem.verify(f.manager).post(endsWith("/divisao/sincronizar"),any(),eq(JsonNode.class));
        verify(f.hub,never()).executar(eq("concluir"),any());verify(f.manager,never()).post(endsWith("/solicitar"),any(),any());
    }
    @Test void timeoutDaDivisaoAtualizaSomentePorConsultaSemNovoPost() {
        selecionar();when(f.hub.executar(eq("divisao/dividir"),any())).thenThrow(new RuntimeException("timeout"));
        assertEquals("DIVISAO_PENDENTE",f.acao("confirmar-divisao",f.json("{\"aceite\":true}")).path("status").asText());
        assertThrows(RegraDeNegocioException.class,()->f.acao("cancelar",f.json("{}")));
        assertEquals(20,f.acao("consultar",f.json("{}")).path("reservaId").asInt());verify(f.hub,times(1)).executar(eq("divisao/dividir"),any());verify(f.hub,times(1)).executar(eq("divisao/recuperar"),any());
    }
    @Test void perdaDaSessaoConfApiRecuperaPlanoPersistidoSemRepetirDivisao() {
        selecionar();when(f.hub.executar(eq("divisao/dividir"),any())).thenThrow(new RuntimeException("timeout"));f.acao("confirmar-divisao",f.json("{\"aceite\":true}"));
        when(f.manager.get(endsWith("/disponibilidade"),eq(JsonNode.class))).thenReturn(f.json("{\"disponivel\":false,\"divisoesPendentes\":[\""+operacao+"\"]}"));
        f.service=new ReemissaoAzulService(f.manager,f.usuarios,f.aereo,f.hub,f.regras,f.aeroportos,f.limites,f.mapper);
        assertEquals(20,f.iniciar().path("reservaId").asInt());verify(f.hub,times(1)).executar(eq("divisao/dividir"),any());
    }
    @Test void sincronizacaoLocalPendenteNaoRepeteDivisaoNoFornecedor() {
        selecionar();when(f.manager.post(endsWith("/divisao/sincronizar"),any(),eq(JsonNode.class))).thenThrow(new RuntimeException("indisponivel"));
        assertEquals("DIVISAO_PENDENTE",f.acao("confirmar-divisao",f.json("{\"aceite\":true}")).path("status").asText());
        when(f.manager.post(endsWith("/divisao/sincronizar"),any(),eq(JsonNode.class))).thenReturn(f.json("{\"status\":\"CONCLUIDA\",\"reservaId\":20}"));
        assertEquals(20,f.iniciar().path("reservaId").asInt());verify(f.hub,times(1)).executar(eq("divisao/dividir"),any());
    }
    @Test void selecaoVaziaDuplicadaOuAlheiaNaoChamaFornecedor() {
        f.iniciar();for(String selecao:List.of("[]","[\"pax-1\",\"pax-1\"]","[\"outro\"]"))assertThrows(RegraDeNegocioException.class,()->f.acao("passageiros",f.json("{\"passageiroKeys\":"+selecao+"}")));
        verify(f.hub,never()).executar(startsWith("divisao/"),any());
    }
}
