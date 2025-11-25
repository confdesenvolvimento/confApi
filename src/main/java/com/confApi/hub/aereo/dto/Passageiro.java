package com.confApi.hub.aereo.dto;

import com.confApi.db.confManager.assentoAereo.Assento;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Passageiro implements Serializable {

    private String cpf;
    private List<Assento> assentos;
    private DocumentoPassageiro documento;
    private String email;
    private String faixaEtaria;
    private String nascimento;
    private String nome;
    private String nomeDoMeio;
    private String sobrenome;
    private Passaporte passaporte;
    private String sexo;
    private Contato telefone;
    private String voeBiz;
    private String idPassageiro;
    private Date dataNascimento;
    private List<Bilhete> bilhetes;

    public Passageiro() {
    }

    public Passageiro(String cpf, DocumentoPassageiro documento, String email,
                      String faixaEtaria, String nascimento, String nome,
                      String nomeDoMeio, String sobrenome, Passaporte passaporte,
                      String sexo,
                      Contato telefone, String voeBiz) {
        this.cpf = cpf;
        this.documento = documento;
        this.email = email;
        this.faixaEtaria = faixaEtaria;
        this.nascimento = nascimento;
        this.nome = nome;
        this.nomeDoMeio = nomeDoMeio;
        this.sobrenome = sobrenome;
        this.passaporte = passaporte;
        this.sexo = sexo;
        this.telefone = telefone;
        this.voeBiz = voeBiz;
    }

    public Passageiro(String cpf, List<Assento> assentos, DocumentoPassageiro documento, String email,
                      String faixaEtaria, String nascimento, String nome, String nomeDoMeio, String sobrenome,
                      Passaporte passaporte, String sexo, Contato telefone, String voeBiz) {
        this.cpf = cpf;
        this.assentos = assentos;
        this.documento = documento;
        this.email = email;
        this.faixaEtaria = faixaEtaria;
        this.nascimento = nascimento;
        this.nome = nome;
        this.nomeDoMeio = nomeDoMeio;
        this.sobrenome = sobrenome;
        this.passaporte = passaporte;
        this.sexo = sexo;
        this.telefone = telefone;
        this.voeBiz = voeBiz;
    }

    public Passageiro(String cpf, List<Assento> assentos, DocumentoPassageiro documento,
                      String email, String faixaEtaria, String nascimento, String nome, String nomeDoMeio,
                      String sobrenome, Passaporte passaporte, String sexo, Contato telefone, String voeBiz,
                      String idPassageiro, List<Bilhete> bilhetes) {
        this.cpf = cpf;
        this.assentos = assentos;
        this.documento = documento;
        this.email = email;
        this.faixaEtaria = faixaEtaria;
        this.nascimento = nascimento;
        this.nome = nome;
        this.nomeDoMeio = nomeDoMeio;
        this.sobrenome = sobrenome;
        this.passaporte = passaporte;
        this.sexo = sexo;
        this.telefone = telefone;
        this.voeBiz = voeBiz;
        this.idPassageiro = idPassageiro;
        this.bilhetes = bilhetes;
    }

    public Passageiro(String cpf, List<Assento> assentos, DocumentoPassageiro documento,
                      String email, String faixaEtaria, String nascimento, String nome, String nomeDoMeio,
                      String sobrenome, Passaporte passaporte, String sexo, Contato telefone, String voeBiz,
                      String idPassageiro, Date dataNascimento, List<Bilhete> bilhetes) {
        this.cpf = cpf;
        this.assentos = assentos;
        this.documento = documento;
        this.email = email;
        this.faixaEtaria = faixaEtaria;
        this.nascimento = nascimento;
        this.nome = nome;
        this.nomeDoMeio = nomeDoMeio;
        this.sobrenome = sobrenome;
        this.passaporte = passaporte;
        this.sexo = sexo;
        this.telefone = telefone;
        this.voeBiz = voeBiz;
        this.idPassageiro = idPassageiro;
        this.dataNascimento = dataNascimento;
        this.bilhetes = bilhetes;
    }

    public List<Bilhete> getBilhetes() {
        return bilhetes;
    }

    public void setBilhetes(List<Bilhete> bilhetes) {
        this.bilhetes = bilhetes;
    }

    public String getIdPassageiro() {
        return idPassageiro;
    }

    public void setIdPassageiro(String idPassageiro) {
        this.idPassageiro = idPassageiro;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public DocumentoPassageiro getDocumento() {
        return documento;
    }

    public void setDocumento(DocumentoPassageiro documento) {
        this.documento = documento;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFaixaEtaria() {
        return faixaEtaria;
    }

    public void setFaixaEtaria(String faixaEtaria) {
        this.faixaEtaria = faixaEtaria;
    }

    public String getNascimento() {
        return nascimento;
    }

    public void setNascimento(String nascimento) {
        this.nascimento = nascimento;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNomeDoMeio() {
        return nomeDoMeio;
    }

    public void setNomeDoMeio(String nomeDoMeio) {
        this.nomeDoMeio = nomeDoMeio;
    }

    public String getSobrenome() {
        return sobrenome;
    }

    public void setSobrenome(String sobrenome) {
        this.sobrenome = sobrenome;
    }

    public Passaporte getPassaporte() {
        return passaporte;
    }

    public void setPassaporte(Passaporte passaporte) {
        this.passaporte = passaporte;
    }

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public Contato getTelefone() {
        return telefone;
    }

    public void setTelefone(Contato telefone) {
        this.telefone = telefone;
    }

    public String getVoeBiz() {
        return voeBiz;
    }

    public void setVoeBiz(String voeBiz) {
        this.voeBiz = voeBiz;
    }

    public List<Assento> getAssentos() {
        return assentos;
    }

    public void setAssentos(List<Assento> assentos) {
        this.assentos = assentos;
    }

    public Date getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(Date dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    @Override
    public String toString() {
        return "Passageiro{" + "email=" + email + ", faixaEtaria=" + faixaEtaria + ", nascimento=" + nascimento + ", nome=" + nome + ", nomeDoMeio=" + nomeDoMeio + ", sobrenome=" + sobrenome + ", sexo=" + sexo + ", telefone=" + telefone + ", voeBiz=" + voeBiz + ", idPassageiro=" + idPassageiro + ", dataNascimento=" + dataNascimento + '}';
    }

}

