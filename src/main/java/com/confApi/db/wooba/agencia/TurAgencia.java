package com.confApi.db.wooba.agencia;

import lombok.Data;

import java.io.Serializable;

@Data
public class TurAgencia implements Serializable {

    private Integer id;
    private String nome;
    private String cnpj;
    private String idErp;
    private Integer turUnidadesOperacionais;
    private Integer status;
    private Integer statusEmissao;
    private String logomarca;
    private String email;
    private String telefone;
    private String endereco;
    private String cidade;
    private String estado;
    private String bairro;
    private String cep;
    private String complemento;
}
