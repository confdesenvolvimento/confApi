package com.confApi.db.confManager.usuario.dto.autenticar;

import lombok.Data;

@Data
public class UnidadeDTO {
    private Long id;
    private String nome;

    public UnidadeDTO() {}

    public UnidadeDTO(Long id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    // getters/setters
}