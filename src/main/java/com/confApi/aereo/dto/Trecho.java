package com.confApi.aereo.dto;

import com.confApi.hub.aereo.dto.Aeroporto;
import com.confApi.hub.aereo.dto.Companhia;
import com.confApi.hub.aereo.dto.Voo;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
@Data
public class Trecho implements Serializable {

    private String sistema;
    private Companhia companhia;
    private Aeroporto destino;
    private Integer duracao;
    private Integer numeroParadas;
    private Aeroporto origem;
    private List<Voo> voos;
    private List<FamiliaPreco> familias;
    private List<FamiliaPreco> familiaPrecoSemBagagem;
    private List<FamiliaPreco> familiaPrecoComBagagem;
    private String tempoDeDuracao;
    private FamiliaPreco familiaSelecionada;
    private Fornecedor fornecedor;
    private String identificacaoDaViagem;
    private boolean selected;
    private String tipoTrecho;
    private String indicadorDescricaoRota;
}
