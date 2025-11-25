package com.confApi.db.confManager.produto;

import javax.persistence.*;
import java.io.Serializable;

public class Produto implements Serializable {

    private Integer codgProduto;
    private String nomeProduto;
    private String status;
    private String idIntegracao;

    public Produto() {
    }

    public Produto(Integer codgProduto) {
        this.codgProduto = codgProduto;
    }



    public Integer getCodgProduto() {
        return codgProduto;
    }

    public void setCodgProduto(Integer codgProduto) {
        this.codgProduto = codgProduto;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public void setNomeProduto(String nomeProduto) {
        this.nomeProduto = nomeProduto;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIdIntegracao() {
        return idIntegracao;
    }

    public void setIdIntegracao(String idIntegracao) {
        this.idIntegracao = idIntegracao;
    }

}

