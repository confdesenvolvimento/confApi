package com.confApi.chatgpt.service;

import com.confApi.db.wooba.checkin.dto.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChatCheckinResumoTest {
    private final ZoneId zona = ZoneId.of("America/Cuiaba");
    private final ChatCheckinResumo formatter = new ChatCheckinResumo(zona);

    private CheckinRQ consulta() {
        CheckinRQ rq = new CheckinRQ("AGENCIA-SESSAO", 2);
        rq.setDataForm("22/09/2026"); rq.setDataTo("25/09/2026"); return rq;
    }
    private TrechoCheckin trecho(String data, String hora) {
        TrechoCheckin t = new TrechoCheckin();
        t.setData(data == null ? null : Date.from(LocalDate.parse(data).atStartOfDay(zona).toInstant()));
        t.setHora(hora); t.setDe("CGB"); t.setPara("GRU"); t.setCompanhia("JJ"); t.setVoo("1234"); return t;
    }
    private Checkin72Horas reserva(Integer status, TrechoCheckin... trechos) {
        Checkin72Horas r = new Checkin72Horas(); r.setStatusReserva(status); r.setLocalizadorCompanhia("ABC123");
        r.setTrechosMultiplaConexao(Arrays.asList(trechos)); r.setPassageiro("Passageiro Teste"); return r;
    }
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> itens(Map<String, Object> payload) { return (List<Map<String, Object>>) payload.get("reservaCheckInIA"); }
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> trechos(Map<String, Object> item) { return (List<Map<String, Object>>) item.get("trechosMultiplaConexao"); }

    @Test void janelaDoContratoIncluiHojeEAteTresDiasDepoisSemMesesDistantes() {
        var resultado = formatter.montar(List.of(reserva(2, trecho("2026-09-21", "23:59"),
                trecho("2026-09-22", "00:00"), trecho("2026-09-25", "23:59"),
                trecho("2026-09-26", "00:00"), trecho("2027-01-05", "10:00"))), consulta());
        var voos = trechos(itens(resultado).get(0));
        assertEquals(List.of("22/09/2026", "25/09/2026"), voos.stream().map(v -> v.get("data")).toList());
        String texto = (String) resultado.get("textoResposta");
        assertTrue(texto.contains("22/09/2026 a 25/09/2026 (inclusive)"));
        assertFalse(texto.contains("2027")); assertFalse(texto.matches("(?s).*\\b[0-9]{13}\\b.*"));
    }
    @Test void naoUsaDataDaReservaQuandoTrechosEstaoForaDaJanela() {
        Checkin72Horas r = reserva(2, trecho("2027-01-05", "10:00"));
        r.setData(trecho("2026-09-22", "10:00").getData());
        var resultado = formatter.montar(List.of(r), consulta());
        assertTrue(itens(resultado).isEmpty()); assertEquals("SEM_RESULTADO", resultado.get("statusConsulta"));
    }
    @Test void filtraCanceladasReservadasENulosMesmoComDataValida() {
        assertTrue(itens(formatter.montar(Arrays.asList(null, reserva(3, trecho("2026-09-22", "10:00")),
                reserva(1, trecho("2026-09-22", "10:00")), reserva(null, trecho("2026-09-22", "10:00"))), consulta())).isEmpty());
    }
    @Test void dataAusenteNaoVira1970OuSucessoInventado() {
        var resultado = formatter.montar(List.of(reserva(2, trecho(null, "10:00"))), consulta());
        assertTrue(itens(resultado).isEmpty());
        assertTrue(((String) resultado.get("textoResposta")).contains("sem data de trecho válida"));
        assertFalse(resultado.toString().contains("1970"));
    }
    @Test void consolidaListasEEliminaTrechoDuplicadoSemModificarObjetoDeOrigem() {
        var perto = trecho("2026-09-23", "11:00"); var longe = trecho("2027-01-05", "10:00");
        var r = reserva(2, perto, longe); r.setTrechosIda(List.of(perto));
        var resultado = formatter.montar(List.of(r), consulta());
        assertEquals(1, trechos(itens(resultado).get(0)).size()); assertEquals(2, r.getTrechosMultiplaConexao().size());
    }
    @Test void textoNaoExibeContatoInternoBilheteOuDizQueCheckinEstaAberto() {
        var r = reserva(2, trecho("2026-09-22", "10:00"));
        r.setEmail("interno@example.invalid"); r.setNumeroDoBilhete("BILHETE-INTERNO");
        r.setLinkCheckin("https://example.invalid/checkin");
        var resultado = formatter.montar(List.of(r), consulta());
        assertTrue(resultado.toString().contains("https://example.invalid/checkin"));
        assertFalse(resultado.toString().contains("interno@example.invalid"));
        assertFalse(resultado.toString().contains("BILHETE-INTERNO"));
        assertTrue(((String) resultado.get("textoResposta")).contains("nenhum check-in foi realizado"));
    }
    @Test void rejeitaLinkExecutavelEHtmlNoTexto() {
        var r = reserva(2, trecho("2026-09-22", "10:00"));
        r.setLinkCheckin("javascript:alert(1)"); r.setPassageiro("<script>alert(1)</script>");
        String texto = (String) formatter.montar(List.of(r), consulta()).get("textoResposta");
        assertFalse(texto.contains("javascript:")); assertFalse(texto.contains("<script>"));
    }
    @Test void vaziaExplicaPeriodoSemRecuperarLocalizadorDeOutraAcao() {
        var resultado = formatter.montar(null, consulta());
        assertEquals("SEM_RESULTADO", resultado.get("statusConsulta")); assertTrue(itens(resultado).isEmpty());
        assertTrue(((String) resultado.get("textoResposta")).contains("agência deste atendimento"));
    }
    @Test void ordenaPorDataSemOrdenacaoLexicograficaBrasileira() {
        CheckinRQ rq = consulta(); rq.setDataForm("29/09/2026"); rq.setDataTo("02/10/2026");
        var resultado = formatter.montar(List.of(reserva(2, trecho("2026-10-02", "10:00")),
                reserva(2, trecho("2026-09-29", "10:00"))), rq);
        assertEquals("29/09/2026", itens(resultado).get(0).get("data"));
    }
}
