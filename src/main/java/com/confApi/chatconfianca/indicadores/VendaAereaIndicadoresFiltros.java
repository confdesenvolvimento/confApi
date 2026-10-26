package com.confApi.chatconfianca.indicadores;

import java.time.LocalDate;
import lombok.Data;

/** Consulta somente de leitura. A agencia e o tipo de data nao sao escolhidos pelo modelo. */
@Data
public class VendaAereaIndicadoresFiltros {
    private LocalDate inicio;
    private LocalDate fim;
    private String origem;
    private String destino;
    private String companhia;
    private String nomePassageiro;
    private String agrupamento = "NENHUM";
    private Integer page = 0;
    private Integer size = 10;
    private boolean detalhar;
    private boolean incluirValores;

    public VendaAereaIndicadoresFiltros copy() {
        var c = new VendaAereaIndicadoresFiltros();
        c.inicio = inicio; c.fim = fim; c.origem = origem; c.destino = destino;
        c.companhia = companhia; c.nomePassageiro = nomePassageiro;
        c.agrupamento = agrupamento; c.page = page; c.size = size; c.detalhar = detalhar; c.incluirValores = incluirValores;
        return c;
    }
}
