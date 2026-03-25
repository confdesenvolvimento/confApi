package com.confApi.db.confManager.seguro.cobertura;

import com.confApi.db.confManager.seguro.reserva.SeguroReserva;
import com.confApi.seguros.dto.PlanoSeguroDTO;
import lombok.Data;

@Data
public class SeguroCobertura {

    private Integer codgSeguroCobertura;
    private SeguroReserva seguroReserva;
    private String idCobertura;
    private String nomeCobertura;
    private String idGrupo;
    private String moeda;
    private Double valorCobertura;
    private Double valorSeguroNet;
    private Double valorSeguroNetBrl;
    private Double valorSeguroMkp;
    private Double valorSeguroMkpBrl;
    private Double percMkp;
    private Double valorMkpAplicado;
    private Double valorMkpAplicadoBrl;

    public SeguroCobertura() {
    }

    public SeguroCobertura(PlanoSeguroDTO planoSeguroDTO) {
        this.codgSeguroCobertura = null;
        this.seguroReserva = null;
        this.idCobertura = planoSeguroDTO.getIdPlano();
        this.nomeCobertura = planoSeguroDTO.getNomePlano();
        this.idGrupo = null;
        this.moeda = planoSeguroDTO.getMoedaCobertura();
        this.valorCobertura = planoSeguroDTO.getValorCobertura();
        this.valorSeguroNet = planoSeguroDTO.getValorCobertura();
        this.valorSeguroNetBrl = planoSeguroDTO.getPrecoBaseBRL();
        this.valorSeguroMkp = planoSeguroDTO.getPrecoFinalBRL();
        this.valorSeguroMkpBrl = planoSeguroDTO.getPrecoFinalBRL();
        this.percMkp = (planoSeguroDTO.getPrecoFinalBRL() - planoSeguroDTO.getPrecoBaseBRL()) / planoSeguroDTO.getPrecoBaseBRL() * 100;
        this.valorMkpAplicado = planoSeguroDTO.getPrecoFinalBRL() - planoSeguroDTO.getPrecoBaseBRL();
        this.valorMkpAplicadoBrl = planoSeguroDTO.getPrecoFinalBRL() - planoSeguroDTO.getPrecoBaseBRL();
    }
}
