package com.confApi.chatgpt.util;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Formato de localizador compartilhado pelo chat legado e pela V2. */
public final class LocalizadorAereo {
    private LocalizadorAereo() {}

    public static boolean isValido(String value) {
        return value != null && value.matches("[A-Za-z0-9]{5,13}")
                && !isPalavraComum(value.toUpperCase(Locale.ROOT));
    }

    public static String extrair(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        if (isValido(input.trim())) {
            return input.trim().toUpperCase(Locale.ROOT);
        }
        String texto = normalizarTexto(input).toUpperCase(Locale.ROOT);
        Matcher explicito = Pattern.compile(
                "\\b(?:LOCALIZADOR|LOC|RESERVA|PNR)\\s*(?:N(?:O|RO)?\\.?\\s*)?[:#-]?\\s*([A-Z0-9]{5,13})\\b",
                Pattern.CASE_INSENSITIVE)
                .matcher(texto);
        if (explicito.find()) {
            String candidato = explicito.group(1).toUpperCase(Locale.ROOT);
            if (!isPalavraComum(candidato)) {
                return candidato;
            }
        }

        Matcher contextoOperacional = Pattern.compile(
                "\\b(?:ABRIR|ABRA|CANCELAR|CANCELE|REGRA|REGRAS|REMARCAR|REMARCACAO|SIMULAR)\\b"
                        + "(?:\\s+[A-Z]+){0,4}\\s+([A-Z0-9]{5,13})\\s*[?.!]*$")
                .matcher(texto);
        if (contextoOperacional.find()) {
            String candidato = contextoOperacional.group(1).toUpperCase(Locale.ROOT);
            if (!isPalavraComum(candidato)) {
                return candidato;
            }
        }

        Matcher alfanumerico = Pattern.compile("\\b(?=[A-Z0-9]{5,13}\\b)(?=[A-Z0-9]*[A-Z])(?=[A-Z0-9]*[0-9])[A-Z0-9]+\\b")
                .matcher(texto);
        while (alfanumerico.find()) {
            String candidato = alfanumerico.group();
            if (!isPalavraComum(candidato)) {
                return candidato;
            }
        }
        return null;
    }

    public static boolean isPalavraComum(String candidato) {
        return candidato == null || Set.of(
                "QUERO", "REGRA", "REGRAS", "MULTA", "AEREO", "AEREA", "VOOS", "VOO",
                "LOCALIZADOR", "RESERVA", "REEMBOLSO", "ALTERACAO", "REMARCACAO", "REMARCAR",
                "SIMULAR", "BILHETE", "PASSAGEM", "CANCELAMENTO", "POSSUI", "TENHO", "SABER",
                "DESEJO", "PRECISO", "GOSTARIA", "ALTERAR", "EMITIDA", "EMITIDO", "RECENTE",
                "RECENTES", "DESEJADA", "DESEJADO", "CANCELAR", "CANCELE", "ABRIR", "ABRA",
                "MOSTRAR", "MOSTRE", "VISUALIZAR", "ULTIMA", "ULTIMAS", "RESERVAS", "LISTAR",
                "MINHA", "MINHAS", "CONSULTAR", "EMITIR", "AGORA", "ANTERIOR", "ANTERIORES",
                "CANCELADA", "CANCELADAS", "CONFIRMADA", "CONFIRMADAS", "INFORMACOES", "DETALHES"
        ).contains(candidato);
    }

    private static String normalizarTexto(String value) {
        if (value == null) {
            return "";
        }
        String semAcento = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT);
    }
}
