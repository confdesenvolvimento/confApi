package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.dto.enums.RemetenteTipo;
import com.confApi.chatconfianca.dto.model.Mensagem;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChatContinuidadeOperacionalTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test void localizadorAtualCompletaPedidoDoSeletorSemReutilizarCodigoAntigo() throws Exception {
        Mensagem m = bot(Map.of("actions", List.of(Map.of("code", "selecionar_reserva_remarcacao", "localizador", "OLD123"))));
        assertEquals("Simular remarcacao da reserva ABC123", completar("abc123", m));
        assertEquals("Quais meus check-ins?", completar("Quais meus check-ins?", m));
        assertEquals("voo G3 1758", completar("voo G3 1758", m));
    }
    @Test void localizadorSemContextoDeRemarcacaoNaoRecebeIntencaoInventada() throws Exception {
        assertEquals("ABC123", completar("ABC123", bot(Map.of("actions", List.of(Map.of("code", "abrir_reserva"))))));
        assertEquals("ABC123", ChatContinuidadeOperacional.completarLocalizador("ABC123", List.of(), mapper));
    }
    @Test void seletorAntigoNaoEReativado() throws Exception {
        Mensagem m = bot(Map.of("actions", List.of(Map.of("code", "selecionar_reserva_remarcacao"))));
        m.setEnviadaEm(LocalDateTime.now().minusMinutes(31));
        assertEquals("ABC123", completar("ABC123", m));
    }
    @Test void trocaDeAssuntoNaoViraLocalizadorMesmoAposSeletor() throws Exception {
        Mensagem m = bot(Map.of("actions", List.of(Map.of("code", "selecionar_reserva_remarcacao"))));
        for (String texto : List.of("financeiro", "FINANCEIRO", "limites", "boletos", "hotel", "HOTEL", "cancelar", "Obrigado", "duvidas", "emergencia")) {
            assertEquals(texto, completar(texto, m));
        }
        assertEquals("Simular remarcacao da reserva QWERTY", completar("QWERTY", m));
        assertEquals("novotema", completar("novotema", m));
    }
    @Test void dataCurtaDuranteRemarcacaoNaoDeveVirarPerguntaBsp() throws Exception {
        String resposta = orientar("20 set 2026", card("AGUARDANDO_CRITERIOS", 10L));
        assertTrue(resposta.contains("Nova data"));
        assertTrue(resposta.contains("nao alterou a reserva"));
    }
    @Test void numeroDeVooNaoDeveVirarLocalizador() throws Exception {
        assertTrue(orientar("voo g3 1758", card("AGUARDANDO_OPCAO", 10L)).contains("nao e um localizador"));
    }
    @Test void cardDeOutraConversaExpiradoOuConcluidoNaoDeveInterferir() throws Exception {
        assertNull(orientar("20 set 2026", card("AGUARDANDO_CRITERIOS", 11L)));
        assertNull(orientar("20 set 2026", card("ENCAMINHADO", 10L)));
        Mensagem expirado = bot(Map.of("remarcacao", Map.of("conversaId", 10L,
                "status", "AGUARDANDO_CRITERIOS", "expiraEm", LocalDateTime.now().minusMinutes(1).toString())));
        assertNull(orientar("20 set 2026", expirado));
        assertNull(orientar("Qual o contato do financeiro?", card("AGUARDANDO_CRITERIOS", 10L)));
    }
    @Test void respostaDeOutroAssuntoImpedeRessuscitarCardAnterior() throws Exception {
        Mensagem card = card("AGUARDANDO_CRITERIOS", 10L);
        Mensagem outro = bot(Map.of("intencao", "financeiro.bsp"));
        assertNull(ChatContinuidadeOperacional.orientarEtapa("20 set 2026", 10L, List.of(card, outro), mapper));
    }
    @Test void orientacaoPodeSerRepetidaSemPerderCard() throws Exception {
        Mensagem card = card("AGUARDANDO_CRITERIOS", 10L);
        Mensagem orientacao = bot(Map.of());
        orientacao.setConteudo(orientar("20 set 2026", card));
        assertNotNull(ChatContinuidadeOperacional.orientarEtapa("21 set 2026", 10L,
                List.of(card, orientacao), mapper));
    }
    @Test void jsonNuloOuInvalidoNaoDerrubaConversa() {
        Mensagem m = new Mensagem(); m.setRemetenteTipo(RemetenteTipo.BOT);
        m.setEnviadaEm(LocalDateTime.now().minusSeconds(1));
        assertNull(orientar("20 set 2026", m));
        m.setConteudoJson("{invalido");
        assertNull(orientar("20 set 2026", m));
        assertEquals("ABC123", completar("ABC123", m));
    }
    @Test void promessaSemEntregaNaoPedeAguardarConsultaInexistente() {
        assertTrue(ChatContinuidadeOperacional.concluirSemPromessa("Vou buscar os pacotes. Aguarde um momento.")
                .contains("Nao ha uma consulta em andamento"));
        assertEquals("Encontrei 3 opcoes.", ChatContinuidadeOperacional.concluirSemPromessa("Encontrei 3 opcoes."));
        assertEquals("Para pesquisar, informe a origem.", ChatContinuidadeOperacional.concluirSemPromessa("Para pesquisar, informe a origem."));
        assertNull(ChatContinuidadeOperacional.concluirSemPromessa(null));
        String pesquisa = "{\"status\":\"OK\",\"mensagem\":\"Vou buscar. Aguarde um momento.\"}";
        assertEquals(pesquisa, ChatContinuidadeOperacional.concluirSemPromessa(pesquisa));
    }
    private String completar(String texto, Mensagem m) { return ChatContinuidadeOperacional.completarLocalizador(texto, List.of(m), mapper); }
    private String orientar(String texto, Mensagem m) { return ChatContinuidadeOperacional.orientarEtapa(texto, 10L, List.of(m), mapper); }
    private Mensagem card(String status, long conversa) throws Exception {
        return bot(Map.of("remarcacao", Map.of("conversaId", conversa, "status", status,
                "expiraEm", LocalDateTime.now().plusMinutes(15).toString())));
    }
    private Mensagem bot(Object json) throws Exception {
        Mensagem m = new Mensagem(); m.setRemetenteTipo(RemetenteTipo.BOT);
        m.setEnviadaEm(LocalDateTime.now().minusSeconds(1)); m.setConteudoJson(mapper.writeValueAsString(json)); return m;
    }
}
