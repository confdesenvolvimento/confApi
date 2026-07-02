package com.confApi.db.clube.contabiliCampanha;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class ContabiliCampanha {

    private Integer id;

    private String codgUsuario;

    private Integer codgCompanha;

    private Integer qtdVenda;

    private Integer qtdBilhetes;

    private Integer qtdTarifa;

    private Double valor;

    private Integer codgAgencia;

    private String nomeAgencia;

    private Integer codgUnidade;

    private String nomeUnidade;

    private String nomeUsuario;

    public ContabiliCampanha() {
    }

    public ContabiliCampanha(Integer codgCampanha) {
    }
}
