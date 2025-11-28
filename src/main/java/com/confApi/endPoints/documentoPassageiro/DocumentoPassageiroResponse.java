package com.confApi.endPoints.documentoPassageiro;

import lombok.Data;

@Data
public class DocumentoPassageiroResponse {
    private String nacionalidade = "";
    private String numero;
    private String paisEmissor;
    private Integer tipo = 1;
    private String validade;
}
