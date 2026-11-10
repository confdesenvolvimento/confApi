package com.confApi.hoteis;

public class CidadeHotelQuantidadeDTO {
    private Integer id;
    private String codeCidade;
    private String nomeCidade;
    private String nomeEstado;
    private String siglaUf;
    private String nomePais;
    private Integer codePais;
    private Integer codeEstado;
    private Long quantidadeHoteis;

    public CidadeHotelQuantidadeDTO() {
    }

    public String getCodeCidade() {
        return codeCidade;
    }

    public void setCodeCidade(String codeCidade) {
        this.codeCidade = codeCidade;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNomeCidade() {
        return nomeCidade;
    }

    public void setNomeCidade(String nomeCidade) {
        this.nomeCidade = nomeCidade;
    }

    public String getNomeEstado() {
        return nomeEstado;
    }

    public void setNomeEstado(String nomeEstado) {
        this.nomeEstado = nomeEstado;
    }

    public String getSiglaUf() {
        return siglaUf;
    }

    public void setSiglaUf(String siglaUf) {
        this.siglaUf = siglaUf;
    }

    public String getNomePais() {
        return nomePais;
    }

    public void setNomePais(String nomePais) {
        this.nomePais = nomePais;
    }

    public Integer getCodePais() {
        return codePais;
    }

    public void setCodePais(Integer codePais) {
        this.codePais = codePais;
    }

    public Integer getCodeEstado() {
        return codeEstado;
    }

    public void setCodeEstado(Integer codeEstado) {
        this.codeEstado = codeEstado;
    }

    public Long getQuantidadeHoteis() {
        return quantidadeHoteis;
    }

    public void setQuantidadeHoteis(Long quantidadeHoteis) {
        this.quantidadeHoteis = quantidadeHoteis;
    }

    private String zonaCode;
    private String codgEz;

    public String getZonaCode() {
        return zonaCode;
    }

    public void setZonaCode(String zonaCode) {
        this.zonaCode = zonaCode;
    }

    public String getCodgEz() {
        return codgEz;
    }

    public void setCodgEz(String codgEz) {
        this.codgEz = codgEz;
    }
}
