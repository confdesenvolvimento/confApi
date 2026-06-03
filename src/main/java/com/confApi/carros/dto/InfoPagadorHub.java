package com.confApi.carros.dto;

import lombok.Data;

@Data
public class InfoPagadorHub {
    private String cpf;
    private String email;
    private String sexo;
    private String nascimento;
    private EnderecoCarroHub endereco;
    private ContatoCarroHub contato;
}
