package com.confApi.carros.dto;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Preserva a descrição da recusa do fornecedor nos contratos de reserva e emissão. */
public final class CarroErroMensagem {
    private static final Pattern AVISO_SUPORTE = Pattern.compile(
            "please\\s+report\\s+the\\s+error\\s+code\\s+to\\s+mobility\\s+support\\s*:?\\s*"
                    + "(?:[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12})?", Pattern.CASE_INSENSITIVE);

    private CarroErroMensagem() {}

    public static String obter(List<ErrorResponse> erros, Object retorno, String mensagem, String fallback) {
        Set<String> descricoes = new LinkedHashSet<>();
        if (erros != null) {
            for (ErrorResponse erro : erros) {
                if (erro == null) continue;
                String descricao = texto(erro.getErrorDescription());
                if (descricao.isEmpty()) descricao = texto(erro.getError());
                if (!descricao.isEmpty()) descricoes.add(descricao);
            }
        }
        String descricao = "";
        Set<String> avisosSuporte = new LinkedHashSet<>();
        for (String candidato : List.of(String.join(" | ", descricoes), texto(retorno), texto(mensagem))) {
            if (candidato.isEmpty()) continue;
            String motivo = AVISO_SUPORTE.matcher(candidato).replaceAll("")
                    .replaceAll("^[\\s|:;.]+|[\\s|]+$", "").trim();
            if (motivo.isEmpty()) avisosSuporte.add(candidato);
            else if (descricao.isEmpty()) descricao = candidato;
        }
        // Conserva a referência de suporte sem deixá-la ocultar o motivo da recusa.
        if (!avisosSuporte.isEmpty()) {
            String suporte = String.join(" | ", avisosSuporte);
            return descricao.isEmpty() ? suporte : descricao + " | " + suporte;
        }
        return descricao.isEmpty() ? fallback : descricao;
    }

    private static String texto(Object valor) {
        if (valor instanceof String) return ((String) valor).trim();
        if (valor instanceof Iterable<?>) {
            Set<String> mensagens = new LinkedHashSet<>();
            for (Object item : (Iterable<?>) valor) {
                String mensagem = texto(item);
                if (!mensagem.isEmpty()) mensagens.add(mensagem);
            }
            return String.join(" | ", mensagens);
        }
        if (valor instanceof Map<?, ?>) {
            Map<?, ?> objeto = (Map<?, ?>) valor;
            for (String campo : List.of("error_description", "errorDescription", "message", "mensagem", "returnMessage", "error")) {
                String mensagem = texto(objeto.get(campo));
                if (!mensagem.isEmpty()) return mensagem;
            }
        }
        return "";
    }
}
