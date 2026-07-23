package com.confApi.aereo.dto;

import com.confApi.hub.aereo.ReservaAereoModel;
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

    public CancelarReservaRequest() {
    }

    public CancelarReservaRequest(IdentificacaoAgenciaModel identificacaoAgenciaModel,
                                  String sistema, Agencia agencia, Boolean cancelarEticketsAtivos,
                                  String localizador, String motivo, String reembolso) {
        this.identificacaoAgenciaModel = identificacaoAgenciaModel;
        this.sistema = sistema;
        this.agencia = agencia;
        this.cancelarEticketsAtivos = cancelarEticketsAtivos;
        this.localizador = localizador;
        this.motivo = motivo;
        this.reembolso = reembolso;
    }

    public CancelarReservaRequest(ReservaAereoModel reservaAerea) {

        String motivoCancelamento = reservaAerea.getMotivoCancelamento() != null
                        && !reservaAerea.getMotivoCancelamento().trim().isEmpty()
                        ? reservaAerea.getMotivoCancelamento()
                        : "Desistencia";

        this.identificacaoAgenciaModel = new IdentificacaoAgenciaModel(reservaAerea.getAgencia());
        this.agencia = new Agencia(reservaAerea.getAgencia());

        this.sistema = reservaAerea.getSistema() != null
                        && !reservaAerea.getSistema().trim().isEmpty()
                        ? reservaAerea.getSistema()
                        : "Wooba";

        this.cancelarEticketsAtivos = reservaAerea.getIsCancelarTktsAtivos() != null
                        ? reservaAerea.getIsCancelarTktsAtivos()
                        : false;

        this.localizador = reservaAerea.getLocalizador();
        this.motivo = motivoCancelamento;

        this.reembolso = reservaAerea.getDescricaoMotivoCancelamento() != null
                        && !reservaAerea.getDescricaoMotivoCancelamento().trim().isEmpty()
                        ? reservaAerea.getDescricaoMotivoCancelamento()
                        : motivoCancelamento;
    }
}
