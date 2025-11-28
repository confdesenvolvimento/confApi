package com.confApi.hub.aereo;

import lombok.Data;

import java.util.List;

@Data
public class TrechoHub {
    private String sistema;
    private CompanhiaHub companhia;
    private AeroportoHub destino;
    private Integer duracao;
    private Integer numeroParadas;
    private AeroportoHub origem;
    private List<VooHub> voos;
    //    private List<FamiliaPreco> familiaPrecoSemBagagem;
    private List<FamiliaPrecoHub> familias;
    private String tempoDeDuracao;
    private String identificacaoDaViagem;
    private FornecedorHub fornecedor;
}
