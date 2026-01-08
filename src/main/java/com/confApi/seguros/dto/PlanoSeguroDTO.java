package com.confApi.seguros.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
@Data
public class PlanoSeguroDTO implements Serializable {

    private String idPlano;
    private String codgFornecedor;
    private String fornecedor; // "HERO"
    private String nomePlano;  // "HERO 60"
    private String dataInicioCobertura;
    private String dataFinalCombertura;
    // ===== Coberturas (moeda + valor) =====
    private String moedaCobertura = "USD"; // USD/EUR etc

    // ===== Preço (sempre em BRL no seu sistema) =====
    private Double precoBaseBRL;        // preço retornado do fornecedor
    private Double precoFinalBRL;       // após comissão/adicionais
    private int maxParcelas = 6;

    // (opcional) score
    private int score;

    List<CoberturaSeguroDTO> coberturasDetalhes = new ArrayList<>();


}
