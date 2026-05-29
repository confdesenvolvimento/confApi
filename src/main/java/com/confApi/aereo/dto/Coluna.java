package com.confApi.aereo.dto;

import lombok.Data;

@Data
public class Coluna {
    private String cabineDeServico;
    private String caracteristicas;
    private String coluna;
    private String localizacao;
    private String moeda;
    private Boolean ocupado;
    private String pagamentoDescricao;
    private Boolean pagamentoObrigatorio;
    private String sinalizarCorredor;
    private String situacao;
    private Integer tipoDeAssento;
    private String tipoDeAssentoDescricao;
    private Double valor;
    private String idAssento;

    private Boolean selecionado = false;
}
