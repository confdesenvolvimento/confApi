package com.confApi.aereo.dto;

import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.hub.aereo.ReservaAereoModel;
import lombok.Data;

import java.sql.Timestamp;
import java.util.Date;

@Data
public class ReservaAereoCancelamentoDto {
    private Integer codgReservaAereo;
    private Timestamp dataCancelamento;
    private Timestamp dataEmissao;
    private String descMotivoCancelamento;
    private Integer codgUsuarioCancelamento;

    public ReservaAereoCancelamentoDto() {
    }

    public ReservaAereoCancelamentoDto(Integer codgReservaAereo, Timestamp dataCancelamento,
                                       Timestamp dataEmissao, String descMotivoCancelamento,
                                       Integer codgUsuarioCancelamento) {
        this.codgReservaAereo = codgReservaAereo;
        this.dataCancelamento = dataCancelamento;
        this.dataEmissao = dataEmissao;
        this.descMotivoCancelamento = descMotivoCancelamento;
        this.codgUsuarioCancelamento = codgUsuarioCancelamento;
    }

    public ReservaAereoCancelamentoDto(ReservaAereoModel reservaAereoModel, Usuario usuario) {
        this.codgReservaAereo = Math.toIntExact(reservaAereoModel.getCodgReservaAereoDB());
        this.dataCancelamento = reservaAereoModel.getDataCancelamento() != null
                ? new Timestamp(reservaAereoModel.getDataCancelamento().getTime())
                : null;
        this.dataEmissao = reservaAereoModel.getDataEmissao() != null
                ? new Timestamp(reservaAereoModel.getDataEmissao().getTime())
                : null;
        this.descMotivoCancelamento = reservaAereoModel.getDescMotivoCancelamento();
        this.codgUsuarioCancelamento = Math.toIntExact(Long.valueOf(usuario.getCodgUsuario()));
    }

    public Integer getCodgReservaAereo() {
        return codgReservaAereo;
    }

    public void setCodgReservaAereo(Integer codgReservaAereo) {
        this.codgReservaAereo = codgReservaAereo;
    }

    public Timestamp getDataCancelamento() {
        return dataCancelamento;
    }

    public void setDataCancelamento(Timestamp dataCancelamento) {
        this.dataCancelamento = dataCancelamento;
    }

    public Timestamp getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(Timestamp dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public String getDescMotivoCancelamento() {
        return descMotivoCancelamento;
    }

    public void setDescMotivoCancelamento(String descMotivoCancelamento) {
        this.descMotivoCancelamento = descMotivoCancelamento;
    }

    public Integer getCodgUsuarioCancelamento() {
        return codgUsuarioCancelamento;
    }

    public void setCodgUsuarioCancelamento(Integer codgUsuarioCancelamento) {
        this.codgUsuarioCancelamento = codgUsuarioCancelamento;
    }
}
