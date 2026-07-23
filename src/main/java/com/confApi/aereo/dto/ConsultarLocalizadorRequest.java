package com.confApi.aereo.dto;

import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.hub.aereo.ReservaAereoModel;
import com.confApi.model.IdentificacaoAgenciaModel;
import lombok.Data;

@Data
public class ConsultarLocalizadorRequest {
    private IdentificacaoAgenciaModel identificacaoAgenciaModel;
    private String sistema;
    private Agencia agencia = null;
    private String localizador;

    public ConsultarLocalizadorRequest() {
    }

    public ConsultarLocalizadorRequest(IdentificacaoAgenciaModel identificacaoAgenciaModel, String sistema,
                                       Agencia agencia, String localizador) {
        this.identificacaoAgenciaModel = identificacaoAgenciaModel;
        this.sistema = sistema;
        this.agencia = agencia;
        this.localizador = localizador;
    }

    public ConsultarLocalizadorRequest(ReservaAereoModel reservaAerea) {
        this.identificacaoAgenciaModel = new IdentificacaoAgenciaModel(reservaAerea.getAgencia());
        this.sistema = reservaAerea.getSistema();
        this.agencia = new Agencia(reservaAerea.getAgencia());
        this.localizador = reservaAerea.getLocalizador();
    }

    public ConsultarLocalizadorRequest(ReservaAereo reservaAerea) {
        this.identificacaoAgenciaModel = new IdentificacaoAgenciaModel(reservaAerea.getCodgAgencia());
        this.sistema = reservaAerea.getCodgSistema() != null ? reservaAerea.getCodgSistema().getNomeSistema() : "Wooba";
        this.agencia = new Agencia(reservaAerea.getCodgAgencia());
        this.localizador = reservaAerea.getLocalizador();
    }


}
