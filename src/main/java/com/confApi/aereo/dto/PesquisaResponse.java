package com.confApi.aereo.dto;

import com.confApi.hub.aereo.dto.Aeroporto;
import com.confApi.hub.aereo.dto.Companhia;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PesquisaResponse  implements Serializable {
    private List<Companhia> companhias;
    private Date dataIda;
    private Date dataVolta;
    private Aeroporto destino;
    private Object exception;
    private Aeroporto origem;
    private Integer quantidadeAdultos;
    private Integer quantidadeBebes;
    private Integer quantidadeCriancas;
    private FamiliaPreco familiaPreco;
    private Object viagensMultiplosTrechos;
    private List<Trecho> trechos1;
    private List<Trecho> trechos2;
    private Resumo resumo;
    }
