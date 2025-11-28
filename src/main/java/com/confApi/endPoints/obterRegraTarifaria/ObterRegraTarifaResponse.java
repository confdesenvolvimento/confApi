package com.confApi.endPoints.obterRegraTarifaria;

import com.confApi.endPoints.regraTarifariaResponse.RegraTarifariaResponse;
import lombok.Data;

import java.util.List;

@Data
public class ObterRegraTarifaResponse {
    private String data;
    private String dataVersao;
    private boolean sessaoExpirada;
    private String exception;
    private List<RegraTarifariaResponse> regraTarifaria;
}
