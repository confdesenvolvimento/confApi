package com.confApi.aereo.dto;

import lombok.Data;

import java.util.List;
@Data
public class Cabine {
    private String cabineID;
    private String classeDaCabine;
    private List<ColunasInfo> colunasInfo;
    private Boolean compraDeAssentos;
    private Boolean espacoAzul;
    private List<Linha> linha;
    private String nomeDaCabine;
}
