package com.confApi.model;

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
}
