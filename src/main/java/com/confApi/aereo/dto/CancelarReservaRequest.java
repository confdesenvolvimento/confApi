package com.confApi.aereo.dto;

import com.confApi.model.IdentificacaoAgenciaModel;
import lombok.Data;

@Data
public class CancelarReservaRequest {
    private IdentificacaoAgenciaModel identificacaoAgenciaModel;
    private String sistema;
    private Agencia agencia;
    private Boolean cancelarEticketsAtivos;
    private String localizador;
    private String motivo;
    private String reembolso;
}
