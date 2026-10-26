package com.confApi.chatconfianca.reservas;

import lombok.Data;

import java.time.LocalDate;

/** Filtros de leitura: a agência nunca pode ser informada pelo usuário/modelo. */
@Data
public class ReservaConsultaFiltros {
    private String nomePassageiro;
    private String origem;
    private String destino;
    private String companhia;
    private String localizador;
    private Integer reservaId;
    private String situacao = "TODAS";
    private String tipoData = "CRIACAO";
    private LocalDate inicio;
    private LocalDate fim;
    private Integer page = 0;
    private Integer size = 10;

    public ReservaConsultaFiltros copy() {
        ReservaConsultaFiltros copy = new ReservaConsultaFiltros();
        copy.nomePassageiro = nomePassageiro;
        copy.origem = origem;
        copy.destino = destino;
        copy.companhia = companhia;
        copy.localizador = localizador;
        copy.reservaId = reservaId;
        copy.situacao = situacao;
        copy.tipoData = tipoData;
        copy.inicio = inicio;
        copy.fim = fim;
        copy.page = page;
        copy.size = size;
        return copy;
    }
}
