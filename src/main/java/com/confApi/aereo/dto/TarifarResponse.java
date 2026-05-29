package com.confApi.aereo.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;
@Data
public class TarifarResponse {
    private Date data;
    private String dataVersao;
    private Boolean sessaoExpirada;
    private Agencia agencia;
    private String companhia;
    private List<DocumentoAceito> documentosAceitos;
    // private Object exception;
    private ExceptionDetail exception;
    private Boolean fidelidadeAmericanAirlines;
    private Boolean fidelidadeAzul;
    private Boolean fidelidadeFidelidadeGenerico;
    private Boolean fidelidadeGDS;
    private Boolean fidelidadeGol;
    private Boolean fidelidadeLatam;
    private Boolean foidObrigatorio;
    private Preco preco;
    private List<RegraTarifaria> regraTarifaria;
    private Boolean requerContatoDosPassageiros;
    private Boolean requerContatoDosPassageirosPermiteRecusar;
    private Boolean requerDataDeNascimentoDosPassageirosAdultos;
    private Boolean requerDataDeNascimentoDosPassageirosCriancas;
    private Boolean requerDocumentoDosPassageiros;
    private Boolean reservarParaHoje;
    private Boolean retarifacaoAutomatica;
    private String telefone;
    private List<Trecho> trecho1;
    private List<Trecho> trecho2;
}
