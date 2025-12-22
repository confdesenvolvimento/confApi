package com.confApi.corporate.dto.usuarioExternoDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class UnidadeExternoDTO {
    @JsonIgnore
    private Integer id;
    private String nome;
}
