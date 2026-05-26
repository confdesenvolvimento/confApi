package com.confApi.carros.dto;

import lombok.Data;

@Data
public class LocalCarro {
    private String cidadeNome;
    private String cidadeIATA;
    private String data;
    private String hora;
    private String paisNome;
    private String paisIATA;
}
