package com.confApi.aereo.dto;

import com.confApi.model.IdentificacaoAgenciaModel;
import lombok.Data;

@Data
public class BuscarFormasFinanciamentoRequest {
    private IdentificacaoAgenciaModel identificacaoAgenciaModel;
    private String sistema;
    private Agencia agencia;
    private String localizador;
    private PagamentoForma pagamentoForma;
}
