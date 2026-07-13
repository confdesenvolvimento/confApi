package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ManagerUsuario {
    private Integer codgUsuario;
    private String nomeCompleto;
    private String loginUsuario;
    private String email;
    private String cpf;
    private String telefone;
    private String celular;
    private String tipoUsuario;
    private String administradorAgencia;
    private Integer status;
    private ManagerUnidade unidade;
    private ManagerAgencia agencia;
}