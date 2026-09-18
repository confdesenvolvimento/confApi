package com.confApi.chatconfianca.intencao;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class ChatIntencaoTermoClassifier {

    public ChatIntencaoClassificacao classificar(String mensagem,
                                                  List<ChatIntencaoRuntimeDto> perfis,
                                                  BigDecimal minScore,
                                                  BigDecimal minMargin) {
        if (perfis == null || perfis.isEmpty()) {
            return ChatIntencaoClassificacao.status("SEM_DADOS");
        }
        String texto = normalizar(mensagem);
        if (texto.isEmpty()) {
            return ChatIntencaoClassificacao.status("SEM_EVIDENCIA");
        }

        List<Candidato> candidatos = new ArrayList<>();
        for (ChatIntencaoRuntimeDto perfil : perfis) {
            Candidato candidato = pontuar(texto, perfil);
            if (candidato.score.signum() > 0) {
                candidatos.add(candidato);
            }
        }
        if (candidatos.isEmpty()) {
            return ChatIntencaoClassificacao.status("SEM_EVIDENCIA");
        }
        candidatos.sort(Comparator.comparing(Candidato::score).reversed());
        Candidato melhor = candidatos.get(0);
        BigDecimal segundoScore = candidatos.size() > 1
                ? candidatos.get(1).score : BigDecimal.ZERO;
        BigDecimal scoreMinimo = positivoOu(minScore, new BigDecimal("8.000"));
        BigDecimal margemMinima = positivoOu(minMargin, new BigDecimal("2.000"));
        BigDecimal margem = melhor.score.subtract(segundoScore);

        String status = melhor.score.compareTo(scoreMinimo) < 0
                ? "BAIXA_CONFIANCA"
                : (segundoScore.signum() > 0 && margem.compareTo(margemMinima) < 0
                ? "AMBIGUA" : "CLASSIFICADA");
        return resultado(status, melhor, segundoScore);
    }

    private Candidato pontuar(String texto, ChatIntencaoRuntimeDto perfil) {
        BigDecimal score = BigDecimal.ZERO;
        List<String> positivos = new ArrayList<>();
        List<String> negativos = new ArrayList<>();
        if (perfil != null && perfil.getTermos() != null) {
            for (ChatIntencaoRuntimeDto.Termo termo : perfil.getTermos()) {
                String normalizado = normalizar(termo.getTermoNormalizado() == null
                        ? termo.getTermo() : termo.getTermoNormalizado());
                if (normalizado.isEmpty() || !contemExpressao(texto, normalizado)) {
                    continue;
                }
                BigDecimal peso = termo.getPeso() == null ? BigDecimal.ONE : termo.getPeso().abs();
                if ("NEGATIVA".equalsIgnoreCase(termo.getPolaridade())) {
                    score = score.subtract(peso);
                    negativos.add(termo.getTermo());
                } else {
                    score = score.add(peso);
                    positivos.add(termo.getTermo());
                }
            }
        }
        return new Candidato(perfil, score, positivos, negativos);
    }

    private ChatIntencaoClassificacao resultado(String status,
                                                 Candidato melhor,
                                                 BigDecimal segundoScore) {
        ChatIntencaoClassificacao resultado = new ChatIntencaoClassificacao();
        resultado.setStatus(status);
        resultado.setIntencaoId(melhor.perfil.getId());
        resultado.setCodigo(melhor.perfil.getCodigo());
        resultado.setNome(melhor.perfil.getNome());
        resultado.setScore(melhor.score.setScale(3, RoundingMode.HALF_UP));
        resultado.setSegundoScore(segundoScore.setScale(3, RoundingMode.HALF_UP));
        resultado.setConfianca(calcularConfianca(melhor, segundoScore));
        resultado.getTermosPositivos().addAll(melhor.positivos);
        resultado.getTermosNegativos().addAll(melhor.negativos);
        return resultado;
    }

    private int calcularConfianca(Candidato melhor, BigDecimal segundoScore) {
        double scoreAbsoluto = Math.min(100d, melhor.score.doubleValue() * 5d);
        double margem = segundoScore.signum() == 0 ? 100d
                : Math.max(0d, Math.min(100d,
                melhor.score.subtract(segundoScore).doubleValue() * 100d / melhor.score.doubleValue()));
        double evidencias = Math.min(100d, melhor.positivos.size() * 35d);
        return (int) Math.round(scoreAbsoluto * 0.55d + margem * 0.30d + evidencias * 0.15d);
    }

    private boolean contemExpressao(String texto, String termo) {
        return (" " + texto + " ").contains(" " + termo + " ");
    }

    private BigDecimal positivoOu(BigDecimal valor, BigDecimal padrao) {
        return valor == null || valor.signum() < 0 ? padrao : valor;
    }

    private String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private record Candidato(ChatIntencaoRuntimeDto perfil,
                             BigDecimal score,
                             List<String> positivos,
                             List<String> negativos) {
    }
}
