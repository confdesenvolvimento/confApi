package com.confApi.aereo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
@Data
public class BuscarFormasFinanciamentoResponse {
    @JsonProperty("Data")
    public Date data;
    @JsonProperty("DataVersao")
    public String dataVersao;
    @JsonProperty("SessaoExpirada")
    public boolean sessaoExpirada;
    @JsonProperty("Exception")
    public ExceptionDetail exception;
    @JsonProperty("Financiamentos")
    public ArrayList<Financiamento> financiamentos;
}
