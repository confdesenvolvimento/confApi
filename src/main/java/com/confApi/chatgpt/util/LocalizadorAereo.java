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
        // A abreviacao de numero exige separador: o N de NICC3S pertence ao codigo.
        Matcher explicito = Pattern.compile(
                "\\b(?:LOCALIZADOR|LOC|RESERVA|PNR)\\b\\s*"
                        + "(?:(?:E|EH)\\s+)?(?:N(?:O|RO|[º°])?(?=[\\s.:#-])\\.?\\s*)?[:#-]?\\s*([A-Z0-9]{5,13})\\b",
                Pattern.CASE_INSENSITIVE)
                .matcher(texto);
        if (explicito.find()) {
            String candidato = explicito.group(1).toUpperCase(Locale.ROOT);
            if (!isPalavraComum(candidato)) {
                return candidato;
            }
        }

        // Fora de um marcador explícito, preserve a evidência do texto original.
        // Converter a frase inteira em maiúsculas transformava DESTA/QUAIS em PNR.
        Matcher tokens = Pattern.compile("(?<![\\p{L}\\p{N}])[A-Za-z0-9]{5,13}(?![\\p{L}\\p{N}])").matcher(normalizarSemAcento(input));
        String encontrado = null;
        while (tokens.find()) {
            String token = tokens.group();
            String candidato = token.toUpperCase(Locale.ROOT);
            boolean codigoMisto = token.matches(".*[A-Za-z].*") && token.matches(".*[0-9].*");
            if (!isValido(candidato) || (!codigoMisto && !token.equals(candidato))) continue;
            if (encontrado != null && !encontrado.equals(candidato)) return null;
            encontrado = candidato;
        }
        return encontrado;
    }

    public static boolean isPalavraComum(String candidato) {
        return candidato == null || Set.of(
                "QUERO", "REGRA", "REGRAS", "MULTA", "AEREO", "AEREA", "VOOS", "VOO",
                "LOCALIZADOR", "RESERVA", "REEMBOLSO", "ALTERACAO", "REMARCACAO", "REMARCAR",
                "SIMULAR", "BILHETE", "PASSAGEM", "CANCELAMENTO", "POSSUI", "TENHO", "SABER",
                "DESEJO", "PRECISO", "GOSTARIA", "ALTERAR", "EMITIDA", "EMITIDO", "RECENTE",
                "RECENTES", "DESEJADA", "DESEJADO", "CANCELAR", "CANCELE", "ABRIR", "ABRA",
                "MOSTRAR", "MOSTRE", "VISUALIZAR", "ULTIMA", "ULTIMAS", "RESERVAS", "LISTAR",
                "SELECIONADA", "SELECIONADO", "CARREGADA", "CONSULTADA", "ATUAL", "CORRETA", "CORRETO",
                "DESTA", "DESTE", "DESSA", "DESSE", "ESTA", "ESTE", "QUAIS", "QUANTA", "QUANTO", "QUANDO", "QUEM",
                "PASSAGEIROS", "TARIFA", "TARIFARIA", "TARIFARIAS", "EXISTE", "PAGAR", "IGUAL", "EXPLIQUE", "FALHAR",
                "RESTRICOES", "SITUACAO", "BAGAGEM", "INCLUIDA", "SIMPLES", "PERMITE", "TEXTO", "VIAGEM", "TODOS",
                "MINHA", "MINHAS", "CONSULTAR", "EMITIR", "AGORA", "ANTERIOR", "ANTERIORES",
                "CANCELADA", "CANCELADAS", "CONFIRMADA", "CONFIRMADAS", "INFORMACOES", "DETALHES"
        ).contains(candidato);
    }

    private static String normalizarSemAcento(String value) {
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
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
