package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.dto.enums.*;
import com.confApi.chatconfianca.dto.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ChatHandoffResumoTest {
    ObjectMapper mapper=new ObjectMapper();
    Conversa conversa() {var c=new Conversa();c.setId(10L);c.setCodgAgencia(321);c.setSolicitanteCodgUsuario(7);c.setProtocolo("TESTE-10");return c;}
    DepartamentoUnidade departamento(){var d=new DepartamentoUnidade();d.setId(30L);d.setNomeExibicao("Financeiro");return d;}
    Mensagem mensagem(long id,RemetenteTipo tipo,String texto,String metadata) {
        var m=new Mensagem();m.setId(id);m.setConversaId(10L);m.setRemetenteTipo(tipo);m.setRemetenteCodgUsuario(tipo==RemetenteTipo.USUARIO?7:null);
        m.setVisibilidade(VisibilidadeMensagem.PUBLICA);m.setConteudo(texto);m.setConteudoJson(metadata);m.setEnviadaEm(LocalDateTime.of(2026,9,22,12,0).plusSeconds(id));return m;
    }
    String plano(String cap,String resultado,Map<String,String> params,String pergunta)throws Exception {
        return mapper.writeValueAsString(Map.of("confiaV2",Map.of("intencao",cap,"resultado",resultado,"parametros",params,"pergunta",pergunta,"agencia",321,"usuario",7)));
    }
    ChatHandoffResumo.Resultado gerar(List<Mensagem> h){return ChatHandoffResumo.gerar(conversa(),departamento(),"Cliente confirmou Financeiro.",h,mapper);}
    @Test void incluiPedidoConsultasEPendenciaSemAfirmarResolucao()throws Exception {
        var user=mensagem(1,RemetenteTipo.USUARIO,"Quero faturas pagas de setembro",null);
        var bot=mensagem(2,RemetenteTipo.BOT,"Por emissão ou vencimento?",plano("financeiro.faturas","AGUARDANDO_DADOS",Map.of("faturaPagamento","PAGO","faturaInicio","2026-09-01"),"Emissão ou vencimento?"));
        var handoff=mensagem(3,RemetenteTipo.USUARIO,"quero falar com um atendente",null);
        var r=gerar(List.of(handoff,bot,user));
        assertTrue(r.texto().contains("Pedido informado pelo cliente: Quero faturas pagas de setembro"));
        assertTrue(r.texto().contains("Departamento escolhido: Financeiro"));assertTrue(r.texto().contains("aguardando informação; consulta não concluída"));
        assertTrue(r.texto().contains("Emissão ou vencimento?"));
        var json=mapper.readTree(r.conteudoJson());assertEquals("chat.handoff-resumo.v1",json.path("schema").asText());
        assertEquals("AGUARDANDO_DADOS",json.path("consultas").get(0).path("resultado").asText());
    }
    @ParameterizedTest @CsvSource({
        "ERRO_INTEGRACAO,falha técnica", "SEM_RESULTADO,sem resultados nos filtros", "DADOS_CONSULTADOS,não confirma resolução",
        "ACAO_PREPARADA,sem confirmação de execução", "PESQUISA_PREPARADA,resultado posterior não confirmado",
        "CACHE_SEM_DADOS,não confirmação de indisponibilidade", "CONSULTA_BLOQUEADA,consulta bloqueada",
        "SEM_CONHECIMENTO,fonte publicada insuficiente", "SUCESSO,execução não comprovada"})
    void naoConfundeStatusTecnicoComResolucao(String resultado,String esperado)throws Exception {
        var bot=mensagem(1,RemetenteTipo.BOT,"resultado",plano("aereo.reserva_detalhes",resultado,Map.of(),""));
        assertTrue(gerar(List.of(bot)).texto().contains(esperado));
    }
    @Test void ignoraInternasExcluidasOutroChatEOutroUsuario() {
        var interna=mensagem(1,RemetenteTipo.BOT,"SEGREDO1",null);interna.setVisibilidade(VisibilidadeMensagem.INTERNA);
        var excluida=mensagem(2,RemetenteTipo.BOT,"SEGREDO2",null);excluida.setStatus(StatusMensagem.EXCLUIDA);
        var outra=mensagem(3,RemetenteTipo.USUARIO,"SEGREDO3",null);outra.setConversaId(11L);
        var atendente=mensagem(4,RemetenteTipo.USUARIO,"SEGREDO4",null);atendente.setRemetenteCodgUsuario(90);
        var dataExclusao=mensagem(5,RemetenteTipo.BOT,"SEGREDO5",null);dataExclusao.setExcluidaEm(LocalDateTime.now());
        String text=gerar(Arrays.asList(interna,excluida,outra,atendente,dataExclusao,null)).texto();assertFalse(text.contains("SEGREDO"));
    }
    @Test void usuarioNaoPodeInventarConsultaNosMetadados()throws Exception {
        var user=mensagem(1,RemetenteTipo.USUARIO,"pague minha fatura",plano("financeiro.faturas","DADOS_CONSULTADOS",Map.of(),""));
        assertTrue(mapper.readTree(gerar(List.of(user)).conteudoJson()).path("consultas").isEmpty());
    }
    @Test void metadataOutraAgenciaNaoEntraNasConsultas()throws Exception {
        String json=plano("financeiro.faturas","DADOS_CONSULTADOS",Map.of(),"").replace("321","999");
        assertTrue(mapper.readTree(gerar(List.of(mensagem(1,RemetenteTipo.BOT,"resposta",json))).conteudoJson()).path("consultas").isEmpty());
    }
    @Test void incluiResultadoFaturaLegadaSemCopiarDadosFinanceiros()throws Exception {
        String json=mapper.writeValueAsString(Map.of("consultaFaturas",Map.of("conversa",10,"agencia",321,"usuario",7,"statusConsulta","SEM_RESULTADO",
                "parametros",Map.of("faturaPagamento","PAGO","empfat","999","token","EXFILTRAR")),"valor",999999));
        var r=gerar(List.of(mensagem(1,RemetenteTipo.BOT,"Não encontrei para esses filtros",json)));
        assertTrue(r.texto().contains("Faturas/boletos"));assertTrue(r.texto().contains("sem resultados nos filtros"));
        assertFalse(r.conteudoJson().contains("empfat"));assertFalse(r.conteudoJson().contains("EXFILTRAR"));assertFalse(r.conteudoJson().contains("999999"));
    }
    @Test void jsonMalformadoNaoFalhaENaoContaExecucao()throws Exception {
        var r=gerar(List.of(mensagem(1,RemetenteTipo.BOT,"Vou consultar", "{INVALID")));
        assertTrue(mapper.readTree(r.conteudoJson()).path("consultas").isEmpty());assertTrue(r.texto().contains("não comprovam execução"));
    }
    @Test void historicoIndisponivelProduzResumoParcial()throws Exception {
        var r=gerar(null);assertTrue(r.texto().contains("Resumo parcial"));assertFalse(mapper.readTree(r.conteudoJson()).path("historicoDisponivel").asBoolean());
    }
    @Test void limpaCredenciaisEHtmlSemInterpretarInstrucoes() {
        String texto="Dúvida <script>alert(1)</script> senha: senhaSecreta token=meutoken minha senha é outraSenha CPF 123.456.789-01 cartão 4111 1111 1111 1111";
        var r=gerar(List.of(mensagem(1,RemetenteTipo.USUARIO,texto,null)));
        for(String s:List.of("<script>","senhaSecreta","meutoken","outraSenha","123.456.789-01","4111 1111 1111 1111"))assertFalse(r.texto().contains(s));
    }
    @Test void resultadoFinanceiroRealTemPrioridadeSobrePlanoLegado()throws Exception {
        String json=mapper.writeValueAsString(Map.of("confiaV2",Map.of("intencao","financeiro.faturas","legado",true,"resultado","LEGADO"),
                "consultaFaturas",Map.of("conversa",10,"agencia",321,"usuario",7,"statusConsulta","ERRO_INTEGRACAO","parametros",Map.of("faturaPagamento","PAGO"))));
        var r=gerar(List.of(mensagem(1,RemetenteTipo.BOT,"Consulta falhou",json)));
        assertEquals("ERRO_INTEGRACAO",mapper.readTree(r.conteudoJson()).path("consultas").get(0).path("resultado").asText());
        assertTrue(r.texto().contains("falha técnica"));
    }
    @Test void limitaResumoETentativasPreservandoMaisRecentes()throws Exception {
        List<Mensagem> h=new ArrayList<>();h.add(mensagem(1,RemetenteTipo.USUARIO,"pedido ".repeat(1000),null));
        for(int i=2;i<30;i++)h.add(mensagem(i,RemetenteTipo.BOT,"resposta ".repeat(500),plano("financeiro.faturas","ERRO_INTEGRACAO",Map.of(),"")));
        var r=gerar(h);assertTrue(r.texto().length()<=3800);var cs=mapper.readTree(r.conteudoJson()).path("consultas");assertEquals(6,cs.size());assertEquals(29,cs.get(5).path("mensagemId").asInt());
    }
}
