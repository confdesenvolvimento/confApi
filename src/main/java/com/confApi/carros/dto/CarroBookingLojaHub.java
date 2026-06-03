package com.confApi.carros.dto;

import lombok.Data;

@Data
public class CarroBookingLojaHub {
    private String cidade;
    private String codigo;
    private String endereco;
    private String telefone;
    private String horasAbertas;
    private Boolean lojaRetirada;
}
