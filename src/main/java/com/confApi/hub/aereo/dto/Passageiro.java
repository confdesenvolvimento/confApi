package com.confApi.hub.aereo.dto;

import com.confApi.aereo.dto.PreReserva;
import com.confApi.db.confManager.assentoAereo.Assento;
import com.confApi.hub.aereo.PassageiroModel;
import com.confApi.hub.aereo.PassaporteModel;

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

    public Passageiro(PassageiroModel passageiroModel, PreReserva preReserva) {
        if (passageiroModel.getDocumento() != null) {
            passageiroModel.getDocumento().setNacionalidade("BR");
            passageiroModel.getDocumento().setPaisEmissor("BR");

            if (passageiroModel.getCpf() != null && !passageiroModel.getCpf().isBlank()) {
                passageiroModel.getDocumento().setNumero(passageiroModel.getCpf());
                passageiroModel.getDocumento().setTipo(1);
            } else if (passageiroModel.getPassaporte() != null) {
                passageiroModel.getDocumento().setTipo(2);
                passageiroModel.getDocumento().setNumero(passageiroModel.getPassaporte().getNumero());
                passageiroModel.setPassaporte(new PassaporteModel(passageiroModel.getPassaporte().getNumero()));
            }
        }

        if (passageiroModel.getTelefone() != null && passageiroModel.getTelefone().getNumeroTelefone() != null) {

            String telefoneRecebido = passageiroModel.getTelefone()
                    .getNumeroTelefone()
                    .replace("(", "")
                    .replace(")", "")
                    .replaceAll("[^0-9]", "");

            if (telefoneRecebido.length() >= 10) {
                String ddd = telefoneRecebido.substring(0, 2);
                String numero = telefoneRecebido.substring(2);

                passageiroModel.getTelefone().setCidade("Cuiaba");
                passageiroModel.getTelefone().setEmail(passageiroModel.getEmail());

                passageiroModel.getTelefone().setNumeroDDI("55");
                passageiroModel.getTelefone().setNumeroDDD(ddd);
                passageiroModel.getTelefone().setNumeroTelefone(numero);

                String endereco = "AVENIDA SAO SEBASTIAO";

                if (preReserva != null
                        && preReserva.getUsuario() != null
                        && preReserva.getUsuario().getAgencia() != null
                        && preReserva.getUsuario().getAgencia().getEndereco() != null
                        && !preReserva.getUsuario().getAgencia().getEndereco().isBlank()) {

                    endereco = preReserva.getUsuario().getAgencia().getEndereco();
                }

                passageiroModel.getTelefone().setEndereco(endereco);
                passageiroModel.getTelefone().setNome(passageiroModel.getNome());
            }
        }

        this.cpf = passageiroModel.getCpf();
        this.assentos = null;
        this.documento = passageiroModel.getDocumento();
        this.email = passageiroModel.getEmail();
        this.faixaEtaria = passageiroModel.getFaixaEtaria();
        this.nascimento = convertDate(passageiroModel.getNascimento());
        this.nome = passageiroModel.getNome();
        this.nomeDoMeio = passageiroModel.getNomeDoMeio();
        this.sobrenome = passageiroModel.getSobrenome();
        this.passaporte = null;
        this.sexo = passageiroModel.getSexo();
        this.telefone = passageiroModel.getTelefone();
        this.voeBiz = passageiroModel.getVoeBiz();
        this.idPassageiro = passageiroModel.getIdPassageiro();
        this.dataNascimento = passageiroModel.converterData(passageiroModel.getNascimento());
        this.bilhetes = null;
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

    public String convertDate(String dateInput) {
        System.err.println("entrou em data convert : " + dateInput);

        if (dateInput == null || dateInput.isEmpty()) {
            return null;
        }

        // Converte dd/MM/yyyy para yyyy-MM-dd
        String date = dateInput.substring(6, 10) + "-"
                + dateInput.substring(3, 5) + "-"
                + dateInput.substring(0, 2);

        // Adiciona hora fixa para evitar problemas de fuso
        return date + "T12:00:00";
    }

    @Override
    public String toString() {
        return "Passageiro{" +
                "cpf='" + cpf + '\'' +
                ", assentos=" + assentos +
                ", documento=" + documento +
                ", email='" + email + '\'' +
                ", faixaEtaria='" + faixaEtaria + '\'' +
                ", nascimento='" + nascimento + '\'' +
                ", nome='" + nome + '\'' +
                ", nomeDoMeio='" + nomeDoMeio + '\'' +
                ", sobrenome='" + sobrenome + '\'' +
                ", passaporte=" + passaporte +
                ", sexo='" + sexo + '\'' +
                ", telefone=" + telefone +
                ", voeBiz='" + voeBiz + '\'' +
                ", idPassageiro='" + idPassageiro + '\'' +
                ", dataNascimento=" + dataNascimento +
                ", bilhetes=" + bilhetes +
                '}';
    }
}

