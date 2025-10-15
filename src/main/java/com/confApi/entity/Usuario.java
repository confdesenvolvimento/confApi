package com.confApi.entity;

import javax.persistence.*;

@Entity
@Table(name = "usuario", schema = "appConf")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long codg_usuario;
    @Column
    // @N(message = "Campo requerido")
    private String login;
    @Column
    // @NotNull(message = "Campo requerido")
    private String senha;

    @Column
    // @NotNull(message = "Campo requerido")
    private String role;

    public Long getCodg_usuario() {
        return codg_usuario;
    }

    public void setCodg_usuario(Long codg_usuario) {
        this.codg_usuario = codg_usuario;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Usuario(Long codg_usuario, String login, String senha, String role) {
        super();
        this.codg_usuario = codg_usuario;
        this.login = login;
        this.senha = senha;
        this.role = role;
    }

    public Usuario() {
        // TODO Auto-generated constructor stub
    }
}
