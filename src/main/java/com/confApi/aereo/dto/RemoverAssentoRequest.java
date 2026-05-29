package com.confApi.aereo.dto;

import com.confApi.model.IdentificacaoAgenciaModel;
import lombok.Data;

@Data
public class RemoverAssentoRequest {
    private IdentificacaoAgenciaModel identificacaoAgenciaModel;
    private Agencia agencia;
    private String sistema;
    private String localizador;
    private String trechoId;
    private String passageiroId;
    private String assentoId;
}
