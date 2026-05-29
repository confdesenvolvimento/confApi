package com.confApi.aereo.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;
@Data
public class MarcarAssentoResponse {
    private Date data;
    private String dataVersao;
    private Boolean sessaoExpirada;
    private Object exception;
    private List<PassageiroMarcarAssento> passageiros;
    private Boolean sucesso;
}
