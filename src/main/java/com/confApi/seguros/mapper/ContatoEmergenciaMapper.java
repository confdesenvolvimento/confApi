package com.confApi.seguros.mapper;

import com.confApi.seguros.dto.ContatoEmergenciaDTO;

public class ContatoEmergenciaMapper {
    private ContatoEmergenciaMapper() {}

    public static ContatoEmergenciaDTO toDTO(String nome, String telefone, String email) {
        if (nome == null && telefone == null && email == null) return null;

        ContatoEmergenciaDTO dto = new ContatoEmergenciaDTO();
        dto.setNome(nome);
        dto.setTelefone(telefone);
        dto.setEmail(email);
        return dto;
    }
}
