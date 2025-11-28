package com.confApi.endPoints.usuario;

import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.endPoints.agencia.Agencia;
import com.confApi.endPoints.unidade.Unidade;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Data
public class UsuarioResponse {
    private Integer codgUsuario;
    private String nomeCompleto;
    private String loginUsuario;
    private String chaveUsuario;
    private String email;
    private String senha;
    private String idToken;
    private String cpf;
    private String telefone;
    private String celular;
    private String codgSistemaBackOffice;
    private Integer status;
    private String emissaoAutorizada;
    private String sexo;
    private String administradorAgencia;
    private String tipoUsuario;
    private Unidade unidade;
    private Agencia agencia;
    private Integer codigoWooba;
    private String qrcode;

    public UsuarioResponse(Usuario usuario) {
        this.codgUsuario = usuario.getCodgUsuario();
        this.nomeCompleto = usuario.getNomeCompleto();
        this.loginUsuario = usuario.getLoginUsuario();
        this.chaveUsuario = usuario.getChaveUsuario();
        this.email = usuario.getEmail();
        this.senha = usuario.getSenha();
        this.idToken = usuario.getIdToken();
        this.cpf = usuario.getCpf();
        this.telefone = usuario.getTelefone();
        this.celular = usuario.getCelular();
        this.codgSistemaBackOffice = usuario.getCodgSistemaBackOffice();
        this.status = usuario.getStatus();
        this.emissaoAutorizada = usuario.getEmissaoAutorizada();
        this.sexo = usuario.getSexo();
        this.administradorAgencia = usuario.getAdministradorAgencia();
        this.tipoUsuario = usuario.getTipoUsuario();
        this.unidade = usuario.getUnidade() != null ? new Unidade(usuario.getUnidade()) : null;
        this.agencia = usuario.getAgencia() != null ? new Agencia(usuario.getAgencia()) : null;
        this.codigoWooba = usuario.getCodigoWooba();
        this.qrcode = usuario.getQrcode();
    }
}
