package com.confApi.chatconfianca.financeiro;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ChatFaturasFiltrosTest {
    private static final LocalDate HOJE = LocalDate.of(2026, 9, 22);

    @ParameterizedTest
    @ValueSource(strings = {"Quero minhas faturas", "boletos", "Histórico financeiro", "PDF da fatura 12345", "faturas pagas"})
    void reconheceSomenteConsultaExplicita(String texto) { assertTrue(ChatFaturasFiltros.pedidoExplicito(texto)); }

    @ParameterizedTest
    @ValueSource(strings = {"Quero falar com atendente sobre faturas", "Qual o contato do financeiro para fatura?",
            "pagar fatura 123", "dar baixa na fatura", "cancelar boleto", "como quitar uma fatura?",
            "qual o limite de crédito?", "qual o telefone do boleto?"})
    void naoSeApropriaDeHumanoContatoOuMutacao(String texto) {
        assertFalse(ChatFaturasFiltros.pedidoExplicito(texto));
        assertFalse(ChatFaturasFiltros.continuacao(texto));
    }

    @ParameterizedTest
    @ValueSource(strings = {"e as pagas?", "setembro de 2026", "por vencimento", "de 01/09/2026 até 30/09/2026",
            "aéreas", "somente abertas", "2025", "PDF", "último mês", "buscar novamente", "e todas?", "últimos 30 dias",
            "como pego ela em pdf?", "onde baixar?", "me mande o pdf"})
    void reconheceContinuacoesCurtas(String texto) { assertTrue(ChatFaturasFiltros.continuacao(texto)); }

    @ParameterizedTest
    @ValueSource(strings = {"quanto vendi esse mês", "quero hotel em setembro", "consultar reserva ABC123",
            "e o limite de crédito?", "bom dia", "sim", "de Brasília para Manaus", "o pagamento foi recebido?"})
    void mudancaDeAssuntoNaoHerdaFiltros(String texto) { assertFalse(ChatFaturasFiltros.continuacao(texto)); }

    @ParameterizedTest
    @CsvSource({"faturas pagas,PAGO", "todas as faturas pagas,PAGO", "histórico de faturas pagas,PAGO",
            "faturas não pagas,ABERTO", "faturas abertas,ABERTO", "faturas quitadas,PAGO",
            "abertas e pagas,AMBOS", "histórico financeiro,AMBOS", "todas as faturas,AMBOS",
            "'não as pagas, só abertas',ABERTO", "'não abertas, só pagas',PAGO"})
    void distingueStatusFinanceiro(String texto, String esperado) { assertEquals(esperado, extrair(texto).get("faturaPagamento")); }

    @ParameterizedTest
    @CsvSource({"faturas aéreas,AEREO", "faturas terrestres,TERRESTRE", "aéreas e terrestres,TODOS", "todos os produtos,TODOS"})
    void usaProdutosReaisDoContrato(String texto, String esperado) { assertEquals(esperado, extrair(texto).get("faturaProduto")); }

    @ParameterizedTest
    @CsvSource({"setembro de 2026,2026-09-01,2026-09-30", "09/2026,2026-09-01,2026-09-30",
            "2026-09,2026-09-01,2026-09-30", "último mês,2026-08-01,2026-08-31", "mês passado,2026-08-01,2026-08-31",
            "esse mês,2026-09-01,2026-09-30", "esse ano,2026-01-01,2026-12-31", "ano passado,2025-01-01,2025-12-31",
            "de 01/09/2026 a 30/09/2026,2026-09-01,2026-09-30", "2026-09-01 até 2026-09-30,2026-09-01,2026-09-30",
            "de 1/9/2026 a 30/9/2026,2026-09-01,2026-09-30", "2025,2025-01-01,2025-12-31",
            "de janeiro a março de 2025,2025-01-01,2025-03-31", "09/2026 a 10/2026,2026-09-01,2026-10-31",
            "últimos 30 dias,2026-08-24,2026-09-22"})
    void interpretaPeriodosDeterministicos(String texto, String inicio, String fim) {
        var p = extrair(texto); assertEquals(inicio, p.get("faturaInicio")); assertEquals(fim, p.get("faturaFim"));
    }

    @Test void consultaPadraoPreservaJanelaDoPortalESemAgencia() {
        var c = ChatFaturasFiltros.validar(Map.of(), HOJE);
        assertNull(c.pergunta()); assertNotNull(c.request()); assertNull(c.request().getEmpfat());
        assertEquals("ABERTO", c.request().getPagamento()); assertEquals("TODOS", c.request().getInvoiceType());
        assertEquals("DATA_VENCIMENTO", c.request().getTipoData());
        assertEquals("22/09/2025", c.request().getDataInicio()); assertEquals("22/10/2026", c.request().getDataFim());
        assertEquals(Boolean.FALSE, c.request().getDisabledAFaturar());
    }

    @Test void pagasEmMesPerguntaTipoDataSemInventarDataPagamento() {
        var c = ChatFaturasFiltros.validar(extrair("faturas pagas em setembro de 2026"), HOJE);
        assertNull(c.request()); assertTrue(c.pergunta().contains("emissao"));
        var p = ChatFaturasFiltros.extrair("vencimento", c.parametros(), HOJE);
        c = ChatFaturasFiltros.validar(p, HOJE);
        assertNull(c.pergunta()); assertEquals("PAGO", c.request().getPagamento());
        assertEquals("DATA_VENCIMENTO", c.request().getTipoData()); assertEquals("01/09/2026", c.request().getDataInicio());
    }

    @Test void dataDePagamentoExplicitaNaoPodeUsarVencimentoAnterior() {
        var p = ChatFaturasFiltros.extrair("pela data de pagamento", filtrosValidos(), HOJE);
        var c = ChatFaturasFiltros.validar(p, HOJE);
        assertNull(c.request()); assertTrue(c.pergunta().contains("nao disponibiliza a data do pagamento"));
    }

    @Test void pagasComNovoPeriodoNaoHerdaVencimentoAnterior() {
        var anterior = filtrosValidos(); anterior.put("faturaTipoData", "DATA_VENCIMENTO");
        var p = ChatFaturasFiltros.extrair("faturas pagas em setembro", anterior, HOJE);
        var c = ChatFaturasFiltros.validar(p, HOJE);
        assertNull(c.request()); assertTrue(c.pergunta().contains("emissao"));
        assertFalse(p.containsKey("faturaTipoData"));
        p = ChatFaturasFiltros.extrair("e as pagas?", anterior, HOJE);
        assertEquals("DATA_VENCIMENTO", p.get("faturaTipoData"));
        assertNotNull(ChatFaturasFiltros.validar(p, HOJE).request());
    }

    @Test void pagamentoEPeriodoExplicitoComTipoPodeConsultarSemNovaPergunta() {
        var p = ChatFaturasFiltros.extrair("faturas pagas emitidas em agosto de 2026", filtrosValidos(), HOJE);
        var c = ChatFaturasFiltros.validar(p, HOJE);
        assertNotNull(c.request()); assertEquals("DATA_EMISSAO", c.request().getTipoData());
    }

    @Test void anoNoPeriodoNaoViraNumeroDeFatura() {
        assertFalse(extrair("faturas de setembro de 2026").containsKey("faturaNumero"));
        assertFalse(extrair("fatura de setembro de 2026").containsKey("faturaNumero"));
        assertFalse(extrair("2026").containsKey("faturaNumero"));
        assertEquals("2026-01-01", extrair("2026").get("faturaInicio"));
    }

    @ParameterizedTest @ValueSource(strings = {"faturas de 01/09 a 30/09", "faturas do primeiro trimestre"})
    void periodoIncompletoOuNaoSuportadoNuncaUsaPadrao(String texto) {
        var c = ChatFaturasFiltros.validar(extrair(texto), HOJE);
        assertNull(c.request()); assertNotNull(c.pergunta());
    }

    @Test void periodoRelativoExecutaDatasPrecisamente() {
        var c = ChatFaturasFiltros.validar(extrair("faturas emitidas nos últimos 30 dias"), HOJE);
        assertNotNull(c.request()); assertEquals("24/08/2026", c.request().getDataInicio());
        assertEquals("22/09/2026", c.request().getDataFim());
    }

    @Test void faturasPagasComVencimentoPassadoNaoSeTornamAbertas() {
        var c = ChatFaturasFiltros.validar(extrair("faturas pagas vencidas em setembro"), HOJE);
        assertNotNull(c.request()); assertEquals("PAGO", c.request().getPagamento());
        assertEquals("DATA_VENCIMENTO", c.request().getTipoData());
    }

    @ParameterizedTest @ValueSource(strings = {"PAGO", "AMBOS"})
    void historicoSemPeriodoPergunta(String pagamento) {
        var c = ChatFaturasFiltros.validar(Map.of("faturaPagamento", pagamento), HOJE);
        assertNull(c.request()); assertTrue(c.pergunta().contains("periodo"));
    }

    @Test void numeroSemPeriodoPerguntaEComPeriodoPreservaFiltro() {
        assertNull(ChatFaturasFiltros.validar(extrair("fatura 12345"), HOJE).request());
        var p = ChatFaturasFiltros.extrair("fatura 12345", filtrosValidos(), HOJE);
        var c = ChatFaturasFiltros.validar(p, HOJE);
        assertEquals(12345, c.request().getInvoiceNumber()); assertEquals("PAGO", c.request().getPagamento());
        assertEquals("01/09/2026", c.request().getDataInicio());
    }

    @Test void naoPropagaIdentidadeOuOutrosParametros() {
        Map<String, String> anterior = new HashMap<>(filtrosValidos());
        anterior.put("empfat", "999"); anterior.put("agencia", "888"); anterior.put("usuario", "1");
        anterior.put("localizador", "ABC123"); anterior.put(null, "invalido");
        var p = ChatFaturasFiltros.extrair("e as abertas", anterior, HOJE);
        var c = ChatFaturasFiltros.validar(p, HOJE);
        assertTrue(ChatFaturasFiltros.PARAMETROS.containsAll(p.keySet())); assertNull(c.request().getEmpfat());
        assertEquals("PAGO", anterior.get("faturaPagamento"));
    }

    @ParameterizedTest
    @CsvSource({"faturaInicio,2026-02-30", "faturaFim,2026-01-01", "faturaPagamento,FECHADO", "faturaProduto,HOTEL",
            "faturaNumero,-1", "faturaNumero,0", "faturaNumero,999999999999", "faturaNumero,abc",
            "faturaDocumento,URL", "faturaTipoData,DATA_PAGAMENTO", "faturaInicio,2024-01-01"})
    void invalidoNuncaMontaRequisicao(String chave, String valor) {
        var p = filtrosValidos(); p.put(chave, valor);
        var c = ChatFaturasFiltros.validar(p, HOJE); assertNull(c.request()); assertNotNull(c.pergunta());
    }

    @Test void umaSoDataPedeComplemento() {
        var p = extrair("faturas emitidas desde 01/09/2026");
        assertNull(ChatFaturasFiltros.validar(p, HOJE).request());
        p = ChatFaturasFiltros.extrair("até 30/09/2026", p, HOJE);
        assertNotNull(ChatFaturasFiltros.validar(p, HOJE).request());
    }

    @Test void dataImpossivelNaoCaiNoPeriodoPadrao() {
        var c = ChatFaturasFiltros.validar(extrair("faturas emitidas de 31/02/2026 até 30/09/2026"), HOJE);
        assertNull(c.request());
    }

    @Test void limites400DiasInclusivos() {
        var p = filtrosValidos(); p.put("faturaInicio", "2025-01-01");
        p.put("faturaFim", LocalDate.of(2025, 1, 1).plusDays(399).toString());
        assertNotNull(ChatFaturasFiltros.validar(p, HOJE).request());
        p.put("faturaFim", LocalDate.of(2025, 1, 1).plusDays(400).toString());
        assertNull(ChatFaturasFiltros.validar(p, HOJE).request());
    }

    @Test void vencidasNaoIncluemDataFutura() {
        var c = ChatFaturasFiltros.validar(extrair("faturas vencidas em setembro de 2026"), HOJE);
        assertEquals("ABERTO", c.request().getPagamento()); assertEquals("DATA_VENCIMENTO", c.request().getTipoData());
        assertEquals("21/09/2026", c.request().getDataFim());
    }

    @ParameterizedTest @CsvSource({"PDF da fatura 123,PDF", "CSV da fatura 123,CSV", "boleto da fatura 123,BOLETO"})
    void documentosSaoApenasPreferenciasSemUrls(String texto, String documento) {
        assertEquals(documento, extrair(texto).get("faturaDocumento"));
    }

    private static Map<String, String> extrair(String texto) { return ChatFaturasFiltros.extrair(texto, Map.of(), HOJE); }
    private static Map<String, String> filtrosValidos() {
        return new HashMap<>(Map.of("faturaPagamento", "PAGO", "faturaTipoData", "DATA_EMISSAO",
                "faturaInicio", "2026-09-01", "faturaFim", "2026-09-30"));
    }
}
