package com.confApi.chatconfianca.dto.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ManagerAgencia {
    private Integer codgAgencia;
    private String nomeAgencia;
    private String cnpj;
    private String codgSistemaBackOffice;
    private ManagerUnidade codgUnidade;
    private Integer status;
    private Integer idWoobaAgencia;
}