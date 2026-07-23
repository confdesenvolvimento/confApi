package com.confApi.aereo.dto;

import com.confApi.hub.aereo.BilheteModel;
import com.confApi.hub.aereo.ReservaAereoModel;
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

    public CancelarBilheteRequest(ReservaAereoModel reservaAereoModel, BilheteModel bilheteModel) {
        this.identificacaoAgenciaModel = new IdentificacaoAgenciaModel(reservaAereoModel.getAgencia());
        this.agencia = new Agencia(reservaAereoModel.getAgencia());
        this.cancelarEticketsAtivos = true;
        this.eticket = bilheteModel.getNumeroBilhete();
        this.sistema = reservaAereoModel.getSistema() != null ? reservaAereoModel.getSistema() : "Wooba";
        this.motivo = reservaAereoModel.getMotivoCancelamento() != null ? reservaAereoModel.getMotivoCancelamento() : "Desistencia";
        this.localizador = reservaAereoModel.getLocalizador();
    }

    public CancelarBilheteRequest() {
    }

    public CancelarBilheteRequest(IdentificacaoAgenciaModel identificacaoAgenciaModel, Agencia agencia, Boolean cancelarEticketsAtivos, String eticket, String sistema, String motivo, String localizador) {
        this.identificacaoAgenciaModel = identificacaoAgenciaModel;
        this.agencia = agencia;
        this.cancelarEticketsAtivos = cancelarEticketsAtivos;
        this.eticket = eticket;
        this.sistema = sistema;
        this.motivo = motivo;
        this.localizador = localizador;
    }
}
