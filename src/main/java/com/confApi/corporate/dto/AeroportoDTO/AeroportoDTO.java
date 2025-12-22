package com.confApi.corporate.dto.AeroportoDTO;

import lombok.Data;

@Data
public class AeroportoDTO {

    private String nomeAeroporto;
    private String iataAeroporto;
    private CidadeDTO cidade;
}
