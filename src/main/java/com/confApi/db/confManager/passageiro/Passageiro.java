package com.confApi.db.confManager.passageiro;

import com.confApi.db.confManager.assentoAereo.Assento;
import com.confApi.db.confManager.faturas.dto.model.Bilhete;
import com.confApi.db.confManager.reservaValor.ReservaValor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Passageiro implements Serializable {

    private int codgPassageiro;
    @JsonIgnore
    private int codgReservaAereo;
    private String nomePassageiro;
    private String meioNomePassageiro;
    private String sobrenomePassageiro;
    private int sexo;
    private int tipoPassageiro;
    private String cpf;
    private String telefone;
    private String celular;
    private String numrDocumento;
    private String email;
    private String idPassageiroCia;
    private Date dataNascimento;
    private List<ReservaValor> reservaValores;
    private List<Bilhete> bilhetes;

    public Passageiro(com.confApi.hub.aereo.dto.Passageiro pass) {
        this.codgPassageiro = 0;
        this.codgReservaAereo = 0;
        this.nomePassageiro = pass.getNome();
        this.meioNomePassageiro = pass.getNomeDoMeio();
        this.sobrenomePassageiro = pass.getSobrenome();
        this.sexo = 0;
        this.tipoPassageiro = 0;
        this.cpf = pass.getCpf();
        if (pass.getTelefone() != null) {
            this.telefone = pass.getTelefone().getNumeroTelefone();
            this.celular = pass.getTelefone().getNumeroTelefone();
        }
        this.numrDocumento = pass.getCpf();
        this.email = pass.getEmail();
        this.idPassageiroCia = pass.getIdPassageiro();
        this.dataNascimento = pass.getDataNascimento();
        List<ReservaValor> reservaValores = new ArrayList<>();
        ReservaValor reserva = new ReservaValor();
        reserva.setValorAssento(0.0);
        for (Assento assento : pass.getAssentos()) {
            reserva.setValorAssento(reserva.getValorAssento() + assento.getValor());
        }
        reservaValores.add(reserva);
        this.reservaValores = reservaValores;
        this.bilhetes = null;
    }

    public List<Bilhete> getBilhetes() {
        return bilhetes;
    }

    public void setBilhetes(List<Bilhete> bilhetes) {
        this.bilhetes = bilhetes;
    }

    public Passageiro(int codgPassageiro) {
        this.codgPassageiro = codgPassageiro;
    }

    public Passageiro() {
    }

    public List<ReservaValor> getReservaValores() {
        return reservaValores;
    }

    public void setReservaValores(List<ReservaValor> reservaValores) {
        this.reservaValores = reservaValores;
    }

    public int getCodgPassageiro() {
        return codgPassageiro;
    }

    public void setCodgPassageiro(int codgPassageiro) {
        this.codgPassageiro = codgPassageiro;
    }

    public int getCodgReservaAereo() {
        return codgReservaAereo;
    }

    public void setCodgReservaAereo(int codgReservaAereo) {
        this.codgReservaAereo = codgReservaAereo;
    }

    public String getNomePassageiro() {
        return nomePassageiro;
    }

    public void setNomePassageiro(String nomePassageiro) {
        this.nomePassageiro = nomePassageiro;
    }

    public String getMeioNomePassageiro() {
        return meioNomePassageiro;
    }

    public void setMeioNomePassageiro(String meioNomePassageiro) {
        this.meioNomePassageiro = meioNomePassageiro;
    }

    public String getSobrenomePassageiro() {
        return sobrenomePassageiro;
    }

    public void setSobrenomePassageiro(String sobrenomePassageiro) {
        this.sobrenomePassageiro = sobrenomePassageiro;
    }

    public int getSexo() {
        return sexo;
    }

    public void setSexo(int sexo) {
        this.sexo = sexo;
    }

    public int getTipoPassageiro() {
        return tipoPassageiro;
    }

    public void setTipoPassageiro(int tipoPassageiro) {
        this.tipoPassageiro = tipoPassageiro;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public String getNumrDocumento() {
        return numrDocumento;
    }

    public void setNumrDocumento(String numrDocumento) {
        this.numrDocumento = numrDocumento;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIdPassageiroCia() {
        return idPassageiroCia;
    }

    public void setIdPassageiroCia(String idPassageiroCia) {
        this.idPassageiroCia = idPassageiroCia;
    }

    public Date getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(Date dataNascimento) {
        this.dataNascimento = dataNascimento;
    }



    @Override
    public String toString() {
        return "Passageiro{" + "codgPassageiro=" + codgPassageiro + ", codgReservaAereo=" + codgReservaAereo + ", nomePassageiro=" + nomePassageiro + ", meioNomePassageiro=" + meioNomePassageiro + ", sobrenomePassageiro=" + sobrenomePassageiro + ", sexo=" + sexo + ", tipoPassageiro=" + tipoPassageiro + ", cpf=" + cpf + ", telefone=" + telefone + ", celular=" + celular + ", numrDocumento=" + numrDocumento + ", email=" + email + ", idPassageiroCia=" + idPassageiroCia + ", reservaValores=" + reservaValores + ", bilhetes=" + bilhetes + '}';
    }



}
