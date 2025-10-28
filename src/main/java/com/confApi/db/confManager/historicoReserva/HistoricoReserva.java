package com.confApi.db.confManager.historicoReserva;

import com.confApi.db.confManager.usuario.Usuario;

import java.io.Serializable;
import java.util.Date;

public class HistoricoReserva implements Serializable {

    private int codgHistoricoReserva;
    private Integer codgReservaAereo;
    private Date dataHoraTransacao;
    private String descricao;
    private String ipAcesso;
    private int flagInterno;
    private Usuario codgUsuario;
    private Integer codgReservaHotel;
    private Integer codgReservaPacote;

    public HistoricoReserva(int codgHistoricoReserva, String descricao, String ipAcesso, int flagInterno, Usuario codgUsuario, Integer codgReservaPacote, Date dataHoraTransacao) {
        this.codgHistoricoReserva = codgHistoricoReserva;
        this.descricao = descricao;
        this.ipAcesso = ipAcesso;
        this.flagInterno = flagInterno;
        this.codgUsuario = codgUsuario;
        this.codgReservaPacote = codgReservaPacote;
        this.dataHoraTransacao = dataHoraTransacao;
    }



    public HistoricoReserva(int codgHistoricoReserva, Integer codgReservaAereo,
                            Date dataHoraTransacao, String descricao, String ipAcesso, int flagInterno,
                            Usuario codgUsuario, Integer codgReservaHotel) {
        this.codgHistoricoReserva = codgHistoricoReserva;
        this.codgReservaAereo = codgReservaAereo;
        this.dataHoraTransacao = dataHoraTransacao;
        this.descricao = descricao;
        this.ipAcesso = ipAcesso;
        this.flagInterno = flagInterno;
        this.codgUsuario = codgUsuario;
        this.codgReservaHotel = codgReservaHotel;
    }

    public HistoricoReserva(int codgHistoricoReserva, Integer codgReservaAereo,
                            Date dataHoraTransacao, String descricao, String ipAcesso, int flagInterno,
                            Usuario codgUsuario, Integer codgReservaHotel, Integer codgReservaPacote) {
        this.codgHistoricoReserva = codgHistoricoReserva;
        this.codgReservaAereo = codgReservaAereo;
        this.dataHoraTransacao = dataHoraTransacao;
        this.descricao = descricao;
        this.ipAcesso = ipAcesso;
        this.flagInterno = flagInterno;
        this.codgUsuario = codgUsuario;
        this.codgReservaHotel = codgReservaHotel;
        this.codgReservaPacote = codgReservaPacote;
    }

    public HistoricoReserva() {
    }

    public int getCodgHistoricoReserva() {
        return codgHistoricoReserva;
    }

    public void setCodgHistoricoReserva(int codgHistoricoReserva) {
        this.codgHistoricoReserva = codgHistoricoReserva;
    }

    public Integer getCodgReservaAereo() {
        return codgReservaAereo;
    }

    public void setCodgReservaAereo(Integer codgReservaAereo) {
        this.codgReservaAereo = codgReservaAereo;
    }

    public Date getDataHoraTransacao() {
        return dataHoraTransacao;
    }

    public void setDataHoraTransacao(Date dataHoraTransacao) {
        this.dataHoraTransacao = dataHoraTransacao;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getIpAcesso() {
        return ipAcesso;
    }

    public void setIpAcesso(String ipAcesso) {
        this.ipAcesso = ipAcesso;
    }

    public int getFlagInterno() {
        return flagInterno;
    }

    public void setFlagInterno(int flagInterno) {
        this.flagInterno = flagInterno;
    }

    public Usuario getCodgUsuario() {
        return codgUsuario;
    }

    public void setCodgUsuario(Usuario codgUsuario) {
        this.codgUsuario = codgUsuario;
    }

    public Integer getCodgReservaHotel() {
        return codgReservaHotel;
    }

    public void setCodgReservaHotel(Integer codgReservaHotel) {
        this.codgReservaHotel = codgReservaHotel;
    }

    public Integer getCodgReservaPacote() {
        return codgReservaPacote;
    }

    public void setCodgReservaPacote(Integer codgReservaPacote) {
        this.codgReservaPacote = codgReservaPacote;
    }
}
