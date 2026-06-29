package com.confApi.aereo.dto;

import com.confApi.aereo.dto.regrasAereas.RegrasAereasReservaResponse;
import com.confApi.hub.aereo.dto.Contato;
import com.confApi.hub.aereo.dto.Passageiro;
import com.confApi.hub.aereo.dto.TrechoReserva;
import lombok.Data;

import java.util.Date;
import java.util.List;
@Data
public class Reserva {
    private Object alerta;
    //private List<Bilhete> bilhetes;
    private List<Contato> contatos;
    private Date dataCriacao;
    private Date dataEmissao;
    private List<Object> eMDs;
    private String localizador;
    private Boolean mapaDeAssentosDisponivel;
    private List<Passageiro> passageiros;
    private Boolean permiteCancelar;
    private Boolean permiteEmitir;
    private String prazoEmissao;
    private String sistema;
    private String status;
    private List<TrechoReserva> viagens;
    private ValorReserva valorReserva;
    private RegrasAereasReservaResponse regrasAereas;
}
