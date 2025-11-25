package com.confApi.db.confManager.usuario;

import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.unidade.dto.Unidade;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Usuario implements Serializable {

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

    public Usuario(Integer codgUsuario) {
        this.codgUsuario = codgUsuario;
    }

    public Usuario() {
    }

    public Usuario(UsuarioClienteDto usuarioClienteDto) {
        this.codgUsuario = usuarioClienteDto.getId();
    }

    public Integer getCodgUsuario() {
        return codgUsuario;
    }

    public void setCodgUsuario(Integer codgUsuario) {
        this.codgUsuario = codgUsuario;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getLoginUsuario() {
        return loginUsuario;
    }

    public void setLoginUsuario(String loginUsuario) {
        this.loginUsuario = loginUsuario;
    }

    public String getChaveUsuario() {
        return chaveUsuario;
    }

    public void setChaveUsuario(String chaveUsuario) {
        this.chaveUsuario = chaveUsuario;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public String getCodgSistemaBackOffice() {
        return codgSistemaBackOffice;
    }

    public void setCodgSistemaBackOffice(String codgSistemaBackOffice) {
        this.codgSistemaBackOffice = codgSistemaBackOffice;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getEmissaoAutorizada() {
        return emissaoAutorizada;
    }

    public void setEmissaoAutorizada(String emissaoAutorizada) {
        this.emissaoAutorizada = emissaoAutorizada;
    }

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public String getAdministradorAgencia() {
        return administradorAgencia;
    }

    public void setAdministradorAgencia(String administradorAgencia) {
        this.administradorAgencia = administradorAgencia;
    }

    public String getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(String tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }

    public Unidade getUnidade() {
        return unidade;
    }

    public void setUnidade(Unidade unidade) {
        this.unidade = unidade;
    }

    public Agencia getAgencia() {
        return agencia;
    }

    public void setAgencia(Agencia agencia) {
        this.agencia = agencia;
    }

    public Integer getCodigoWooba() {
        return codigoWooba;
    }

    public void setCodigoWooba(Integer codigoWooba) {
        this.codigoWooba = codigoWooba;
    }

    public String getQrcode() {
        return qrcode;
    }

    public void setQrcode(String qrcode) {
        this.qrcode = qrcode;
    }
}
