package com.confApi.aereo.dto;

import com.confApi.hub.aereo.dto.Bagagem;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FamiliaPreco implements Serializable {

    private Boolean bagagemInclusa;
    private Double bagagemPeso;
    private Integer bagagemQuantidade;
    private String baseTarifaria;
    private String cabine;
    private String classe;
    private String tipo;
    private Familia familia;
    private Preco preco;
    private String identificacaoDeVoo;
    private String identificacaoTarifaDeVoo;
    private String descricaocss = "color_default";
    private String color = "#007f7c";
    private boolean isLinkFamilia = false;
    private boolean isFamiliaSelecionadaCotacao = false;
    private List<FamiliaPrecoInter> familiaPrecoInterList;
    private String identificacaoDaViagem;
    private List<Bagagem> bagagens = new ArrayList<>();
    private Boolean linkFamilia;
}
