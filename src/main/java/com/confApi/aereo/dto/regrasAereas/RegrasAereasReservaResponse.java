package com.confApi.aereo.dto.regrasAereas;

import com.confApi.db.confManager.regraAereaAlteracao.dto.RegraAereaAlteracaoConsultaResponse;
import com.confApi.db.confManager.regraAereaReembolso.dto.RegraAereaReembolsoConsultaResponse;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class RegrasAereasReservaResponse {
    private String status;
    private String mensagem;
    private RegraAereaReembolsoConsultaResponse reembolso;
    private List<RegraAereaReembolsoConsultaResponse> reembolsos = new ArrayList<>();
    private RegraAereaAlteracaoConsultaResponse alteracao;
    private List<RegraAereaAlteracaoConsultaResponse> alteracoes = new ArrayList<>();
    private Map<String, Object> dadosReservaUtilizados = new HashMap<>();
}
