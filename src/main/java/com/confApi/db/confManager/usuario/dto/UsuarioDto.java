package com.confApi.db.confManager.usuario.dto;

import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.exception.MensagemDeErro;
import com.confApi.db.confManager.unidade.dto.Unidade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioDto {
    private Integer codigo;
    private Integer id;
    private String nome;
    private String login;
    private String chaveToken;
    private String email;
    private String tokenAcesso;
    private String cpf;
    private String telefone;
    private String celular;
    private String usuarioBackOffice;
    private Integer status;
    private String emissaoAutorizada;
    private String sexo;
    private String administradorAgencia;
    private String tipoUsuario;
    private Unidade unidade ;
    private Agencia agencia;
    private Integer codigoWooba;
    private MensagemDeErro mensagemDeErro;

}
