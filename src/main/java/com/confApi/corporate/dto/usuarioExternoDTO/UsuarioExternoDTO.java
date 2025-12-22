package com.confApi.corporate.dto.usuarioExternoDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UsuarioExternoDTO {
    private String login;
    private String email;
    @JsonIgnore
    private String telefone;
    private String celular;
    private String cpf;
    @JsonProperty("nome_completo")
    private String nomeCompleto;
    @JsonProperty("primeiro_nome")
    private String primeiroNome;
    @JsonProperty("ultimo_sobrenome")
    private String ultimoNome;
    private String sexo;
    @JsonProperty("emissao_permitida")
    private boolean podeEmitir;
    @JsonIgnore
    private String token;
    private Integer status;
}
