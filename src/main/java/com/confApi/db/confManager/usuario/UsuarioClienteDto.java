package com.confApi.db.confManager.usuario;

import lombok.Data;

@Data
public class UsuarioClienteDto {

    private Integer id;
    private String nome;
    private String email;
    private String telefone;
    private String celular;

    public UsuarioClienteDto() {
    }

    public UsuarioClienteDto(Usuario usuario) {
        this.id = usuario.getCodgUsuario();
        this.nome = usuario.getNomeCompleto();
        this.email = usuario.getEmail();
        this.telefone = usuario.getTelefone();
        this.celular = usuario.getCelular();
    }
}
