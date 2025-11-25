package com.confApi.db.confManager.hotel.model;

import com.confApi.db.confManager.bandeira.BandeiraApi;
import com.confApi.db.confManager.historicoReserva.dto.HistoricoReserva;
import com.confApi.db.confManager.reservaHotel.dto.ReservaHotel;
import com.confApi.db.confManager.reservaPacote.ReservaPacote;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.hub.aereo.CartaoModel;
import com.confApi.hub.aereo.FormaPagamentoModel;
import com.confApi.hub.aereo.RecebimentoModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReservaHotelModel implements Serializable {

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

    public ReservaHotelModel() {
    }

    public ReservaHotelModel(ReservaHotel reservaHotel) {
        this.codgReservaHotelDB = null;
        this.codgHotel = reservaHotel != null && reservaHotel.getCodgHotel() != null
                && reservaHotel.getCodgHotel().getCodgHotel() != null
                ? this.codgHotel = reservaHotel.getCodgHotel().getCodgHotel().toString() : null;
        this.usuarioCriacao = reservaHotel.getCodgUsuarioCriacao().getNomeCompleto();
        this.codgUsuarioCriacao = reservaHotel.getCodgUsuarioCriacao();
        this.nomeSistema = reservaHotel.getCodgSistema().getNomeSistema();
        this.dataCriacao = reservaHotel.getDataCriacao();
        this.dataEmissao = reservaHotel.getDataEmissao();
        this.dataLimitePagamento = reservaHotel.getPrazoPagamentoCliente();
        this.dataPagamento = reservaHotel.getDataEmissao();
        this.dataCancelamento = reservaHotel.getDataCancelamento();
        this.localizador = reservaHotel.getLocalizador();
        this.status = reservaHotel.getStatus();
        this.codgAgencia = reservaHotel.getCodgAgencia().getCodgAgencia();
        this.descricaMotivoCancelamento = reservaHotel.getDescricaoMotivoCancelamento();
        this.usuarioCancelamento = reservaHotel.getCodgUsuarioCancelamento() != null
                ? reservaHotel.getCodgUsuarioCancelamento().toString()
                : null;
        this.descricaoRegraCancelamento = reservaHotel.getDescricaoRegraCancelamento();
        this.msg = null;
        this.hotel = new HotelResponse(reservaHotel);
        this.tarifaHotel = null;
        this.formasPagamentos = null;
        this.recebimento = null;
        this.recebimentos = null;
        this.historico = null;
        this.observacao = null;
        this.codgReservaPacote = null;
        this.fonte = reservaHotel.getFonte();
        this.statusPagamento = reservaHotel.getStatusPagamento();
        this.tarifaHotel = new TarifaHotel(reservaHotel.getHotelQuartoValorList().get(0), reservaHotel);
        this.tarifaTotalReserva = new TarifaHotel(reservaHotel.getHotelQuartoValorList().get(0), reservaHotel);
        this.isExibirBtnVisualizarVoucher = true;
    }


    public void populaRecebimento() {

        FormaPagamentoModel formaPagamentoSelect = new FormaPagamentoModel();
        for (FormaPagamentoModel formas : formasPagamentos) {
            if (formaPagamentoSelecionada.getCodgFormaPagto().equals(formas.getCodgFormaPagto())) {
                formaPagamentoSelect = formas;
            }
        }

        if (formaPagamentoSelect != null) {
            RecebimentoModel recebimentoModel = new RecebimentoModel();
            recebimentoModel.setCodgFormaPagamento(formaPagamentoSelect.getCodgFormaPagto());
            recebimentoModel.setFormaDePagamento(formaPagamentoSelect);
            recebimentoModel.setNomeFormaPagamento(formaPagamentoSelect.getNomeFormaPagto());
            recebimentoModel.setValorEntrada(valorTotalReserva);
            recebimentoModel.setValorPagamento(valorTotalReserva);

            recebimento = recebimentoModel;
        }
        if (recebimento.getCodgFormaPagamento() != null) {
            if (recebimento.getCodgFormaPagamento() == 2) {
                listarCartoes(recebimento.getFormaDePagamento());
                recebimento.setCartaoSelecionado(new CartaoModel());
            } else if (recebimento.getCodgFormaPagamento() == 4) {
               // UtilDebug.sysError("Forma de pagamento Selecionada: " + recebimento.getCodgFormaPagamento());
            }
        }

    }

    public void listarCartoes(FormaPagamentoModel formaPagamentoModel) {

        if (formaPagamentoModel.getBandeiras() == null) {
            formaPagamentoModel.setBandeiras(new ArrayList<>());
        } else {
            formaPagamentoModel.getBandeiras().clear();
        }

        formaPagamentoModel.getBandeiras().addAll(new BandeiraApi().findByAll());
    }

    public String getCodgHotel() {
        return codgHotel;
    }

    public void setCodgHotel(String codgHotel) {
        this.codgHotel = codgHotel;
    }

    public String getUsuarioCriacao() {
        return usuarioCriacao;
    }

    public void setUsuarioCriacao(String usuarioCriacao) {
        this.usuarioCriacao = usuarioCriacao;
    }

    public String getNomeSistema() {
        return nomeSistema;
    }

    public void setNomeSistema(String nomeSistema) {
        this.nomeSistema = nomeSistema;
    }

    public Date getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(Date dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public Date getDataLimitePagamento() {
        return dataLimitePagamento;
    }

    public void setDataLimitePagamento(Date dataLimitePagamento) {
        this.dataLimitePagamento = dataLimitePagamento;
    }

    public Date getDataPagamento() {
        return dataPagamento;
    }

    public void setDataPagamento(Date dataPagamento) {
        this.dataPagamento = dataPagamento;
    }

    public Date getDataCancelamento() {
        return dataCancelamento;
    }

    public void setDataCancelamento(Date dataCancelamento) {
        this.dataCancelamento = dataCancelamento;
    }

    public String getLocalizador() {
        return localizador;
    }

    public void setLocalizador(String localizador) {
        this.localizador = localizador;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getStatusPagamento() {
        return statusPagamento;
    }

    public void setStatusPagamento(Integer statusPagamento) {
        this.statusPagamento = statusPagamento;
    }

    public Integer getCodgAgencia() {
        return codgAgencia;
    }

    public void setCodgAgencia(Integer codgAgencia) {
        this.codgAgencia = codgAgencia;
    }

    public String getDescricaMotivoCancelamento() {
        return descricaMotivoCancelamento;
    }

    public void setDescricaMotivoCancelamento(String descricaMotivoCancelamento) {
        this.descricaMotivoCancelamento = descricaMotivoCancelamento;
    }

    public String getUsuarioCancelamento() {
        return usuarioCancelamento;
    }

    public void setUsuarioCancelamento(String usuarioCancelamento) {
        this.usuarioCancelamento = usuarioCancelamento;
    }

    public String getDescricaoRegraCancelamento() {
        return descricaoRegraCancelamento;
    }

    public void setDescricaoRegraCancelamento(String descricaoRegraCancelamento) {
        this.descricaoRegraCancelamento = descricaoRegraCancelamento;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Double getValorTotalReserva() {
        return valorTotalReserva;
    }

    public void setValorTotalReserva(Double valorTotalReserva) {
        this.valorTotalReserva = valorTotalReserva;
    }

    public HotelResponse getHotel() {
        return hotel;
    }

    public void setHotel(HotelResponse hotel) {
        this.hotel = hotel;
    }

    public TarifaHotel getTarifaHotel() {
        return tarifaHotel;
    }

    public void setTarifaHotel(TarifaHotel tarifaHotel) {
        this.tarifaHotel = tarifaHotel;
    }

    public FormaPagamentoModel getFormaPagamentoSelecionada() {
        return formaPagamentoSelecionada;
    }

    public void setFormaPagamentoSelecionada(FormaPagamentoModel formaPagamentoSelecionada) {
        this.formaPagamentoSelecionada = formaPagamentoSelecionada;
    }

    public List<FormaPagamentoModel> getFormasPagamentos() {
        return formasPagamentos;
    }

    public void setFormasPagamentos(List<FormaPagamentoModel> formasPagamentos) {
        this.formasPagamentos = formasPagamentos;
    }

    public RecebimentoModel getRecebimento() {
        return recebimento;
    }

    public void setRecebimento(RecebimentoModel recebimento) {
        this.recebimento = recebimento;
    }

    public List<RecebimentoModel> getRecebimentos() {
        return recebimentos;
    }

    public void setRecebimentos(List<RecebimentoModel> recebimentos) {
        this.recebimentos = recebimentos;
    }

    public List<HistoricoReserva> getHistorico() {
        return historico;
    }

    public void setHistorico(List<HistoricoReserva> historico) {
        this.historico = historico;
    }

    public Usuario getCodgUsuarioCriacao() {
        return codgUsuarioCriacao;
    }

    public void setCodgUsuarioCriacao(Usuario codgUsuarioCriacao) {
        this.codgUsuarioCriacao = codgUsuarioCriacao;
    }

    public Long getCodgReservaHotelDB() {
        return codgReservaHotelDB;
    }

    public void setCodgReservaHotelDB(Long codgReservaHotelDB) {
        this.codgReservaHotelDB = codgReservaHotelDB;
    }

    public Date getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(Date dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public Boolean getIsExibitBtnImprimir() {
        return isExibitBtnImprimir;
    }

    public void setIsExibitBtnImprimir(Boolean isExibitBtnImprimir) {
        this.isExibitBtnImprimir = isExibitBtnImprimir;
    }

    public Boolean getIsExibirBtnCancelarReserva() {
        return isExibirBtnCancelarReserva;
    }

    public void setIsExibirBtnCancelarReserva(Boolean isExibirBtnCancelarReserva) {
        this.isExibirBtnCancelarReserva = isExibirBtnCancelarReserva;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public void setMotivoCancelamento(String motivoCancelamento) {
        this.motivoCancelamento = motivoCancelamento;
    }

    public String getDescricaoMotivoCancelamento() {
        return descricaoMotivoCancelamento;
    }

    public void setDescricaoMotivoCancelamento(String descricaoMotivoCancelamento) {
        this.descricaoMotivoCancelamento = descricaoMotivoCancelamento;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public TarifaHotel getTarifaTotalReserva() {
        return tarifaTotalReserva;
    }

    public void setTarifaTotalReserva(TarifaHotel tarifaTotalReserva) {
        this.tarifaTotalReserva = tarifaTotalReserva;
    }

    public Boolean getIsExibirBtnVisualizarVoucher() {
        return isExibirBtnVisualizarVoucher;
    }

    public void setIsExibirBtnVisualizarVoucher(Boolean isExibirBtnVisualizarVoucher) {
        this.isExibirBtnVisualizarVoucher = isExibirBtnVisualizarVoucher;
    }

    public ReservaPacote getCodgReservaPacote() {
        return codgReservaPacote;
    }

    public void setCodgReservaPacote(ReservaPacote codgReservaPacote) {
        this.codgReservaPacote = codgReservaPacote;
    }

    public String getFonte() {
        return fonte;
    }

    public void setFonte(String fonte) {
        this.fonte = fonte;
    }

    @Override
    public String toString() {
        return "ReservaHotelModel{" + "codgReservaHotelDB=" + codgReservaHotelDB
                + ", codgHotel=" + codgHotel
                + ", usuarioCriacao=" + usuarioCriacao
                + ", codgUsuarioCriacao=" + codgUsuarioCriacao
                + ", nomeSistema=" + nomeSistema
                + ", dataCriacao=" + dataCriacao
                + ", dataLimitePagamento=" + dataLimitePagamento
                + ", dataPagamento=" + dataPagamento
                + ", dataCancelamento=" + dataCancelamento
                + ", localizador=" + localizador
                + ", status=" + status
                + ", statusPagamento=" + statusPagamento
                + ", codgAgencia=" + codgAgencia
                + ", descricaMotivoCancelamento=" + descricaMotivoCancelamento
                + ", usuarioCancelamento=" + usuarioCancelamento
                + ", descricaoRegraCancelamento=" + descricaoRegraCancelamento
                + ", msg=" + msg
                + ", valorTotalReserva=" + valorTotalReserva
                + ", hotel=" + hotel
                + ", tarifaHotel=" + tarifaHotel
                + ", formaPagamentoSelecionada=" + formaPagamentoSelecionada
                + ", formasPagamentos=" + formasPagamentos
                + ", recebimento=" + recebimento
                + ", recebimentos=" + recebimentos
                + ", historico=" + historico
                + '}';
    }

    public Boolean getIsReservaPacote() {
        return isReservaPacote;
    }

    public void setIsReservaPacote(Boolean isReservaPacote) {
        this.isReservaPacote = isReservaPacote;
    }

}

