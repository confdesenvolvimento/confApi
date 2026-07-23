package com.confApi.hub.aereo;

import com.confApi.aereo.dto.Reserva;
import com.confApi.endPoints.bilhete.BilheteResponse;
import com.confApi.endPoints.passageiro.PassageiroResponse;
import com.confApi.endPoints.reservaValoresAereos.ReservaValoresAereoResponse;
import com.confApi.hub.aereo.dto.Bilhete;
import com.confApi.hub.aereo.dto.DocumentoPassageiro;
import com.confApi.hub.aereo.dto.Passageiro;

import java.io.Serializable;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.time.LocalDate;

public class PassageiroModel implements Serializable {

    private Integer codgPassageiroDb;
    private String cpf;
    private DocumentoPassageiro documento = new DocumentoPassageiro();
    private String email;
    private String faixaEtaria;
    private String nascimento;
    private String nome;
    private String nomeDoMeio;
    private String sobrenome;
    private PassaporteModel passaporte = new PassaporteModel();
    private String sexo;
    private ContatoModel telefone = new ContatoModel();
    private String voeBiz;
    private String idPassageiro;
    private List<ReservaValoresAereo> valores;
    private List<BilheteModel> bilhetes;

    public PassageiroModel(Passageiro passageiro, Reserva reservaApi) {
        if (passageiro == null) {
            return;
        }

        this.nome = passageiro.getNome();
        this.nomeDoMeio = passageiro.getNomeDoMeio();
        this.sobrenome = passageiro.getSobrenome();
        this.faixaEtaria = passageiro.getFaixaEtaria();
        this.email = passageiro.getEmail();
        this.telefone = new ContatoModel(passageiro.getTelefone());
        this.sexo = passageiro.getSexo();
        this.cpf = passageiro.getCpf();
        this.passaporte = new PassaporteModel(passageiro.getPassaporte());
        this.voeBiz = passageiro.getVoeBiz();
        this.idPassageiro = passageiro.getIdPassageiro();
        this.nascimento = passageiro.getNascimento();

        if (passageiro.getDocumento() != null) {
            this.documento = passageiro.getDocumento();
        }

        if (reservaApi.getValorReserva() != null) {
            this.valores = new ArrayList<>();
            this.valores.add(new ReservaValoresAereo(reservaApi.getValorReserva().getValorBase().getValorPassageiroList().get(0)));
        }

        if (passageiro.getBilhetes() != null) {
            this.bilhetes = new ArrayList<>();
            for (Bilhete bilhete : passageiro.getBilhetes()) {
                this.bilhetes.add(new BilheteModel(bilhete));
            }
        }
    }

    public Date converterData(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }

        LocalDate localDate;

        if (data.matches("\\d{2}/\\d{2}/\\d{4}")) {
            localDate = LocalDate.parse(data, DateTimeFormatter.ofPattern("dd/MM/yyyy")
            );
        } else if (data.matches("\\d{4}-\\d{2}-\\d{2}")) {
            localDate = LocalDate.parse(data, DateTimeFormatter.ISO_LOCAL_DATE);
        } else {
            throw new IllegalArgumentException("Formato de data não suportado: " + data);
        }

        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public PassageiroModel(PassageiroResponse passageiroResponse) {
        this.codgPassageiroDb = passageiroResponse.getCodgPassageiroDb();
        this.cpf = passageiroResponse.getCpf();
        this.documento = new DocumentoPassageiro(passageiroResponse.getDocumento());
        this.email = passageiroResponse.getEmail();
        this.faixaEtaria = passageiroResponse.getFaixaEtaria();
        this.nascimento = passageiroResponse.getNascimento();
        this.nome = passageiroResponse.getNome();
        this.nomeDoMeio = passageiroResponse.getNomeDoMeio();
        this.sobrenome = passageiroResponse.getSobrenome();
        this.passaporte = new PassaporteModel(passageiroResponse.getPassaporte());
        this.sexo = passageiroResponse.getSexo();
        this.telefone = new ContatoModel(passageiroResponse.getTelefone());
        this.voeBiz = passageiroResponse.getVoeBiz();
        this.idPassageiro = passageiroResponse.getIdPassageiro();
        this.valores = new ArrayList<>();
        if(passageiroResponse.getValores() != null) {
            for (ReservaValoresAereoResponse reservaValoresAereoResponse : passageiroResponse.getValores()) {
                this.valores.add(new ReservaValoresAereo(reservaValoresAereoResponse));
            }
        }
        this.bilhetes = new ArrayList<>();
        if(passageiroResponse.getBilhetes() != null){
            for(BilheteResponse bilheteResponse : passageiroResponse.getBilhetes()){
                this.bilhetes.add(new BilheteModel(bilheteResponse));
            }
        }
    }

    public PassageiroModel(String faixaEtaria) {
        this.faixaEtaria = faixaEtaria;
    }

    public PassageiroModel(String faixaEtaria, String email) {
        this.email = email;
        this.faixaEtaria = faixaEtaria;
    }

    public PassageiroModel() {
    }

    public PassageiroModel(String cpf, DocumentoPassageiro documento, String email, String faixaEtaria,
                           String nascimento, String nome, String nomeDoMeio, String sobrenome,
                           PassaporteModel passaporte, String sexo, ContatoModel telefone,
                           String voeBiz, String idPassageiro) {
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
        this.idPassageiro = idPassageiro;
    }

    public Integer getCodgPassageiroDb() {
        return codgPassageiroDb;
    }

    public void setCodgPassageiroDb(Integer codgPassageiroDb) {
        this.codgPassageiroDb = codgPassageiroDb;
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

    public PassaporteModel getPassaporte() {
        return passaporte;
    }

    public void setPassaporte(PassaporteModel passaporte) {
        this.passaporte = passaporte;
    }

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public ContatoModel getTelefone() {
        return telefone;
    }

    public void setTelefone(ContatoModel telefone) {
        this.telefone = telefone;
    }

    public String getVoeBiz() {
        return voeBiz;
    }

    public void setVoeBiz(String voeBiz) {
        this.voeBiz = voeBiz;
    }

    public String getIdPassageiro() {
        return idPassageiro;
    }

    public void setIdPassageiro(String idPassageiro) {
        this.idPassageiro = idPassageiro;
    }

    public List<ReservaValoresAereo> getValores() {
        return valores;
    }

    public void setValores(List<ReservaValoresAereo> valores) {
        this.valores = valores;
    }

    public List<BilheteModel> getBilhetes() {
        return bilhetes;
    }

    public void setBilhetes(List<BilheteModel> bilhetes) {
        this.bilhetes = bilhetes;
    }

    @Override
    public String toString() {
        return "PassageiroModel{" +
                "codgPassageiroDb=" + codgPassageiroDb +
                ", cpf='" + cpf + '\'' +
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
                ", bilhetes=" + bilhetes +
                '}';
    }
}
