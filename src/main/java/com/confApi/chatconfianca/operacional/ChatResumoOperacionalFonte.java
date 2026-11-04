package com.confApi.chatconfianca.operacional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Projecao de leitura: contagens pertencem somente a fonte e ao periodo consultados. */
public record ChatResumoOperacionalFonte(String codigo, String titulo, Status status, String fonte,
        String cobertura, Long totalConhecido, List<Item> itens, boolean limitado) {
    public enum Status { DISPONIVEL, INCOMPLETO, SEM_PERMISSAO, INDISPONIVEL }

    public record Item(String id, String titulo, String descricao, LocalDate data, LocalTime hora,
                       Long reservaId, String localizador, String produto) { }

    public ChatResumoOperacionalFonte {
        if (codigo == null || titulo == null || status == null || fonte == null || cobertura == null
                || totalConhecido != null && totalConhecido < 0 || itens == null || itens.size() > 20)
            throw new IllegalArgumentException("Projecao operacional invalida.");
        itens = List.copyOf(itens);
        if ((status == Status.INDISPONIVEL || status == Status.SEM_PERMISSAO)
                && (!itens.isEmpty() || totalConhecido != null))
            throw new IllegalArgumentException("Fonte indisponivel nao pode comprovar dados.");
        if (totalConhecido != null && totalConhecido < itens.size())
            throw new IllegalArgumentException("Total menor que os itens apresentados.");
    }

    public static ChatResumoOperacionalFonte indisponivel(String codigo, String titulo) {
        return new ChatResumoOperacionalFonte(codigo, titulo, Status.INDISPONIVEL,
                "NAO_CONFIRMADA", "Não foi possível concluir esta consulta. Isso não significa ausência de pendências.",
                null, List.of(), false);
    }
}
