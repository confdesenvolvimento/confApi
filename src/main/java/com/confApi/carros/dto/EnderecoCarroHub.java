package com.confApi.carros.dto;

import lombok.Data;

@Data
public class EnderecoCarroHub {
    private String rua;
    private String numero;
    private String bairro;
    private String cidade;
    private String complemento;
    private String estado;
    private String pais;
    private String cep;
}
