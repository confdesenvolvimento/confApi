package com.confApi.db.confManager.carro;

import com.confApi.carros.dto.CarroBookingHub;
import com.confApi.carros.dto.ReservarCarroResponseDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Carro {

    private CarroReserva carroReserva;
    private String idGrupo;
    private String descricaoGrupo;
    private String fornecedorNome;
    private String fornecedorIata;
    private String descModeloCarro;
    private String combustivel;
    private String urlImg;
    private String planoTarifa;
    private String codgPlanoTarifa;
    private Boolean arCondicionado;
    private Integer qtdPassageiros;
    private Integer qtdBagagens;
    private String transmissao;
    private Integer qtdPortas;

    public Carro(ReservarCarroResponseDTO obj) {
        this(obj, obj != null ? new CarroReserva(obj) : null);
    }

    public Carro(ReservarCarroResponseDTO obj, CarroReserva carroReserva) {
        CarroBookingHub reserva = obj != null ? obj.getReservaCarro() : null;

        this.carroReserva = carroReserva;
        this.idGrupo = reserva != null ? reserva.getGrupo() : null;
        this.descricaoGrupo = reserva != null ? reserva.getDescricaoGrupo() : null;
        this.fornecedorNome = reserva != null ? reserva.getFornecedorNome() : null;
        this.fornecedorIata = reserva != null ? reserva.getFornecedorIata() : null;
        this.descModeloCarro = reserva != null ? reserva.getModelo() : null;
        this.combustivel = reserva != null ? reserva.getCombustivel() : null;
        this.urlImg = reserva != null ? reserva.getImagem() : null;
        this.planoTarifa = obj != null ? obj.getTariffType() : null;
        this.codgPlanoTarifa = null;
        this.arCondicionado = reserva != null ? reserva.getArCondicionado() : null;
        this.qtdPassageiros = reserva != null ? reserva.getQtdPassageiros() : null;
        this.qtdBagagens = reserva != null ? reserva.getQtdBagagens() : null;
        this.transmissao = reserva != null ? reserva.getTransmissao() : null;
        this.qtdPortas = null;
    }
}
