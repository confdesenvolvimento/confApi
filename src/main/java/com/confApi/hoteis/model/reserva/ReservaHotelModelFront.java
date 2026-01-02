package com.confApi.hoteis.model.reserva;

import com.confApi.db.confManager.historicoReserva.dto.HistoricoReserva;
import com.confApi.db.confManager.hotel.model.HotelResponse;
import com.confApi.db.confManager.hotel.model.TarifaHotel;
import com.confApi.db.confManager.reservaPacote.ReservaPacote;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.hub.aereo.FormaPagamentoModel;
import com.confApi.hub.aereo.RecebimentoModel;
import lombok.Data;

import java.util.Date;
import java.util.List;
@Data
public class ReservaHotelModelFront {
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
}
