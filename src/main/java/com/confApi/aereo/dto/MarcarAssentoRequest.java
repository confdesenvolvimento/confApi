package com.confApi.aereo.dto;
import com.confApi.model.IdentificacaoAgenciaModel;
import lombok.Data;

import java.util.List;
@Data
public class MarcarAssentoRequest {

    private IdentificacaoAgenciaModel identificacaoAgenciaModel;
    private Agencia agencia;
    private String sistema;
    private String localizador;
    private String indentificacaoDeVoo;
    private List<AssentoMarcar> assentoMarcarList;
}
