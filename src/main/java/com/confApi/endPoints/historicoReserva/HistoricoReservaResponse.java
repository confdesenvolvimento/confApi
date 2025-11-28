package com.confApi.endPoints.historicoReserva;

import com.confApi.endPoints.usuario.UsuarioResponse;
import lombok.Data;

import java.util.Date;

@Data
public class HistoricoReservaResponse {
    private int codgHistoricoReserva;
    private Integer codgReservaAereo;
    private Date dataHoraTransacao;
    private String descricao;
    private String ipAcesso;
    private int flagInterno;
    private UsuarioResponse codgUsuario;
    private Integer codgReservaHotel;
    private Integer codgReservaPacote;
}
