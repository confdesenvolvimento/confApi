package com.confApi.db.confManager.seguro.segurado;

import com.confApi.db.confManager.seguro.cobertura.SeguroCobertura;
import lombok.Data;
import java.util.Date;

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
}
