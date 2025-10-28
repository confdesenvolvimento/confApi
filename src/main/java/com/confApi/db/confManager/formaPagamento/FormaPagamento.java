package com.confApi.db.confManager.formaPagamento;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FormaPagamento implements Serializable {

    private Integer codgFormaPagto;
    private String nomeFormaPagto;
    private Integer flagStatus;
    private Integer flagExibirReceb;
    private Integer flagExibirPagto;
    private Integer flagExibirFormaReserva;

    public FormaPagamento() {
    }

    public FormaPagamento(Integer codgFormaPagto) {
        this.codgFormaPagto = codgFormaPagto;
    }



    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.codgFormaPagto);
        hash = 79 * hash + Objects.hashCode(this.nomeFormaPagto);
        hash = 79 * hash + Objects.hashCode(this.flagStatus);
        hash = 79 * hash + Objects.hashCode(this.flagExibirReceb);
        hash = 79 * hash + Objects.hashCode(this.flagExibirPagto);
        hash = 79 * hash + Objects.hashCode(this.flagExibirFormaReserva);
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
        final FormaPagamento other = (FormaPagamento) obj;
        if (!Objects.equals(this.nomeFormaPagto, other.nomeFormaPagto)) {
            return false;
        }
        if (!Objects.equals(this.codgFormaPagto, other.codgFormaPagto)) {
            return false;
        }
        if (!Objects.equals(this.flagStatus, other.flagStatus)) {
            return false;
        }
        if (!Objects.equals(this.flagExibirReceb, other.flagExibirReceb)) {
            return false;
        }
        if (!Objects.equals(this.flagExibirPagto, other.flagExibirPagto)) {
            return false;
        }
        return Objects.equals(this.flagExibirFormaReserva, other.flagExibirFormaReserva);
    }



    public Integer getCodgFormaPagto() {
        return codgFormaPagto;
    }

    public void setCodgFormaPagto(Integer codgFormaPagto) {
        this.codgFormaPagto = codgFormaPagto;
    }

    public String getNomeFormaPagto() {
        return nomeFormaPagto;
    }

    public void setNomeFormaPagto(String nomeFormaPagto) {
        this.nomeFormaPagto = nomeFormaPagto;
    }

    public Integer getFlagStatus() {
        return flagStatus;
    }

    public void setFlagStatus(Integer flagStatus) {
        this.flagStatus = flagStatus;
    }

    public Integer getFlagExibirReceb() {
        return flagExibirReceb;
    }

    public void setFlagExibirReceb(Integer flagExibirReceb) {
        this.flagExibirReceb = flagExibirReceb;
    }

    public Integer getFlagExibirPagto() {
        return flagExibirPagto;
    }

    public void setFlagExibirPagto(Integer flagExibirPagto) {
        this.flagExibirPagto = flagExibirPagto;
    }

    public Integer getFlagExibirFormaReserva() {
        return flagExibirFormaReserva;
    }

    public void setFlagExibirFormaReserva(Integer flagExibirFormaReserva) {
        this.flagExibirFormaReserva = flagExibirFormaReserva;
    }



}

