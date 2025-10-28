package com.confApi.hub.aereo;

import java.io.Serializable;
import java.util.Objects;

public class ParcelaCartaoModel implements Serializable {


    private Integer numeroDaParcela;
    private Double valorPrimeiraParcela = 0.0;
    private Double valorDemaisParcelas = 0.0;
    private Double valorJuros =0.0;
    private String descricaoParcela;

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.numeroDaParcela);
        hash = 59 * hash + Objects.hashCode(this.valorPrimeiraParcela);
        hash = 59 * hash + Objects.hashCode(this.valorDemaisParcelas);
        hash = 59 * hash + Objects.hashCode(this.valorJuros);
        hash = 59 * hash + Objects.hashCode(this.descricaoParcela);
        return hash;
    }



    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ParcelaCartaoModel other = (ParcelaCartaoModel) obj;
        if (!Objects.equals(this.descricaoParcela, other.descricaoParcela)) {
            return false;
        }
        if (!Objects.equals(this.numeroDaParcela, other.numeroDaParcela)) {
            return false;
        }
        if (!Objects.equals(this.valorPrimeiraParcela, other.valorPrimeiraParcela)) {
            return false;
        }
        if (!Objects.equals(this.valorDemaisParcelas, other.valorDemaisParcelas)) {
            return false;
        }
        return Objects.equals(this.valorJuros, other.valorJuros);
    }



    public String getDescricaoParcela() {
        return descricaoParcela;
    }

    public void setDescricaoParcela(String descricaoParcela) {
        this.descricaoParcela = descricaoParcela;
    }

    public ParcelaCartaoModel(Integer numeroDaParcela) {
        this.numeroDaParcela = numeroDaParcela;
    }

    public ParcelaCartaoModel() {
    }




    public Integer getNumeroDaParcela() {
        return numeroDaParcela;
    }

    public void setNumeroDaParcela(Integer numeroDaParcela) {
        this.numeroDaParcela = numeroDaParcela;
    }

    public Double getValorPrimeiraParcela() {
        return valorPrimeiraParcela;
    }

    public void setValorPrimeiraParcela(Double valorPrimeiraParcela) {
        this.valorPrimeiraParcela = valorPrimeiraParcela;
    }

    public Double getValorDemaisParcelas() {
        return valorDemaisParcelas;
    }

    public void setValorDemaisParcelas(Double valorDemaisParcelas) {
        this.valorDemaisParcelas = valorDemaisParcelas;
    }

    public Double getValorJuros() {
        return valorJuros;
    }

    public void setValorJuros(Double valorJuros) {
        this.valorJuros = valorJuros;
    }


}
