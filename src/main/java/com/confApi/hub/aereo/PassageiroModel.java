package com.confApi.hub.aereo;

import com.confApi.endPoints.bilhete.BilheteResponse;
import com.confApi.endPoints.passageiro.PassageiroResponse;
import com.confApi.endPoints.reservaValoresAereos.ReservaValoresAereoResponse;
import com.confApi.hub.aereo.dto.DocumentoPassageiro;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    public Date converterData(String data) {
        String timestampString = data.substring(data.indexOf("(") + 1, data.indexOf("-"));
        if (!timestampString.equalsIgnoreCase("")) {
            String offsetString = data.substring(data.indexOf("-") + 1, data.indexOf(")"));
            long timestamp = Long.parseLong(timestampString);
            int offsetHours = Integer.parseInt(offsetString.substring(0, 3));
            int offsetMinutes = Integer.parseInt(offsetString.substring(3));

            // Calcular o deslocamento em milissegundos
            int offsetMilliseconds = (offsetHours * 60 + offsetMinutes) * 60 * 1000;

            // Somar o deslocamento ao timestamp
            Date date = new Date(timestamp + offsetMilliseconds);

            // Imprimir a data convertida
            return date;
        } else {
            return null;
        }
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
        return "Passageiro{"
                + "cpf='" + cpf + '\''
                + ", documento=" + documento
                + ", email='" + email + '\''
                + ", faixaEtaria='" + faixaEtaria + '\''
                + ", nascimento=" + nascimento
                + ", nome='" + nome + '\''
                + ", nomeDoMeio='" + nomeDoMeio + '\''
                + ", sobrenome='" + sobrenome + '\''
                + ", passaporte=" + passaporte
                + ", sexo='" + sexo + '\''
                + ", telefone=" + telefone
                + ", voeBiz='" + voeBiz + '\''
                + ", idPassageiro='" + idPassageiro + '\''
                + '}';
    }
}
