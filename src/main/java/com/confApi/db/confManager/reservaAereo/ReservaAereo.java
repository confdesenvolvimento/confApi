package com.confApi.db.confManager.reservaAereo;

import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.companhiaAerea.CompanhiaAerea;
import com.confApi.db.confManager.passageiro.Passageiro;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.db.confManager.reservaPacote.ReservaPacote;
import com.confApi.db.confManager.sistema.Sistema;
import com.confApi.db.confManager.trecho.Trecho;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.hub.aereo.ReservaAereoModel;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReservaAereo implements Serializable {

    private int codgReservaAereo;
    private Usuario codgUsuarioCriacao;
    private Date dataCriacao;
    private Date dataEmissao;
    private Date dataLimiteEmissao;
    private Date dataCancelamento;
    private String localizador;
    private Integer status;
    private String descMotivoCancelamento;
    private String regraReserva;
    private Sistema codgSistema;
    private CompanhiaAerea codgCompanhiaAerea;
    private Agencia codgAgencia;
    private Integer codgUsuarioEmissao;
    private Integer codgUsuarioCancelamento;
    private ReservaPacote codgReservaPacote;
    private List<Trecho> trechos;
    private List<Passageiro> passageiros;
    private List<Recebimento> recebimentos;

    private Double valorTotalReserva =0.0;

    public ReservaAereo(ReservaAereoModel obj) {
        this.codgReservaAereo = obj.getCodgReservaAereoDB().intValue();
        this.codgUsuarioCriacao = null;
        this.dataCriacao = obj.getDataCriacao();
        this.dataEmissao = obj.getDataEmissao();
        this.dataLimiteEmissao = null;
        this.dataCancelamento = null;
        this.localizador = obj.getLocalizador();
        this.status = 0;
        this.descMotivoCancelamento = obj.getDescMotivoCancelamento();
        this.regraReserva = obj.getRegraReserva();
        this.codgSistema = new Sistema(obj.getSistema(), null);
        this.codgCompanhiaAerea = null;
        this.codgAgencia = null;
        this.codgUsuarioEmissao = null;
        this.codgUsuarioCancelamento = null;
        this.trechos = null;
        this.passageiros = null;
        this.recebimentos = null;
        this.codgReservaPacote = obj.getReservaPacote();
    }



    public List<Recebimento> getRecebimentos() {
        return recebimentos;
    }

    public void setRecebimentos(List<Recebimento> recebimentos) {
        this.recebimentos = recebimentos;
    }

    public ReservaAereo(String localizador) {
        this.localizador = localizador;
    }




    public ReservaAereo(int codgReservaAereo) {
        this.codgReservaAereo = codgReservaAereo;
    }

    public ReservaAereo() {
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ReservaAereo{");
        sb.append("codgReservaAereo=").append(codgReservaAereo);
        sb.append(", codgUsuarioCriacao=").append(codgUsuarioCriacao);
        sb.append(", dataCriacao=").append(dataCriacao);
        sb.append(", dataEmissao=").append(dataEmissao);
        sb.append(", dataLimiteEmissao=").append(dataLimiteEmissao);
        sb.append(", dataCancelamento=").append(dataCancelamento);
        sb.append(", localizador=").append(localizador);
        sb.append(", status=").append(status);
        sb.append(", descMotivoCancelamento=").append(descMotivoCancelamento);
        sb.append(", regraReserva=").append(regraReserva);
        sb.append(", codgSistema=").append(codgSistema);
        sb.append(", codgCompanhiaAerea=").append(codgCompanhiaAerea);
        sb.append(", codgAgencia=").append(codgAgencia);
        sb.append(", codgUsuarioEmissao=").append(codgUsuarioEmissao);
        sb.append(", codgUsuarioCancelamento=").append(codgUsuarioCancelamento);
        sb.append(", trechos=").append(trechos);
        sb.append(", passageiros=").append(passageiros);
        sb.append(", recebimentos=").append(recebimentos);
        sb.append('}');
        return sb.toString();
    }

    public Double getValorTotalReserva() {
        return valorTotalReserva;
    }

    public void setValorTotalReserva(Double valorTotalReserva) {
        this.valorTotalReserva = valorTotalReserva;
    }






    public List<Passageiro> getPassageiros() {
        return passageiros;
    }

    public void setPassageiros(List<Passageiro> passageiros) {
        this.passageiros = passageiros;
    }


    public List<Trecho> getTrechos() {
        return trechos;
    }

    public void setTrechos(List<Trecho> trechos) {
        this.trechos = trechos;
    }

    public int getCodgReservaAereo() {
        return codgReservaAereo;
    }

    public void setCodgReservaAereo(int codgReservaAereo) {
        this.codgReservaAereo = codgReservaAereo;
    }

    public Usuario getCodgUsuarioCriacao() {
        return codgUsuarioCriacao;
    }

    public void setCodgUsuarioCriacao(Usuario codgUsuarioCriacao) {
        this.codgUsuarioCriacao = codgUsuarioCriacao;
    }

    public Date getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(Date dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public Date getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(Date dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public Date getDataLimiteEmissao() {
        return dataLimiteEmissao;
    }

    public void setDataLimiteEmissao(Date dataLimiteEmissao) {
        this.dataLimiteEmissao = dataLimiteEmissao;
    }

    public Date getDataCancelamento() {
        return dataCancelamento;
    }

    public void setDataCancelamento(Date dataCancelamento) {
        this.dataCancelamento = dataCancelamento;
    }

    public String getLocalizador() {
        return localizador;
    }

    public void setLocalizador(String localizador) {
        this.localizador = localizador;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDescMotivoCancelamento() {
        return descMotivoCancelamento;
    }

    public void setDescMotivoCancelamento(String descMotivoCancelamento) {
        this.descMotivoCancelamento = descMotivoCancelamento;
    }

    public String getRegraReserva() {
        return regraReserva;
    }

    public void setRegraReserva(String regraReserva) {
        this.regraReserva = regraReserva;
    }

    public Sistema getCodgSistema() {
        return codgSistema;
    }

    public void setCodgSistema(Sistema codgSistema) {
        this.codgSistema = codgSistema;
    }

    public CompanhiaAerea getCodgCompanhiaAerea() {
        return codgCompanhiaAerea;
    }

    public void setCodgCompanhiaAerea(CompanhiaAerea codgCompanhiaAerea) {
        this.codgCompanhiaAerea = codgCompanhiaAerea;
    }

    public Agencia getCodgAgencia() {
        return codgAgencia;
    }

    public void setCodgAgencia(Agencia codgAgencia) {
        this.codgAgencia = codgAgencia;
    }

    public Integer getCodgUsuarioEmissao() {
        return codgUsuarioEmissao;
    }

    public void setCodgUsuarioEmissao(Integer codgUsuarioEmissao) {
        this.codgUsuarioEmissao = codgUsuarioEmissao;
    }

    public Integer getCodgUsuarioCancelamento() {
        return codgUsuarioCancelamento;
    }

    public void setCodgUsuarioCancelamento(Integer codgUsuarioCancelamento) {
        this.codgUsuarioCancelamento = codgUsuarioCancelamento;
    }

    public ReservaPacote getCodgReservaPacote() {
        return codgReservaPacote;
    }

    public void setCodgReservaPacote(ReservaPacote codgReservaPacote) {
        this.codgReservaPacote = codgReservaPacote;
    }





}

