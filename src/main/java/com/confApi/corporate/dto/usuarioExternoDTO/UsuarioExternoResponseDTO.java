package com.confApi.corporate.dto.usuarioExternoDTO;

import lombok.Data;

@Data
public class UsuarioExternoResponseDTO {
    private UsuarioExternoDTO usuario;
    private AgenciaExternoDTO agencia;
    private UnidadeExternoDTO unidade;
    private String mensagem;

    public UsuarioExternoResponseDTO(UsuarioExternoDTO usuario, AgenciaExternoDTO agencia, UnidadeExternoDTO unidade, String mensagem) {
        this.usuario = usuario;
        this.agencia = agencia;
        this.unidade = unidade;
        this.mensagem = mensagem;
    }

    public UsuarioExternoResponseDTO() {

    }
}
