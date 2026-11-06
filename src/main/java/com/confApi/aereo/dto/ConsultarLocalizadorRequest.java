package com.confApi.aereo.dto;

import com.confApi.model.IdentificacaoAgenciaModel;
import lombok.Data;

@Data
public class ConsultarLocalizadorRequest {
    private String tipoTarifa = "CONS";
    private Integer codgReservaAereo;
    private IdentificacaoAgenciaModel identificacaoAgenciaModel;
    private String sistema;
    private Agencia agencia =null;
    private String localizador;
}
