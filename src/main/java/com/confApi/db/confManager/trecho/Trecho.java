package com.confApi.db.confManager.trecho;

import com.confApi.db.confManager.aeroporto.Aeroporto;
import com.confApi.db.confManager.companhiaAerea.CompanhiaAerea;
import com.confApi.db.confManager.voo.Voo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Trecho implements Serializable {
    @JsonIgnore
    private int codgTrecho;
    @JsonIgnore
    private int codgReservaAereo;
    private CompanhiaAerea codgCompanhiaAerea;
    private Date dataPartida;
    private Date dataChegada;
    private Aeroporto codgAeroportoOrigem;
    private Aeroporto codgAeroportoDestino;
    private List<Voo> voos;

    public int getCodgTrecho() {
        return codgTrecho;
    }

    public void setCodgTrecho(int codgTrecho) {
        this.codgTrecho = codgTrecho;
    }

    public int getCodgReservaAereo() {
        return codgReservaAereo;
    }

    public void setCodgReservaAereo(int codgReservaAereo) {
        this.codgReservaAereo = codgReservaAereo;
    }

    public CompanhiaAerea getCodgCompanhiaAerea() {
        return codgCompanhiaAerea;
    }

    public void setCodgCompanhiaAerea(CompanhiaAerea codgCompanhiaAerea) {
        this.codgCompanhiaAerea = codgCompanhiaAerea;
    }



    public Date getDataPartida() {
        return dataPartida;
    }

    public void setDataPartida(Date dataPartida) {
        this.dataPartida = dataPartida;
    }

    public Date getDataChegada() {
        return dataChegada;
    }

    public void setDataChegada(Date dataChegada) {
        this.dataChegada = dataChegada;
    }

    public Aeroporto getCodgAeroportoOrigem() {
        return codgAeroportoOrigem;
    }

    public void setCodgAeroportoOrigem(Aeroporto codgAeroportoOrigem) {
        this.codgAeroportoOrigem = codgAeroportoOrigem;
    }

    public Aeroporto getCodgAeroportoDestino() {
        return codgAeroportoDestino;
    }

    public void setCodgAeroportoDestino(Aeroporto codgAeroportoDestino) {
        this.codgAeroportoDestino = codgAeroportoDestino;
    }



    public List<Voo> getVoos() {
        return voos;
    }

    public void setVoos(List<Voo> voos) {
        this.voos = voos;
    }

}

