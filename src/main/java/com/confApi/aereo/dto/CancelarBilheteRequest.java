package com.confApi.aereo.dto;

import com.confApi.model.IdentificacaoAgenciaModel;
import lombok.Data;

@Data
public class CancelarBilheteRequest {
    private IdentificacaoAgenciaModel identificacaoAgenciaModel;
    private Agencia agencia;
    private Boolean cancelarEticketsAtivos;
    private String eticket;
    private String sistema;
    private String motivo;
    private String localizador;
}
