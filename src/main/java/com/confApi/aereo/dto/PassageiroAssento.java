package com.confApi.aereo.dto;

import lombok.Data;

import java.util.List;
@Data
public class PassageiroAssento {
    private List<Assento> assentos;
    private String nome;
    private String paxID;
    private String sobrenome;
}
