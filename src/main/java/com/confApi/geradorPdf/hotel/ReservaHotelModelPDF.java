package com.confApi.geradorPdf.hotel;

import com.confApi.db.confManager.agencia.AgenciaApi;
import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.bandeira.BandeiraApi;
import com.confApi.db.confManager.historicoReserva.dto.HistoricoReserva;
import com.confApi.db.confManager.hotel.model.HotelResponse;
import com.confApi.db.confManager.hotel.model.ReservaHotelModel;
import com.confApi.db.confManager.hotel.model.TarifaHotel;
import com.confApi.db.confManager.reservaHotel.dto.ReservaHotel;
import com.confApi.db.confManager.reservaPacote.ReservaPacote;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.hub.aereo.CartaoModel;
import com.confApi.hub.aereo.FormaPagamentoModel;
import com.confApi.hub.aereo.RecebimentoModel;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class ReservaHotelModelPDF implements Serializable {

    private Long codgReservaHotelDB;
    private String codgHotel;
    private String usuarioCriacao;
    private Usuario codgUsuarioCriacao;
    private String nomeSistema;
    private Date dataCriacao;
    private Date dataEmissao;
    private Date dataLimitePagamento;
    private Date dataPagamento;
    private Date dataCancelamento;
    private String localizador;
    private Integer status;
    private Integer statusPagamento = 1;
    private Integer codgAgencia;
    private Agencia agencia;
    private String descricaMotivoCancelamento;
    private String usuarioCancelamento;
    private String descricaoRegraCancelamento;
    private String msg;
    private Double valorTotalReserva = 0.0;
    private HotelResponse hotel;
    private TarifaHotel tarifaHotel;
    private FormaPagamentoModel formaPagamentoSelecionada = new FormaPagamentoModel();
    private List<FormaPagamentoModel> formasPagamentos;
    private RecebimentoModel recebimento;
    private List<RecebimentoModel> recebimentos;
    private List<HistoricoReserva> historico;
    private String observacao;
    private Boolean isExibitBtnImprimir = false;
    private Boolean isExibirBtnCancelarReserva = true;
    private Boolean isExibirBtnVisualizarVoucher = false;
    private Boolean isReservaPacote = false;
    private String motivoCancelamento = "Desistencia";
    private String descricaoMotivoCancelamento = "Desistencia";
    private TarifaHotel tarifaTotalReserva = new TarifaHotel();
    private ReservaPacote codgReservaPacote;

    private String fonte;

    public ReservaHotelModelPDF() {
    }

    public ReservaHotelModelPDF(ReservaHotelModel reservaHotel) {
        this.codgReservaHotelDB = null;
        this.codgHotel = reservaHotel.getCodgHotel();
        this.usuarioCriacao = reservaHotel.getCodgUsuarioCriacao().getNomeCompleto();
        this.codgUsuarioCriacao = reservaHotel.getCodgUsuarioCriacao();
        this.nomeSistema = reservaHotel.getNomeSistema();
        this.dataCriacao = reservaHotel.getDataCriacao();
        this.dataEmissao = reservaHotel.getDataEmissao();
        this.dataLimitePagamento = reservaHotel.getDataLimitePagamento();
        this.dataPagamento = reservaHotel.getDataEmissao();
        this.dataCancelamento = reservaHotel.getDataCancelamento();
        this.localizador = reservaHotel.getLocalizador();
        this.status = reservaHotel.getStatus();
        this.codgAgencia = reservaHotel.getCodgAgencia();
        this.agencia = new AgenciaApi().findCodgAgencia(reservaHotel.getCodgAgencia());
        this.descricaMotivoCancelamento = reservaHotel.getDescricaoMotivoCancelamento();
        this.usuarioCancelamento = reservaHotel.getUsuarioCancelamento();
        this.descricaoRegraCancelamento = reservaHotel.getDescricaoRegraCancelamento();
        this.msg = reservaHotel.getMsg();
        this.hotel = reservaHotel.getHotel();
        this.tarifaHotel = reservaHotel.getTarifaHotel();
        this.formasPagamentos = reservaHotel.getFormasPagamentos();
        this.recebimento = reservaHotel.getRecebimento();
        this.recebimentos = reservaHotel.getRecebimentos();
        this.historico = reservaHotel.getHistorico();
        this.observacao = reservaHotel.getObservacao();
        this.codgReservaPacote = reservaHotel.getCodgReservaPacote();
        this.fonte = reservaHotel.getFonte();
        this.statusPagamento = reservaHotel.getStatusPagamento();
        this.tarifaTotalReserva = reservaHotel.getTarifaTotalReserva();
        this.isExibirBtnVisualizarVoucher = reservaHotel.getIsExibirBtnVisualizarVoucher();
    }



}


