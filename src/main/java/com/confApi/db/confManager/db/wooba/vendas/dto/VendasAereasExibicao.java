package com.confApi.db.confManager.db.wooba.vendas.dto;

import lombok.Data;

import java.io.Serializable;
@Data
public class VendasAereasExibicao implements Serializable {
    private String siglaCia;
    private String nomeCia;
    private String  numeroBilhete;
    private String localizador;
    private String passageiro;
    private String rota;
    private String tipoRota;
    private Double valor =0.0;
    private String status;
    private String fonte = "Portal do Agente";
    private String dataEmissao;

}
