package com.confApi.aereo.dto;

import com.confApi.model.IdentificacaoAgenciaModel;
import lombok.Data;

@Data
public class CancelarBilheteRequest {
    private String tipoTarifa = "CONS";
    private Integer codgReservaAereo;

    public String getTipoTarifa() { return tipoTarifa; }
    public void setTipoTarifa(String valor) { tipoTarifa = valor; }
    public Integer getCodgReservaAereo() { return codgReservaAereo; }
    public void setCodgReservaAereo(Integer valor) { codgReservaAereo = valor; }
    private IdentificacaoAgenciaModel identificacaoAgenciaModel;
    private Agencia agencia;
    private Boolean cancelarEticketsAtivos;
    private String eticket;
    private String sistema;
    private String motivo;
    private String localizador;
}
