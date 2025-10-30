package com.confApi.db.confManager.alertaTarifa.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Data
public class AlertaTarifaLogDTO implements Serializable {
    private Integer codgAlertaTarifaLog;
    private Integer alerta;
    private Date dataTarifa;
    private Double valorEncontrado;
    private Date dataProcessamento;
    private Integer canal;
    private Integer status;
    private String mensagem;
    private Double percentualDesconto =0.0;
    private Integer flagNotificado;

    public AlertaTarifaLogDTO() {
    }

    public AlertaTarifaLogDTO(Date dataTarifa, Double valorEncontrado, Date dataProcessamento, Integer canal, Integer status, String mensagem) {
        this.dataTarifa = dataTarifa;
        this.valorEncontrado = valorEncontrado;
        this.dataProcessamento = dataProcessamento;
        this.canal = canal;
        this.status = status;
        this.mensagem = mensagem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlertaTarifaLogDTO that)) return false;
        return Objects.equals(getCodgAlertaTarifaLog(), that.getCodgAlertaTarifaLog()) && Objects.equals(getAlerta(), that.getAlerta()) && Objects.equals(getDataTarifa(), that.getDataTarifa()) && Objects.equals(getValorEncontrado(), that.getValorEncontrado()) && Objects.equals(getDataProcessamento(), that.getDataProcessamento()) && Objects.equals(getCanal(), that.getCanal()) && Objects.equals(getStatus(), that.getStatus()) && Objects.equals(getMensagem(), that.getMensagem()) && Objects.equals(getPercentualDesconto(), that.getPercentualDesconto()) && Objects.equals(getFlagNotificado(), that.getFlagNotificado());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCodgAlertaTarifaLog(), getAlerta(), getDataTarifa(), getValorEncontrado(), getDataProcessamento(), getCanal(), getStatus(), getMensagem(), getPercentualDesconto(), getFlagNotificado());
    }
}
