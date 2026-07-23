package com.confApi.db.confManager.passageiro;

import com.confApi.db.confManager.assentoAereo.Assento;
import com.confApi.db.confManager.bilhete.BilheteAereo;
import com.confApi.db.confManager.reservaValor.ReservaValor;
import com.confApi.db.confManager.seguro.segurado.SeguroSegurado;
import com.confApi.hub.aereo.BilheteModel;
import com.confApi.hub.aereo.PassageiroModel;
import com.confApi.hub.aereo.ReservaAereoModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Passageiro implements Serializable {

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int codgPassageiro;
    @JsonIgnore
    private int codgReservaAereo;
    private String nomePassageiro;
    private String meioNomePassageiro;
    private String sobrenomePassageiro;
    private Integer sexo;
    private int tipoPassageiro;
    private String cpf;
    private String telefone;
    private String celular;
    private String numrDocumento;
    private String email;
    private String idPassageiroCia;
    private Date dataNascimento;
    private List<ReservaValor> reservaValores;
    private List<BilheteAereo> bilhetes;
    private List<SeguroSegurado> segurados;

    public Passageiro(int codgPassageiro, int codgReservaAereo, String nomePassageiro,
                      String meioNomePassageiro, String sobrenomePassageiro, Integer sexo,
                      int tipoPassageiro, String cpf, String telefone, String celular,
                      String numrDocumento, String email, String idPassageiroCia,
                      Date dataNascimento, List<ReservaValor> reservaValores,
                      List<BilheteAereo> bilhetes, List<SeguroSegurado> segurados) {
        this.codgPassageiro = codgPassageiro;
        this.codgReservaAereo = codgReservaAereo;
        this.nomePassageiro = nomePassageiro;
        this.meioNomePassageiro = meioNomePassageiro;
        this.sobrenomePassageiro = sobrenomePassageiro;
        this.sexo = sexo;
        this.tipoPassageiro = tipoPassageiro;
        this.cpf = cpf;
        this.telefone = telefone;
        this.celular = celular;
        this.numrDocumento = numrDocumento;
        this.email = email;
        this.idPassageiroCia = idPassageiroCia;
        this.dataNascimento = dataNascimento;
        this.reservaValores = reservaValores;
        this.bilhetes = bilhetes;
        this.segurados = segurados;
    }

    public Passageiro(PassageiroModel passageiroModel, ReservaAereoModel reservaAereoModel) {
        this.codgPassageiro = passageiroModel.getCodgPassageiroDb();
        this.codgReservaAereo = reservaAereoModel.getCodgReservaAereoDB().intValue();
        this.nomePassageiro = passageiroModel.getNome();
        this.meioNomePassageiro = passageiroModel.getNomeDoMeio();
        this.sobrenomePassageiro = passageiroModel.getSobrenome();
        this.sexo = passageiroModel.getSexo().equalsIgnoreCase("M") ? 1 : 0;
        String faixaEtaria = passageiroModel.getFaixaEtaria();

        if ("ADT".equalsIgnoreCase(faixaEtaria) || "Adult".equalsIgnoreCase(faixaEtaria)) {
            this.tipoPassageiro = 1;
        } else if ("CHD".equalsIgnoreCase(faixaEtaria) || "Child".equalsIgnoreCase(faixaEtaria)) {
            this.tipoPassageiro = 2;
        } else if ("INF".equalsIgnoreCase(faixaEtaria) || "Infant".equalsIgnoreCase(faixaEtaria)) {
            this.tipoPassageiro = 3;
        } else {
            this.tipoPassageiro = 1;
        }
        this.cpf = passageiroModel.getCpf();
        this.telefone = passageiroModel.getTelefone() != null ? passageiroModel.getTelefone().getNumeroTelefone() : "";
        this.celular = passageiroModel.getTelefone() != null ? passageiroModel.getTelefone().getNumeroTelefone() : "";
        this.numrDocumento = passageiroModel.getDocumento().getNumero();
        this.email = passageiroModel.getEmail();
        this.idPassageiroCia = passageiroModel.getIdPassageiro() != null ? passageiroModel.getIdPassageiro().toString() : "";
        this.dataNascimento = Date.from(
                Instant.parse(passageiroModel.getNascimento())
        );

        if(passageiroModel.getBilhetes() != null && !passageiroModel.getBilhetes().isEmpty()){
            this.bilhetes = new ArrayList<>();
            for(BilheteModel bilheteModel : passageiroModel.getBilhetes()){
                BilheteAereo bilheteAereo = new BilheteAereo(bilheteModel);
                this.bilhetes.add(bilheteAereo);
            }
        } else {
            this.bilhetes = null;
        }

//        this.reservaValores = passageiroModel;
//        this.segurados = passageiroModel;
    }

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

    public List<BilheteAereo> getBilhetes() {
        return bilhetes;
    }

    public void setBilhetes(List<BilheteAereo> bilhetes) {
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

    public Integer getSexo() {
        return sexo;
    }

    public void setSexo(Integer sexo) {
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

    public List<SeguroSegurado> getSegurados() {
        return segurados;
    }

    public void setSegurados(List<SeguroSegurado> segurados) {
        this.segurados = segurados;
    }

    @Override
    public String toString() {
        return "Passageiro{" + "codgPassageiro=" + codgPassageiro + ", codgReservaAereo=" + codgReservaAereo + ", nomePassageiro=" + nomePassageiro + ", meioNomePassageiro=" + meioNomePassageiro + ", sobrenomePassageiro=" + sobrenomePassageiro + ", sexo=" + sexo + ", tipoPassageiro=" + tipoPassageiro + ", cpf=" + cpf + ", telefone=" + telefone + ", celular=" + celular + ", numrDocumento=" + numrDocumento + ", email=" + email + ", idPassageiroCia=" + idPassageiroCia + ", reservaValores=" + reservaValores + ", bilhetes=" + bilhetes + '}';
    }



}
