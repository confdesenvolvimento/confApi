package com.confApi.aereo.dto;

import lombok.Data;

import java.io.Serializable;
@Data
public class Resumo implements Serializable {

    private String tempo;
    private String sistema;
    private String mensagem;
}
