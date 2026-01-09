package com.confApi.seguros.mapper;

import com.confApi.db.confManager.seguro.segurado.SeguroSegurado;
import com.confApi.seguros.dto.SeguradoDTO;
import com.confApi.seguros.util.DateUtil;

import java.util.ArrayList;
import java.util.List;

public final class SeguradoMapper {

    private SeguradoMapper() {}

    public static SeguradoDTO toDTO(SeguroSegurado s) {
        if (s == null) return null;

        SeguradoDTO dto = new SeguradoDTO();

        dto.setNome(s.getPrimeiroNome());
        dto.setSobrenome(s.getUltimoNome());


        dto.setCpf(s.getCpf());
        dto.setTelefone(s.getTelefone());
        dto.setEmail(s.getEmail());

        // Sexo (1=M, 2=F)
        dto.setSexo(mapSexoBack(s.getSexo()));

        // Data nascimento
        dto.setNascimento(DateUtil.extractDateToString(s.getDataNascimento()));

        // Endereço
        dto.setCep(s.getEnderecoCep());
        dto.setEndereco(s.getEnderecoEndereco());
        dto.setNumero(s.getEnderecoNumero());
        dto.setComplemento(s.getEnderecoComplemento());
        dto.setBairro(s.getEnderecoBairro());
        dto.setCidade(s.getEnderecoCidade());
        dto.setUf(s.getEnderecoEstado());

        return dto;
    }

    public static List<SeguradoDTO> toDTOList(List<SeguroSegurado> list) {
        if (list == null || list.isEmpty()) return new ArrayList<>();
        return list.stream().map(SeguradoMapper::toDTO).toList();
    }

    private static String mapSexoBack(Integer sexo) {
        if (sexo == null) return null;
        return sexo == 1 ? "M" : sexo == 2 ? "F" : null;
    }
}
