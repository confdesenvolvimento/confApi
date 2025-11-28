package com.confApi.hub.aereo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class PassageiroHub {
    private String cpf;
    private List<AssentoHub> assentos;
    private DocumentoPassageiroHub documento;
    private String email;
    private String faixaEtaria;
    private Date nascimento;
    private String nome;
    private String nomeDoMeio;
    private String sobrenome;
    private PassaporteHub passaporte;
    private String sexo;
    private ContatoHub telefone;
    private String voeBiz;
    private String idPassageiro;
    private List<BilheteHub> bilhetes;
}
