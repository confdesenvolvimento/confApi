package com.confApi.model;

import com.confApi.db.confManager.usuario.Usuario;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
@Data
public class HistoricoReservaModel implements Serializable {

    private int codgHistoricoReserva;
    private Integer codgReservaAereo;
    private Date dataHoraTransacao;
    private String descricao;
    private String ipAcesso;
    private int flagInterno;
    private Usuario codgUsuario;
    private Integer codgReservaHotel;
    private Integer codgReservaPacote;
    private Integer codgReservaSeguro;
}
