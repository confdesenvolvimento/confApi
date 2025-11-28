package com.confApi.hub.aereo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class PesquisaResponseHub {
    private List<CompanhiaHub> companhias;
    private Date dataIda;
    private Date dataVolta;
    private AeroportoHub destino;
    private Object exception;
    private AeroportoHub origem;
    private Integer quantidadeAdultos = 0;
    private Integer quantidadeBebes = 0;
    private Integer quantidadeCriancas = 0;
    //    private FamiliaPreco familiaPreco;
    private List<TrechoHub> viagensMultiplosTrechos;
    private List<TrechoHub> trechos1;
    private List<TrechoHub> trechos2;
    private ResumoHub resumo;
}
