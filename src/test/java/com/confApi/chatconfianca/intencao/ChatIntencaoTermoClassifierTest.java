package com.confApi.chatconfianca.intencao;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatIntencaoTermoClassifierTest {

    private final ChatIntencaoTermoClassifier classifier = new ChatIntencaoTermoClassifier();

    @Test
    void classificaCalendarioBspPorExpressaoPonderada() {
        ChatIntencaoRuntimeDto calendario = perfil(
                "financeiro.calendario_bsp",
                termo("calendario bsp", "15.000", "POSITIVA"),
                termo("bsp", "10.000", "POSITIVA"));

        ChatIntencaoClassificacao resultado = classifier.classificar(
                "Quero consultar o calendário BSP deste mês.",
                List.of(calendario),
                new BigDecimal("8.000"),
                new BigDecimal("2.000"));

        assertThat(resultado.getStatus()).isEqualTo("CLASSIFICADA");
        assertThat(resultado.getCodigo()).isEqualTo("financeiro.calendario_bsp");
        assertThat(resultado.getScore()).isEqualByComparingTo("25.000");
        assertThat(resultado.getConfianca()).isGreaterThanOrEqualTo(90);
    }

    @Test
    void marcaComoAmbiguaQuandoCandidatosPossuemPontuacaoProxima() {
        ChatIntencaoRuntimeDto detalhes = perfil(
                "aereo.reserva_detalhes", termo("reserva", "10.000", "POSITIVA"));
        ChatIntencaoRuntimeDto regras = perfil(
                "aereo.regra_tarifaria", termo("reserva", "10.000", "POSITIVA"));

        ChatIntencaoClassificacao resultado = classifier.classificar(
                "Preciso de ajuda com uma reserva.",
                List.of(detalhes, regras),
                new BigDecimal("8.000"),
                new BigDecimal("2.000"));

        assertThat(resultado.getStatus()).isEqualTo("AMBIGUA");
        assertThat(resultado.getScore()).isEqualByComparingTo(resultado.getSegundoScore());
    }

    @Test
    void termoNegativoPodeCancelarUmaEvidenciaPositiva() {
        ChatIntencaoRuntimeDto reembolso = perfil(
                "aereo.regra_tarifaria",
                termo("reembolso", "10.000", "POSITIVA"),
                termo("hotel", "12.000", "NEGATIVA"));

        ChatIntencaoClassificacao resultado = classifier.classificar(
                "Quero saber sobre reembolso de hotel.",
                List.of(reembolso),
                new BigDecimal("8.000"),
                new BigDecimal("2.000"));

        assertThat(resultado.getStatus()).isEqualTo("SEM_EVIDENCIA");
    }

    @Test
    void naoConfundeTermoComParteDeOutraPalavra() {
        ChatIntencaoRuntimeDto limite = perfil(
                "financeiro.limites", termo("limite", "10.000", "POSITIVA"));

        ChatIntencaoClassificacao resultado = classifier.classificar(
                "O acesso está ilimitado.",
                List.of(limite),
                new BigDecimal("8.000"),
                new BigDecimal("2.000"));

        assertThat(resultado.getStatus()).isEqualTo("SEM_EVIDENCIA");
    }

    private ChatIntencaoRuntimeDto perfil(String codigo, ChatIntencaoRuntimeDto.Termo... termos) {
        ChatIntencaoRuntimeDto perfil = new ChatIntencaoRuntimeDto();
        perfil.setCodigo(codigo);
        perfil.setNome(codigo);
        perfil.setPrioridade(100);
        perfil.setTermos(List.of(termos));
        return perfil;
    }

    private ChatIntencaoRuntimeDto.Termo termo(String texto, String peso, String polaridade) {
        ChatIntencaoRuntimeDto.Termo termo = new ChatIntencaoRuntimeDto.Termo();
        termo.setTermo(texto);
        termo.setTermoNormalizado(texto);
        termo.setPeso(new BigDecimal(peso));
        termo.setPolaridade(polaridade);
        return termo;
    }
}
