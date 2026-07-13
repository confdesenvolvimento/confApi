package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ManagerUnidade {
    private Integer codgUnidade;
    private String nomeUnidade;
    private String codgSistemaBackOffice;
    private Integer status;
    private Integer idWoobaUnidade;
}