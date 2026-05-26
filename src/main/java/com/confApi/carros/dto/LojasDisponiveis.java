package com.confApi.carros.dto;

import lombok.Data;

@Data
public class LojasDisponiveis {
    private Integer id;
    private String iataLocal;
    private String tipo;
    private String cidade;
    private String pais;
    private String paisDescricao;
    private Object codigoPostal;
    private String code;
    private String tipoCode;
    private String estadoCode;
    private String nome;
    private String email;
    private String endereco;
    private String suffix;
    private String telefone;
    private Object operacao;
    private Boolean lojaDisponivel;
    private String latitude;
    private String longitude;
    private String horaAberto;
    private Boolean createDriver;
    private String horasOficialComeca;
    private String horaOficialTermina;
}
