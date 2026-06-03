package com.confApi.carros.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FormasPagamentoCarroHub {
    private String nome;
    private String codigo;
    private List<FormasPagamentoParcelasCarroHub> formasPagamentoParcelasCarroList = new ArrayList<>();
}
