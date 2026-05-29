package com.confApi.aereo.dto;

import com.confApi.model.IdentificacaoAgenciaModel;
import lombok.Data;

@Data
public class MapaAssentoRequest {
    private IdentificacaoAgenciaModel identificacaoAgenciaModel;
    private Agencia agencia;
    private String sistema;
    private String localizador;
    private String identificacaoDeVoo;
}
