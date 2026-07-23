package com.confApi.model;

import com.confApi.db.confManager.agencia.dto.Agencia;
import lombok.Data;

@Data
public class IdentificacaoAgenciaModel {
    private Integer codgAgencia;
    private Integer codgErp;
    private Integer codgUnidade;
    private Integer codgUsuario;
    private Integer codgProduto;

    public IdentificacaoAgenciaModel() {

    }

    public IdentificacaoAgenciaModel(Integer codgAgencia, Integer codgErp, Integer codgUnidade, Integer codgUsuario, Integer codgProduto) {
        this.codgAgencia = codgAgencia;
        this.codgErp = codgErp;
        this.codgUnidade = codgUnidade;
        this.codgUsuario = codgUsuario;
        this.codgProduto = codgProduto;
    }

    public IdentificacaoAgenciaModel(Agencia agencia) {
        this.codgAgencia = agencia.getCodgAgencia();
        this.codgErp = agencia.getIdWoobaAgencia();
        this.codgUnidade = agencia.getCodgUnidade() != null ? agencia.getCodgUnidade().getCodgUnidade() : null;
        this.codgUsuario = null;
        this.codgProduto = null;
    }



    public Integer getCodgAgencia() {
        return codgAgencia;
    }

    public void setCodgAgencia(Integer codgAgencia) {
        this.codgAgencia = codgAgencia;
    }

    public Integer getCodgErp() {
        return codgErp;
    }

    public void setCodgErp(Integer codgErp) {
        this.codgErp = codgErp;
    }

    public Integer getCodgUnidade() {
        return codgUnidade;
    }

    public void setCodgUnidade(Integer codgUnidade) {
        this.codgUnidade = codgUnidade;
    }

    public Integer getCodgUsuario() {
        return codgUsuario;
    }

    public void setCodgUsuario(Integer codgUsuario) {
        this.codgUsuario = codgUsuario;
    }

    public Integer getCodgProduto() {
        return codgProduto;
    }

    public void setCodgProduto(Integer codgProduto) {
        this.codgProduto = codgProduto;
    }
}
