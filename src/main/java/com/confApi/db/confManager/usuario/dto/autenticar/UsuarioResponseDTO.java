package com.confApi.db.confManager.usuario.dto.autenticar;

import lombok.Data;

@Data
public class UsuarioResponseDTO {
    private UsuarioDTO usuario;
    private AgenciaDTO agencia; // null quando não tiver
    private UnidadeDTO unidade; // null quando não tiver
    private String mensagem;    // "sem agencia vinculada", "sem unidade vinculada" etc.

    public UsuarioResponseDTO() {}

    public UsuarioResponseDTO(UsuarioDTO usuario, AgenciaDTO agencia, UnidadeDTO unidade, String mensagem) {
        this.usuario = usuario;
        this.agencia = agencia;
        this.unidade = unidade;
        this.mensagem = mensagem;
    }

}
