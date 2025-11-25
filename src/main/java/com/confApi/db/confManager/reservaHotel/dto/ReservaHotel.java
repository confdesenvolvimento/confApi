package com.confApi.db.confManager.reservaHotel.dto;

import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.hotel.dto.Hotel;

import com.confApi.db.confManager.hotel.model.ReservaHotelModel;
import com.confApi.db.confManager.hotelQuartoValor.HotelQuartoValor;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.db.confManager.reservaPacote.ReservaPacote;
import com.confApi.db.confManager.sistema.Sistema;
import com.confApi.db.confManager.usuario.Usuario;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Data
public class ReservaHotel implements Serializable {

    private Integer codgReservaHotel;
    private Usuario codgUsuarioCriacao;
    private Sistema codgSistema;
    private Timestamp dataCriacao;
    private Timestamp dataLimiteEmissao;
    private Timestamp dataEmissao;
    private Timestamp dataCancelamento;
    private String localizador;
    private Integer status = 0;
    private Integer statusPagamento = 1;
    private Agencia codgAgencia;
    private String descricaoMotivoCancelamento;
    private Integer codgUsuarioCancelamento = 0;
    private String descricaoRegraCancelamento;
    private Hotel codgHotel;
    private Timestamp prazoPagamentoCliente;
    private Timestamp prazoPagamentoFornecedor;
    private List<Recebimento> recebimentos;
    private List<HotelQuartoValor> hotelQuartoValorList;
    private Double valorTotalReserva = 0.0;
    private Date dataEntrada = new Date();
    private Date dataSaida = new Date();
    private String fonte;
    private ReservaPacote codgReservaPacote;
    private String fornecedor;
    private Boolean isExterno = false;
    private Integer numeroDeQuartos = 0;
    private Integer quantidadeDiarias = 0;

    public ReservaHotel() {
    }

    public ReservaHotel(Integer codgReservaHotel, Usuario codgUsuarioCriacao, Sistema codgSistema,
                        Timestamp dataCriacao, Timestamp dataLimiteEmissao, Timestamp dataEmissao,
                        Timestamp dataCancelamento, String localizador, Agencia codgAgencia,
                        String descricaoMotivoCancelamento, String descricaoRegraCancelamento, Hotel codgHotel,
                        Timestamp prazoPagamentoCliente, Timestamp prazoPagamentoFornecedor, List<Recebimento> recebimentos,
                        List<HotelQuartoValor> hotelQuartoValorList, Integer statusPagamento) {
        this.codgReservaHotel = codgReservaHotel;
        this.codgUsuarioCriacao = codgUsuarioCriacao;
        this.codgSistema = codgSistema;
        this.dataCriacao = dataCriacao;
        this.dataLimiteEmissao = dataLimiteEmissao;
        this.dataEmissao = dataEmissao;
        this.dataCancelamento = dataCancelamento;
        this.localizador = localizador;
        this.codgAgencia = codgAgencia;
        this.descricaoMotivoCancelamento = descricaoMotivoCancelamento;
        this.descricaoRegraCancelamento = descricaoRegraCancelamento;
        this.codgHotel = codgHotel;
        this.prazoPagamentoCliente = prazoPagamentoCliente;
        this.prazoPagamentoFornecedor = prazoPagamentoFornecedor;
        this.recebimentos = recebimentos;
        this.hotelQuartoValorList = hotelQuartoValorList;
        this.statusPagamento = statusPagamento;
        if (hotelQuartoValorList != null) {
            if (!hotelQuartoValorList.isEmpty()) {
                for (HotelQuartoValor hotelQuartoValor : hotelQuartoValorList) {
                    valorTotalReserva = valorTotalReserva + hotelQuartoValor.getValorTotalEstadiaMarkup();
                }
            }
        }
    }

    public ReservaHotel(Integer codgReservaHotel, Usuario codgUsuarioCriacao, Sistema codgSistema,
                        Timestamp dataCriacao, Timestamp dataLimiteEmissao, Timestamp dataEmissao,
                        Timestamp dataCancelamento, String localizador, Agencia codgAgencia,
                        String descricaoMotivoCancelamento, String descricaoRegraCancelamento, Hotel codgHotel,
                        Timestamp prazoPagamentoCliente, Timestamp prazoPagamentoFornecedor, List<Recebimento> recebimentos,
                        List<HotelQuartoValor> hotelQuartoValorList, Integer statusPagamento, ReservaPacote reservaPacote) {
        this.codgReservaHotel = codgReservaHotel;
        this.codgUsuarioCriacao = codgUsuarioCriacao;
        this.codgSistema = codgSistema;
        this.dataCriacao = dataCriacao;
        this.dataLimiteEmissao = dataLimiteEmissao;
        this.dataEmissao = dataEmissao;
        this.dataCancelamento = dataCancelamento;
        this.localizador = localizador;
        this.codgAgencia = codgAgencia;
        this.descricaoMotivoCancelamento = descricaoMotivoCancelamento;
        this.descricaoRegraCancelamento = descricaoRegraCancelamento;
        this.codgHotel = codgHotel;
        this.prazoPagamentoCliente = prazoPagamentoCliente;
        this.prazoPagamentoFornecedor = prazoPagamentoFornecedor;
        this.recebimentos = recebimentos;
        this.hotelQuartoValorList = hotelQuartoValorList;
        this.statusPagamento = statusPagamento;
        this.codgReservaPacote = reservaPacote;
        if (hotelQuartoValorList != null) {
            if (!hotelQuartoValorList.isEmpty()) {
                for (HotelQuartoValor hotelQuartoValor : hotelQuartoValorList) {
                    valorTotalReserva = valorTotalReserva + hotelQuartoValor.getValorTotalEstadiaMarkup();
                }
            }
        }
    }



    public ReservaHotel(Integer codgReservaHotel) {
        this.codgReservaHotel = codgReservaHotel;
    }

    public ReservaHotel(ReservaHotelModel obj) {
        this.codgReservaHotel = obj.getCodgReservaHotelDB().intValue();
        this.codgUsuarioCriacao = null;
        this.codgSistema = null;
        this.dataCriacao = (Timestamp) obj.getDataCriacao();
        this.dataLimiteEmissao = (Timestamp) obj.getDataLimitePagamento();
        this.dataEmissao = (Timestamp) obj.getDataPagamento();
        this.dataCancelamento = (Timestamp) obj.getDataCancelamento();
        this.localizador = obj.getLocalizador();
        this.status = 0;
        this.codgAgencia = null;
        this.descricaoMotivoCancelamento = obj.getDescricaMotivoCancelamento();
        this.codgUsuarioCancelamento = Integer.parseInt(obj.getUsuarioCancelamento());
        this.descricaoRegraCancelamento = obj.getDescricaoRegraCancelamento();
        this.codgHotel = new Hotel(Integer.parseInt(obj.getCodgHotel()));
        this.prazoPagamentoCliente = (Timestamp) obj.getDataLimitePagamento();
        this.prazoPagamentoFornecedor = null;
        this.recebimentos = null;
        this.statusPagamento = obj.getStatusPagamento();
        this.hotelQuartoValorList = null;
        this.codgReservaPacote = obj.getCodgReservaPacote();
        if (hotelQuartoValorList != null) {
            if (!hotelQuartoValorList.isEmpty()) {
                for (HotelQuartoValor hotelQuartoValor : hotelQuartoValorList) {
                   // UtilDebug.sysOut("VALOR TOTAL");
                  //  UtilDebug.sysOut(hotelQuartoValor.getValorTotalEstadiaMarkup());
                  //  UtilDebug.sysOut(hotelQuartoValor.getValorTotalEstadiaNet());
                    valorTotalReserva = valorTotalReserva + hotelQuartoValor.getValorTotalEstadiaNet();
                }
            }
        }
    }

    public ReservaHotel(Integer codgReservaHotel, Usuario codgUsuarioCriacao, Sistema codgSistema, Timestamp dataCriacao, Timestamp dataLimiteEmissao, Timestamp dataEmissao, Timestamp dataCancelamento, String localizador, Agencia codgAgencia, String descricaoMotivoCancelamento, String descricaoRegraCancelamento, Hotel codgHotel, Timestamp prazoPagamentoCliente, Timestamp prazoPagamentoFornecedor, List<Recebimento> recebimentos, List<HotelQuartoValor> hotelQuartoValorList, String fonte, ReservaPacote codgReservaPacote) {
        this.codgReservaHotel = codgReservaHotel;
        this.codgUsuarioCriacao = codgUsuarioCriacao;
        this.codgSistema = codgSistema;
        this.dataCriacao = dataCriacao;
        this.dataLimiteEmissao = dataLimiteEmissao;
        this.dataEmissao = dataEmissao;
        this.dataCancelamento = dataCancelamento;
        this.localizador = localizador;
        this.codgAgencia = codgAgencia;
        this.descricaoMotivoCancelamento = descricaoMotivoCancelamento;
        this.descricaoRegraCancelamento = descricaoRegraCancelamento;
        this.codgHotel = codgHotel;
        this.prazoPagamentoCliente = prazoPagamentoCliente;
        this.prazoPagamentoFornecedor = prazoPagamentoFornecedor;
        this.recebimentos = recebimentos;
        this.hotelQuartoValorList = null;
        this.fonte = fonte;
        this.codgReservaPacote = codgReservaPacote;
        if (this.hotelQuartoValorList != null) {
            if (!this.hotelQuartoValorList.isEmpty()) {
                for (HotelQuartoValor hotelQuartoValor : hotelQuartoValorList) {
                    valorTotalReserva = valorTotalReserva + hotelQuartoValor.getValorTotalEstadiaNet();
                }
            }
        }
    }




}

