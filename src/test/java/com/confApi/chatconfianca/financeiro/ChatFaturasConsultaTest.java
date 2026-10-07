package com.confApi.chatconfianca.financeiro;

import com.confApi.chatgpt.dto.ChatResponseDTO;
import com.confApi.chatgpt.dto.ConversationRequestDTO;
import com.confApi.db.confManager.faturas.FaturasService;
import com.confApi.db.confManager.faturas.dto.FaturaSicaRQ;
import com.confApi.db.confManager.faturas.dto.FaturaSicaRS;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatFaturasConsultaTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private FaturasService financeiro;
    private ChatFaturasConsulta consulta;

    @BeforeEach void preparar() {
        financeiro = mock(FaturasService.class);
        consulta = new ChatFaturasConsulta(financeiro, mapper);
    }

    private ConversationRequestDTO request() { return request("321", 51L, 71L); }
    private ConversationRequestDTO request(String erp, Long agencia, Long usuario) {
        return new ConversationRequestDTO("confia", "unidade", erp, agencia, usuario,
                "Quero as faturas pagas de agosto de 2026 pela data de emissao", List.of(), null, false, List.of());
    }
    private Map<String, String> filtros() {
        Map<String, String> filtros = new LinkedHashMap<>();
        filtros.put("faturaPagamento", "PAGO");
        filtros.put("faturaInicio", "2026-08-01");
        filtros.put("faturaFim", "2026-08-31");
        filtros.put("faturaTipoData", "DATA_EMISSAO");
        return filtros;
    }
    private FaturaSicaRS fatura(int numero, String paga) {
        FaturaSicaRS fatura = new FaturaSicaRS();
        fatura.setNumfat(numero); fatura.setEmpfat(321); fatura.setPago(paga);
        fatura.setValor(1200.50); fatura.setInvoiceType("Aéreo"); fatura.setSituacao("Faturada");
        fatura.setDataFatura("10/08/2026"); fatura.setDataVen("20/09/2026");
        fatura.setCodest("INTERNO-NAO-PUBLICAR"); fatura.setContbanco(999999);
        return fatura;
    }
    private JsonNode dados(ChatResponseDTO resposta) throws Exception { return mapper.readTree(resposta.history().get(0).content()); }
    private String status(ChatResponseDTO resposta) throws Exception { return dados(resposta).path("statusConsulta").asText(); }

    @Test void consultaPagasComPeriodoEIdentidadeSomenteDaSessao() throws Exception {
        when(financeiro.faturaSicaEstrita(any())).thenReturn(List.of(fatura(8801, "Sim")));
        Map<String, String> filtros = filtros();
        filtros.put("empfat", "987"); filtros.put("agencia", "987"); filtros.put("usuario", "987");
        ChatResponseDTO resposta = consulta.responder(request("000321", 51L, 71L), filtros, false);
        assertEquals("DADOS_CONSULTADOS", status(resposta));
        ArgumentCaptor<FaturaSicaRQ> captor = ArgumentCaptor.forClass(FaturaSicaRQ.class);
        verify(financeiro).faturaSicaEstrita(captor.capture());
        assertEquals("000321", captor.getValue().getEmpfat());
        assertEquals("PAGO", captor.getValue().getPagamento());
        assertEquals("DATA_EMISSAO", captor.getValue().getTipoData());
        assertEquals("01/08/2026", captor.getValue().getDataInicio());
        assertEquals("31/08/2026", captor.getValue().getDataFim());
        assertTrue(resposta.content().contains("Paga"));
        assertTrue(resposta.content().contains("\n\nFatura 8801 — Paga\nProduto: Aéreo | Valor: R$"));
        assertTrue(resposta.content().contains("\nEmissão: 10/08/2026\nVencimento: 20/09/2026"));
        assertFalse(resposta.content().contains("| Fatura |"));
        assertFalse(resposta.content().contains("| --- |"));
        assertTrue(resposta.content().contains("1.200,50"));
        assertTrue(resposta.content().contains("01/08/2026 a 31/08/2026"));
        assertEquals(List.of("faturas"), resposta.keywords());
        assertTrue(resposta.actions().isEmpty()); assertTrue(resposta.toolCalls().isEmpty());
        JsonNode dados = dados(resposta);
        assertEquals("chat.faturas.v1", dados.path("schema").asText());
        assertEquals(51, dados.path("contexto").path("agencia").asLong());
        assertEquals(71, dados.path("contexto").path("usuario").asLong());
        assertTrue(dados.path("contexto").path("atualizadoEm").asLong() > 0);
        assertFalse(resposta.content().contains("000321"));
        assertFalse(resposta.history().get(0).content().contains("empfat"));
        assertFalse(resposta.history().get(0).content().contains("INTERNO-NAO-PUBLICAR"));
        assertFalse(resposta.history().get(0).content().contains("999999"));
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"0", "-1", "abc", "321 OR 1=1", "321 ", "2147483648"})
    void identidadeInvalidaBloqueiaSemConsulta(String erp) throws Exception {
        assertEquals("CONSULTA_BLOQUEADA", status(consulta.responder(request(erp, 51L, 71L), filtros(), false)));
        verifyNoInteractions(financeiro);
    }

    @Test void sessaoAusenteOuInvalidaNaoConsulta() throws Exception {
        for (ConversationRequestDTO req : Arrays.asList(null, request("321", null, 71L), request("321", 0L, 71L),
                request("321", 51L, null), request("321", 51L, -1L))) {
            assertEquals("CONSULTA_BLOQUEADA", status(consulta.responder(req, filtros(), false)));
        }
        verifyNoInteractions(financeiro);
    }

    @Test void faltaPeriodoPerguntaAntesDeChamarIntegracao() throws Exception {
        ChatResponseDTO resposta = consulta.responder(request(), Map.of("faturaPagamento", "PAGO"), false);
        assertEquals("AGUARDANDO_DADOS", status(resposta));
        assertTrue(resposta.content().contains("periodo"));
        assertEquals("PAGO", dados(resposta).path("contexto").path("parametros").path("faturaPagamento").asText());
        verifyNoInteractions(financeiro);
    }

    @Test void faltaTipoDataPerguntaAntesDeChamarIntegracao() throws Exception {
        Map<String, String> filtros = filtros(); filtros.remove("faturaTipoData");
        assertEquals("AGUARDANDO_DADOS", status(consulta.responder(request(), filtros, false)));
        verifyNoInteractions(financeiro);
    }

    @Test void listaVaziaRealEhSemResultadoComFiltros() throws Exception {
        when(financeiro.faturaSicaEstrita(any())).thenReturn(List.of());
        ChatResponseDTO resposta = consulta.responder(request(), filtros(), false);
        assertEquals("SEM_RESULTADO", status(resposta));
        assertTrue(resposta.content().contains("consulta foi concluída"));
        assertTrue(resposta.content().contains("pagas"));
        assertTrue(dados(resposta).path("faturas").isEmpty());
    }

    @Test void falhaTecnicaNaoDizQueNaoExistemFaturasNemExibePayload() throws Exception {
        when(financeiro.faturaSicaEstrita(any())).thenThrow(new FaturasService.ConsultaFaturasException("SECRET-TOKEN-ERP-123"));
        ChatResponseDTO resposta = consulta.responder(request(), filtros(), false);
        assertEquals("ERRO_INTEGRACAO", status(resposta));
        assertTrue(resposta.content().contains("não significa que não existam"));
        assertFalse(resposta.content().contains("SECRET"));
        assertFalse(resposta.history().get(0).content().contains("SECRET"));
    }

    @Test void listaNulaNaoEhSemResultado() throws Exception {
        when(financeiro.faturaSicaEstrita(any())).thenReturn(null);
        assertEquals("ERRO_INTEGRACAO", status(consulta.responder(request(), filtros(), false)));
    }

    @Test void qualquerRegistroDeOutraAgenciaBloqueiaTodaRespostaInclusiveDepoisDoLimite() throws Exception {
        List<FaturaSicaRS> lista = new ArrayList<>();
        for (int i = 1; i <= 20; i++) lista.add(fatura(8800 + i, "Sim"));
        FaturaSicaRS outra = fatura(9901, "Sim"); outra.setEmpfat(987);
        lista.add(outra);
        when(financeiro.faturaSicaEstrita(any())).thenReturn(lista);
        ChatResponseDTO resposta = consulta.responder(request(), filtros(), false);
        assertEquals("CONSULTA_BLOQUEADA", status(resposta));
        assertFalse(resposta.content().contains("8801")); assertFalse(resposta.content().contains("9901"));
        assertTrue(dados(resposta).path("faturas").isEmpty());
    }

    @Test void agenciaAusenteOuRegistroNuloBloqueiaTodaResposta() throws Exception {
        FaturaSicaRS semAgencia = fatura(8801, "Sim"); semAgencia.setEmpfat(0);
        when(financeiro.faturaSicaEstrita(any())).thenReturn(List.of(semAgencia));
        assertEquals("CONSULTA_BLOQUEADA", status(consulta.responder(request(), filtros(), false)));
        when(financeiro.faturaSicaEstrita(any())).thenReturn(Arrays.asList(fatura(8801, "Sim"), null));
        assertEquals("CONSULTA_BLOQUEADA", status(consulta.responder(request(), filtros(), false)));
    }

    @Test void retornoForaDoStatusSolicitadoFalhaSemFalsaAusencia() throws Exception {
        when(financeiro.faturaSicaEstrita(any())).thenReturn(List.of(fatura(8801, "Não")));
        assertEquals("ERRO_INTEGRACAO", status(consulta.responder(request(), filtros(), false)));
    }

    @Test void retornoForaDoPeriodoFalhaSemFalsaAusencia() throws Exception {
        FaturaSicaRS fatura = fatura(8801, "Sim"); fatura.setDataFatura("01/09/2026");
        when(financeiro.faturaSicaEstrita(any())).thenReturn(List.of(fatura));
        assertEquals("ERRO_INTEGRACAO", status(consulta.responder(request(), filtros(), false)));
    }

    @Test void retornoDeOutroProdutoFalhaSemFalsaAusencia() throws Exception {
        when(financeiro.faturaSicaEstrita(any())).thenReturn(List.of(fatura(8801, "Sim")));
        Map<String, String> filtros = filtros(); filtros.put("faturaProduto", "TERRESTRE");
        assertEquals("ERRO_INTEGRACAO", status(consulta.responder(request(), filtros, false)));
    }

    @Test void retornoDeOutroNumeroFalhaSemFalsaAusencia() throws Exception {
        when(financeiro.faturaSicaEstrita(any())).thenReturn(List.of(fatura(8801, "Sim")));
        Map<String, String> filtros = filtros(); filtros.put("faturaNumero", "7777");
        assertEquals("ERRO_INTEGRACAO", status(consulta.responder(request(), filtros, false)));
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"32/08/2026", "31/02/2026", "<script>"})
    void dataNecessariaAusenteOuInvalidaFalha(String data) throws Exception {
        FaturaSicaRS fatura = fatura(8801, "Sim"); fatura.setDataFatura(data);
        when(financeiro.faturaSicaEstrita(any())).thenReturn(List.of(fatura));
        assertEquals("ERRO_INTEGRACAO", status(consulta.responder(request(), filtros(), false)));
    }

    @Test void valorInvalidoFalha() throws Exception {
        FaturaSicaRS fatura = fatura(8801, "Sim"); fatura.setValor(Double.NaN);
        when(financeiro.faturaSicaEstrita(any())).thenReturn(List.of(fatura));
        assertEquals("ERRO_INTEGRACAO", status(consulta.responder(request(), filtros(), false)));
    }

    @Test void pagamentoDesconhecidoNaoEhAssumidoComoAberto() throws Exception {
        FaturaSicaRS fatura = fatura(8801, null);
        when(financeiro.faturaSicaEstrita(any())).thenReturn(List.of(fatura));
        Map<String, String> filtros = filtros(); filtros.put("faturaPagamento", "ABERTO");
        assertEquals("ERRO_INTEGRACAO", status(consulta.responder(request(), filtros, false)));
    }

    @Test void diferenciaSituacoesSemSomarComoVendas() throws Exception {
        FaturaSicaRS paga = fatura(8801, "Sim"), aberta = fatura(8802, "Não"), cancelada = fatura(8803, "Não"),
                credito = fatura(8804, "Não"), pendente = fatura(0, "Não");
        aberta.setFatVenc("Sim"); cancelada.setCancelado(true); credito.setSituacao("Faturada Crédito");
        pendente.setSituacao("À Faturar");
        when(financeiro.faturaSicaEstrita(any())).thenReturn(List.of(paga, aberta, cancelada, credito, pendente));
        Map<String, String> filtros = filtros(); filtros.put("faturaPagamento", "AMBOS");
        ChatResponseDTO resposta = consulta.responder(request(), filtros, false);
        assertEquals("DADOS_CONSULTADOS", status(resposta));
        for (String situacao : List.of("Paga", "Em aberto (vencida)", "Cancelada", "Crédito", "À faturar (não emitida)", "Ainda não emitida")) {
            assertTrue(resposta.content().contains(situacao), situacao);
        }
        assertTrue(resposta.content().contains("não representam o total de vendas"));
    }

    @Test void boletoExcluiCreditoAFaturarCanceladaSemConfundirComSemResultado() throws Exception {
        FaturaSicaRS credito = fatura(8804, "Não"), pendente = fatura(0, "Não"), cancelada = fatura(8803, "Não");
        credito.setSituacao("Faturada Crédito"); pendente.setSituacao("À Faturar"); cancelada.setCancelado(true);
        when(financeiro.faturaSicaEstrita(any())).thenReturn(List.of(credito, pendente, cancelada));
        Map<String, String> filtros = filtros(); filtros.put("faturaPagamento", "ABERTO");
        ChatResponseDTO resposta = consulta.responder(request(), filtros, true);
        assertEquals("DADOS_CONSULTADOS", status(resposta));
        assertEquals(3, dados(resposta).path("excluidasDaListaBoletos").asInt());
        assertEquals(3, dados(resposta).path("totalRetornado").asInt());
        assertTrue(dados(resposta).path("faturas").isEmpty());
        assertTrue(resposta.content().contains("3 registro(s)"));
        assertFalse(resposta.content().contains("não encontrou faturas"));
        assertEquals(List.of("boletos"), resposta.keywords());
        assertTrue(dados(resposta).path("contexto").path("boletos").asBoolean());
    }

    @ParameterizedTest @ValueSource(strings = {"PDF", "CSV", "BOLETO"})
    void documentosSomenteOrientamTelaAutenticadaSemPrometerArquivoOuComprovante(String documento) throws Exception {
        when(financeiro.faturaSicaEstrita(any())).thenReturn(List.of(fatura(8801, "Sim")));
        Map<String, String> filtros = filtros(); filtros.put("faturaDocumento", documento);
        ChatResponseDTO resposta = consulta.responder(request(), filtros, false);
        assertEquals("DADOS_CONSULTADOS", status(resposta));
        assertTrue(resposta.content().contains("sessão autenticada"));
        assertTrue(resposta.content().contains("não gerou um arquivo nem um comprovante"));
        assertFalse(resposta.content().contains("http"));
        verify(financeiro, never()).downloadPdfFatura(any());
        verify(financeiro, never()).downloadCsvFatura(any());
        verify(financeiro, never()).downloadPdfFaturaBoleto(any());
    }

    @Test void limitaLinhasEHistoryMasInformaQuantidadeRetornada() throws Exception {
        List<FaturaSicaRS> lista = new ArrayList<>();
        for (int i = 1; i <= 23; i++) lista.add(fatura(8800 + i, "Sim"));
        when(financeiro.faturaSicaEstrita(any())).thenReturn(lista);
        ChatResponseDTO resposta = consulta.responder(request(), filtros(), false);
        assertEquals("DADOS_CONSULTADOS", status(resposta));
        assertEquals(20, dados(resposta).path("faturas").size());
        assertEquals(23, dados(resposta).path("totalRetornado").asInt());
        assertTrue(resposta.content().contains("primeiros 20 registros de 23"));
        assertTrue(resposta.content().contains("\n\nFatura 8802 — Paga\n"));
        assertFalse(resposta.content().contains("8821"));
    }

    @Test void textosLivresDaIntegracaoNaoSaoPropagados() throws Exception {
        FaturaSicaRS fatura = fatura(8801, "Sim");
        fatura.setSituacao("<script>alert('segredo')</script>");
        fatura.setInvoiceType("http://invasor.invalid/token");
        when(financeiro.faturaSicaEstrita(any())).thenReturn(List.of(fatura));
        ChatResponseDTO resposta = consulta.responder(request(), filtros(), false);
        assertEquals("DADOS_CONSULTADOS", status(resposta));
        assertFalse(resposta.content().contains("script"));
        assertFalse(resposta.history().get(0).content().contains("invasor"));
        assertTrue(resposta.content().contains("Não informado"));
    }

    @Test void consultaPorVencimentoUsaDataCorretaEDataIsoViraPtBr() throws Exception {
        FaturaSicaRS fatura = fatura(8801, "Sim"); fatura.setDataFatura("2026-07-30"); fatura.setDataVen("2026-08-10");
        when(financeiro.faturaSicaEstrita(any())).thenReturn(List.of(fatura));
        Map<String, String> filtros = filtros(); filtros.put("faturaTipoData", "DATA_VENCIMENTO");
        ChatResponseDTO resposta = consulta.responder(request(), filtros, false);
        assertEquals("DADOS_CONSULTADOS", status(resposta));
        assertTrue(resposta.content().contains("30/07/2026"));
        assertTrue(resposta.content().contains("10/08/2026"));
    }

    @Test void aFaturarSemEmissaoPodeAparecerPorVencimentoSemSerChamadaDeEmitida() throws Exception {
        FaturaSicaRS fatura = fatura(0, "Não"); fatura.setSituacao("À Faturar");
        fatura.setDataFatura(null); fatura.setDataVen("10/08/2026");
        when(financeiro.faturaSicaEstrita(any())).thenReturn(List.of(fatura));
        Map<String, String> filtros = filtros(); filtros.put("faturaTipoData", "DATA_VENCIMENTO");
        filtros.put("faturaPagamento", "ABERTO");
        ChatResponseDTO resposta = consulta.responder(request(), filtros, false);
        assertEquals("DADOS_CONSULTADOS", status(resposta));
        assertTrue(resposta.content().contains("À faturar (não emitida)"));
        assertTrue(resposta.content().contains("Ainda não emitida"));
        assertTrue(resposta.content().contains("Não informada"));
    }

    @Test void mesmoFiltroNaoReaproveitaResultadoFinanceiroEntreChamadas() throws Exception {
        when(financeiro.faturaSicaEstrita(any())).thenReturn(List.of(fatura(8801, "Sim")), List.of());
        assertEquals("DADOS_CONSULTADOS", status(consulta.responder(request(), filtros(), false)));
        assertEquals("SEM_RESULTADO", status(consulta.responder(request(), filtros(), false)));
        verify(financeiro, times(2)).faturaSicaEstrita(any());
    }
}
