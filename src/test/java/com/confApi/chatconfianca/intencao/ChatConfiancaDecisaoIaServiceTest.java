package com.confApi.chatconfianca.intencao;

import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.chatgpt.service.ChatService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatConfiancaDecisaoIaServiceTest {
    private final ChatIntencaoShadowService intencaoService = mock(ChatIntencaoShadowService.class);
    private final ChatService chatService = mock(ChatService.class);
    private ChatIntencaoShadowProperties properties;
    private ChatConfiancaDecisaoIaService service;

    @BeforeEach
    void setUp() {
        properties = new ChatIntencaoShadowProperties();
        properties.setUnifiedDecisionEnabled(true);
        properties.setUnifiedDecisionCanaryEnabled(false);
        service = new ChatConfiancaDecisaoIaService(intencaoService, chatService, properties);
        when(chatService.identificarKeywordOperacionalDeterministica(anyString())).thenReturn(null);
    }

    @Test
    void deveDerivarIntencaoMemoriaAcaoEDepartamentoDaMesmaClassificacao() {
        ChatIntencaoClassificacao classificacao = classificacao(
                "financeiro.faturas", 92);
        ChatIntencaoRuntimeDto.Memoria memoria = memoria(10, "Faturas vencem toda sexta-feira.");
        classificacao.setMemoriasDetalhadas(List.of(memoria));
        classificacao.setMemoriasRecuperadas(List.of(10));
        when(intencaoService.classificar("Quero minhas faturas", 1, "Cuiaba"))
                .thenReturn(classificacao);
        DepartamentoUnidade financeiro = departamento(20L, "Financeiro");

        ChatConfiancaDecisaoIa decisao = service.decidir(
                "Quero minhas faturas", null, List.of(financeiro), 1, "Cuiaba");

        assertTrue(decisao.isAplicada());
        assertEquals("UNIFICADA", decisao.getModo());
        assertEquals("financeiro.faturas", decisao.getIntencao());
        assertEquals("faturas", decisao.getAcao());
        assertEquals(List.of(memoria), decisao.getMemorias());
        assertEquals(financeiro, decisao.getDepartamento());
        assertEquals(List.of("financeiro", "financeiro.faturas"), decisao.getTopicos());
    }

    @Test
    void comandoDeterministicoDevePrevalecerESuprimirMemoriaDeOutraIntencao() {
        ChatIntencaoClassificacao classificacao = classificacao(
                "financeiro.faturas", 90);
        classificacao.setMemoriasDetalhadas(List.of(memoria(11, "Conteudo financeiro")));
        when(intencaoService.classificar("Cancelar reserva ABC123", 1, "Cuiaba"))
                .thenReturn(classificacao);
        when(chatService.identificarKeywordOperacionalDeterministica("Cancelar reserva ABC123"))
                .thenReturn("reserva_aerea_regras");
        DepartamentoUnidade financeiro = departamento(20L, "Financeiro");
        DepartamentoUnidade aereo = departamento(21L, "Aereo e Emissao");

        ChatConfiancaDecisaoIa decisao = service.decidir(
                "Cancelar reserva ABC123", null,
                List.of(financeiro, aereo), 1, "Cuiaba");

        assertTrue(decisao.isAplicada());
        assertEquals("COMANDO_DETERMINISTICO", decisao.getStatus());
        assertEquals("aereo.regra_tarifaria", decisao.getIntencao());
        assertEquals("reserva_aerea_regras", decisao.getAcao());
        assertTrue(decisao.getMemorias().isEmpty());
        assertEquals(aereo, decisao.getDepartamento());
    }

    @Test
    void devePreservarFluxoLegadoQuandoFlagEstaDesligada() {
        properties.setUnifiedDecisionEnabled(false);
        when(intencaoService.classificar("Consultar boleto", 1, "Cuiaba"))
                .thenReturn(classificacao("financeiro.boletos", 95));
        DepartamentoUnidade financeiro = departamento(20L, "Financeiro");

        ChatConfiancaDecisaoIa decisao = service.decidir(
                "Consultar boleto", null, List.of(financeiro), 1, "Cuiaba");

        assertFalse(decisao.isAplicada());
        assertEquals("LEGADO_FALLBACK", decisao.getModo());
        assertEquals("financeiro", decisao.getIntencao());
        assertNull(decisao.getAcao());
        assertTrue(decisao.getMemorias().isEmpty());
        assertEquals(financeiro, decisao.getDepartamento());
    }

    @Test
    void classificacaoDeFerramentaDeveSelecionarUmaUnicaFerramenta() {
        when(intencaoService.classificar("Menor tarifa ida e volta", 1, "Cuiaba"))
                .thenReturn(classificacao("aereo.melhor_tarifa_ida_volta", 94));

        ChatConfiancaDecisaoIa decisao = service.decidir(
                "Menor tarifa ida e volta", null, List.of(), 1, "Cuiaba");

        assertTrue(decisao.isAplicada());
        assertEquals(ChatConfiancaDecisaoIaService.TOOL_MELHORES_TARIFAS_IDA_VOLTA,
                decisao.getFerramenta());
        assertNull(decisao.getAcao());
    }

    @Test
    void departamentoEscolhidoPeloUsuarioDeveTerPrioridade() {
        when(intencaoService.classificar("Consultar faturas", 1, "Cuiaba"))
                .thenReturn(classificacao("financeiro.faturas", 90));
        DepartamentoUnidade financeiro = departamento(20L, "Financeiro");
        DepartamentoUnidade atendimento = departamento(21L, "Atendimento");

        ChatConfiancaDecisaoIa decisao = service.decidir(
                "Consultar faturas", 21L,
                List.of(financeiro, atendimento), 1, "Cuiaba");

        assertEquals(atendimento, decisao.getDepartamento());
        assertEquals(100, decisao.getDepartamentoConfianca());
    }

    @Test
    void canarioDeveAplicarDecisaoUnificadaSomenteParaIntencaoInstitucional() {
        properties.setUnifiedDecisionCanaryEnabled(true);
        properties.setUnifiedDecisionCanaryIntentionPrefixes(List.of("institucional."));
        ChatIntencaoClassificacao classificacao = classificacao(
                "institucional.horario_atendimento", 96);
        ChatIntencaoRuntimeDto.Memoria memoria = memoria(
                7, "O horario de atendimento e de segunda a sexta-feira.");
        classificacao.setMemoriasDetalhadas(List.of(memoria));
        when(intencaoService.classificar("Qual e o horario de atendimento?", 1, "Cuiaba"))
                .thenReturn(classificacao);
        DepartamentoUnidade atendimento = departamento(30L, "Atendimento e Relacionamento");

        ChatConfiancaDecisaoIa decisao = service.decidir(
                "Qual e o horario de atendimento?", null,
                List.of(atendimento), 1, "Cuiaba");

        assertTrue(decisao.isCanarioHabilitado());
        assertTrue(decisao.isCanarioElegivel());
        assertTrue(decisao.isAplicada());
        assertEquals("UNIFICADA", decisao.getModo());
        assertEquals("institucional.horario_atendimento", decisao.getIntencao());
        assertEquals(List.of(memoria), decisao.getMemorias());
        assertEquals(atendimento, decisao.getDepartamento());
    }

    @Test
    void canarioDevePreservarLegadoParaIntencaoNaoInstitucional() {
        properties.setUnifiedDecisionCanaryEnabled(true);
        properties.setUnifiedDecisionCanaryIntentionPrefixes(List.of("institucional."));
        ChatIntencaoClassificacao classificacao = classificacao("financeiro.faturas", 95);
        classificacao.setMemoriasDetalhadas(List.of(memoria(12, "Conteudo financeiro")));
        when(intencaoService.classificar("Quero consultar minhas faturas", 1, "Cuiaba"))
                .thenReturn(classificacao);
        DepartamentoUnidade financeiro = departamento(20L, "Financeiro");

        ChatConfiancaDecisaoIa decisao = service.decidir(
                "Quero consultar minhas faturas", null,
                List.of(financeiro), 1, "Cuiaba");

        assertTrue(decisao.isCanarioHabilitado());
        assertFalse(decisao.isCanarioElegivel());
        assertFalse(decisao.isAplicada());
        assertEquals("LEGADO_FORA_CANARIO", decisao.getModo());
        assertEquals("financeiro", decisao.getIntencao());
        assertNull(decisao.getAcao());
        assertTrue(decisao.getMemorias().isEmpty());
        assertEquals(financeiro, decisao.getDepartamento());
    }

    @Test
    void canarioSemPrefixosDeveFalharFechadoEPreservarLegado() {
        properties.setUnifiedDecisionCanaryEnabled(true);
        properties.setUnifiedDecisionCanaryIntentionPrefixes(List.of());
        when(intencaoService.classificar("Qual e o horario?", 1, "Cuiaba"))
                .thenReturn(classificacao("institucional.horario_atendimento", 95));

        ChatConfiancaDecisaoIa decisao = service.decidir(
                "Qual e o horario?", null, List.of(), 1, "Cuiaba");

        assertFalse(decisao.isCanarioElegivel());
        assertFalse(decisao.isAplicada());
        assertEquals("LEGADO_FORA_CANARIO", decisao.getModo());
    }

    private ChatIntencaoClassificacao classificacao(String codigo, int confianca) {
        ChatIntencaoClassificacao classificacao = ChatIntencaoClassificacao.status("CLASSIFICADA");
        classificacao.setIntencaoId(15L);
        classificacao.setCodigo(codigo);
        classificacao.setNome(codigo);
        classificacao.setScore(new BigDecimal("20.000"));
        classificacao.setConfianca(confianca);
        return classificacao;
    }

    private ChatIntencaoRuntimeDto.Memoria memoria(Integer id, String texto) {
        ChatIntencaoRuntimeDto.Memoria memoria = new ChatIntencaoRuntimeDto.Memoria();
        memoria.setCodgMemoria(id);
        memoria.setTexto(texto);
        memoria.setBase("Cuiaba");
        memoria.setCodgUnidade(1);
        return memoria;
    }

    private DepartamentoUnidade departamento(Long id, String nome) {
        DepartamentoUnidade departamento = new DepartamentoUnidade();
        departamento.setId(id);
        departamento.setNomeExibicao(nome);
        return departamento;
    }
}
