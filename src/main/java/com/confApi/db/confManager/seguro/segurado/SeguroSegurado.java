package com.confApi.db.confManager.seguro.segurado;

import com.confApi.db.confManager.passageiro.Passageiro;
import com.confApi.db.confManager.seguro.apolice.SeguroApolice;
import com.confApi.db.confManager.seguro.cobertura.SeguroCobertura;
import com.confApi.seguros.dto.PlanoSeguroDTO;
import com.confApi.seguros.dto.SeguradoDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class SeguroSegurado {

    private Integer codgSeguroSegurado;
    private SeguroCobertura codgSeguroCobertura;
    private Passageiro codgPassageiro;
    private String nomeCompleto;
    private String primeiroNome;
    private String ultimoNome;
    private Integer sexo;
    private String cpf;
    private Date dataNascimento;
    private String estadoCivil;
    private String telefone;
    private String email;
    private String enderecoCep;
    private String enderecoEndereco;
    private String enderecoNumero;
    private String enderecoComplemento;
    private String enderecoBairro;
    private String enderecoCidade;
    private String enderecoEstado;
    private String localizador;
    private List<SeguroApolice> apoliceList = new ArrayList<>();

//    private SeguroReserva codgReservaSeguro;

    public SeguroSegurado() {
    }

    public SeguroSegurado(Integer codgSeguroSegurado, SeguroCobertura codgSeguroCobertura,
                          Passageiro codgPassageiro, String nomeCompleto, String primeiroNome,
                          String ultimoNome, Integer sexo, String cpf, Date dataNascimento,
                          String estadoCivil, String telefone, String email, String enderecoCep,
                          String enderecoEndereco, String enderecoNumero, String enderecoComplemento,
                          String enderecoBairro, String enderecoCidade, String enderecoEstado,
                          String localizador, List<SeguroApolice> apoliceList) {
        this.codgSeguroSegurado = codgSeguroSegurado;
        this.codgSeguroCobertura = codgSeguroCobertura;
        this.codgPassageiro = codgPassageiro;
        this.nomeCompleto = nomeCompleto;
        this.primeiroNome = primeiroNome;
        this.ultimoNome = ultimoNome;
        this.sexo = sexo;
        this.cpf = cpf;
        this.dataNascimento = dataNascimento;
        this.estadoCivil = estadoCivil;
        this.telefone = telefone;
        this.email = email;
        this.enderecoCep = enderecoCep;
        this.enderecoEndereco = enderecoEndereco;
        this.enderecoNumero = enderecoNumero;
        this.enderecoComplemento = enderecoComplemento;
        this.enderecoBairro = enderecoBairro;
        this.enderecoCidade = enderecoCidade;
        this.enderecoEstado = enderecoEstado;
        this.localizador = localizador;
        this.apoliceList = apoliceList;
    }

    public SeguroSegurado(SeguradoDTO seguradoDTO, PlanoSeguroDTO planoSeguroDTO) {
        this.codgSeguroSegurado = null;
        this.codgSeguroCobertura = new SeguroCobertura(planoSeguroDTO);
        this.nomeCompleto = seguradoDTO.getNome() + " " + seguradoDTO.getSobrenome();
        this.primeiroNome = seguradoDTO.getNome();
        this.ultimoNome = seguradoDTO.getSobrenome();
        this.sexo = seguradoDTO.getSexo().contains("F") ? 2 : 1;
        this.cpf = seguradoDTO.getCpf();
        this.dataNascimento = java.sql.Date.valueOf(seguradoDTO.getNascimento());
        this.estadoCivil = seguradoDTO.getEstadoCivil() != null ? seguradoDTO.getEstadoCivil().getDescricao() : null;
        this.telefone = seguradoDTO.getTelefone();
        this.email = seguradoDTO.getEmail();
        this.enderecoCep = seguradoDTO.getCep();
        this.enderecoEndereco = seguradoDTO.getEndereco();
        this.enderecoNumero = seguradoDTO.getNumero();
        this.enderecoComplemento = seguradoDTO.getComplemento();
        this.enderecoBairro = seguradoDTO.getBairro();
        this.enderecoCidade = seguradoDTO.getCidade();
        this.enderecoEstado = seguradoDTO.getUf();
        this.localizador = null;
    }
}
