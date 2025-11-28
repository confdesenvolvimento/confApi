package com.confApi.hub.aereo;

import lombok.Data;

import java.util.Date;

@Data
public class DocumentoPassageiroHub {
    private String nacionalidade;
    private String numero;
    private String paisEmissor;
    private Integer tipo;
    private Date validade;
}
