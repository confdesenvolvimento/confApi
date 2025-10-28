package com.confApi.db.sica.empresa;

import lombok.Data;

import java.io.Serializable;

@Data
public class Empresa implements Serializable {

    private Integer codemp;
    private String nome;
    private String endereco;
    private String bairro;
    private String cidade;
    private String UF;
    private String cep;
    private String fone1;
    private String fone2;
    private String cnpj;
    private String email;
    private Integer condpag;
    private String codest;
    private Integer codUnidade;
    private Boolean Ativo;
}
