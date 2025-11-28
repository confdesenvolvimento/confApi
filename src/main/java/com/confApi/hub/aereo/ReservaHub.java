package com.confApi.hub.aereo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ReservaHub {
    private Object alerta;
    @JsonIgnore
    private List<BilheteHub> bilhetes;
    private List<ContatoHub> contatos;
    private Date dataCriacao;
    private Date dataEmissao;
    private List<Object> eMDs;
    private String localizador;
    private Boolean mapaDeAssentosDisponivel;
    private List<PassageiroHub> passageiros;
    private Boolean permiteCancelar;
    private Boolean permiteEmitir;
    private Date prazoEmissao;
    private String sistema;
    private String status;
    private List<TrechoReservaHub> viagens;
    private ValorReservaHub valorReserva;
}
