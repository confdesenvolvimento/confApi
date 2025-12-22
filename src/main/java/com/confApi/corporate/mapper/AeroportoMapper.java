package com.confApi.corporate.mapper;

import com.confApi.corporate.dto.AeroportoDTO.AeroportoDTO;
import com.confApi.corporate.dto.AeroportoDTO.CidadeDTO;
import com.confApi.corporate.dto.AeroportoDTO.PaisDTO;
import com.confApi.db.confManager.aeroporto.Aeroporto;

import java.util.List;
import java.util.stream.Collectors;

public class AeroportoMapper {

    private AeroportoMapper() {
    }

    public static AeroportoDTO toAeroportoDTO(Aeroporto aeroporto) {

        if (aeroporto == null) {
            return null;
        }

        AeroportoDTO aeroportoDTO = new AeroportoDTO();
        aeroportoDTO.setNomeAeroporto(aeroporto.getNomeAeroporto());
        aeroportoDTO.setIataAeroporto(aeroporto.getIataAeroporto());

        if (aeroporto.getCidade() != null) {

            CidadeDTO cidadeDTO = new CidadeDTO();
            cidadeDTO.setNomeCidade(aeroporto.getCidade().getNomeCidade());
            cidadeDTO.setIataCidade(aeroporto.getCidade().getIataCidade());

            if (aeroporto.getCidade().getPais() != null) {

                PaisDTO paisDTO = new PaisDTO();
                paisDTO.setNomePais(aeroporto.getCidade().getPais().getNomePais());
                paisDTO.setIataPais(aeroporto.getCidade().getPais().getIataPais());

                cidadeDTO.setPais(paisDTO);
            }

            aeroportoDTO.setCidade(cidadeDTO);
        }

        return aeroportoDTO;
    }

    public static List<AeroportoDTO> toAeroportoDTOList(List<Aeroporto> aeroportos) {
        return aeroportos.stream()
                .map(AeroportoMapper::toAeroportoDTO)
                .collect(Collectors.toList());
    }
}

