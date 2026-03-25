package com.confApi.db.confManager.seguro.segurado;

import com.confApi.db.confManager.seguro.apolice.SeguroApolice;
import com.confApi.db.confManager.seguro.cobertura.SeguroCobertura;
import com.confApi.seguros.dto.PlanoSeguroDTO;
import com.confApi.seguros.dto.SeguradoDTO;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class SeguroSegurado {

    private Integer codgSeguroSegurado;
    private SeguroCobertura seguroCobertura;
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

    public SeguroSegurado() {
    }

    public SeguroSegurado(SeguradoDTO seguradoDTO, PlanoSeguroDTO planoSeguroDTO) {
        this.codgSeguroSegurado = null;
        this.seguroCobertura = new SeguroCobertura(planoSeguroDTO);
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
