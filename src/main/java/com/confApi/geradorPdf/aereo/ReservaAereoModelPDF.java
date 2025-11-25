package com.confApi.geradorPdf.aereo;

import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.companhiaAerea.CompanhiaAerea;
import com.confApi.db.confManager.historicoReserva.dto.HistoricoReserva;
import com.confApi.db.confManager.reservaPacote.ReservaPacote;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.hub.aereo.*;
import com.confApi.hub.aereo.dto.TrechoReserva;
import lombok.Data;

import java.util.Date;
import java.util.List;
@Data
public class ReservaAereoModelPDF {
    private Long codgReservaAereoDB;
    private String localizador;
    private String statusReserva;
    private Date dataCriacao;
    private Date dataEmissao;
    private Date prazoReserva;
    private Date dataCancelamento;
    private String descMotivoCancelamento;
    private String regraReserva;
    private String sistema;
    private String companhiaAerea;
    private CompanhiaAerea codgCompanhiaAerea;
    private Agencia agencia;
    private String nomeUnidade;
    private Usuario usuarioCriacao;
    private String usuarioCancelamento;
    private List<TrechoReserva> trechos;
    private List<ContatoModel> contatos;
    private List<PassageiroModel> passageiros;
    private String motivoCancelamento = "Desistencia";
    private String descricaoMotivoCancelamento = "Desistencia";
    private Boolean isCancelarTktsAtivos = false;
    private FormaPagamentoModel formaPagamentoSelecionada = new FormaPagamentoModel();
    private List<FormaPagamentoModel> formasPagamentos;
    private RecebimentoModel recebimento;
    private List<RecebimentoModel> recebimentos;
    private PagamentoModel pagamento;
    private Boolean isEmitido = false;

    private Double tarifaGeral = 0.0;
    private Double tarifaNetGeral = 0.0;
    private Double taxaEmbarqueGeral = 0.0;
    private Double taxaDUGeral = 0.0;
    private Double taxaRAVGeral = 0.0;
    private Double taxaRCGeral = 0.0;
    private Double taxaAssento = 0.0;
    private Double taxaTxCombustivelGeral = 0.0;
    private Double valorTotalReserva = 0.0;
    private Boolean isExibirTkt = false;
    private Boolean isExibirBtnCancelarTkt = false;
    private Boolean isExibirBtnCancelarReserva = true;
    private Boolean isExibirBtnMarcarAssento = true;
    private Boolean isExibitBtnImprimir = true;
    private Boolean isExibirRav = false;
    private Boolean isExibirRC = false;
    private Boolean isExibirTxCombustivel = false;
    private String msg;
    private List<HistoricoReserva> historico;
    private ReservaPacote reservaPacote;
    private Boolean isPacote = false;

    public ReservaAereoModelPDF(ReservaAereoModel reservaAereoModel) {
        this.codgReservaAereoDB = reservaAereoModel.getCodgReservaAereoDB();
        this.localizador = reservaAereoModel.getLocalizador();
        this.statusReserva = reservaAereoModel.getStatusReserva();
        this.dataCriacao = reservaAereoModel.getDataCriacao();
        this.dataEmissao = reservaAereoModel.getDataEmissao();
        this.prazoReserva = reservaAereoModel.getPrazoReserva();
        this.dataCancelamento = reservaAereoModel.getDataCancelamento();
        this.descMotivoCancelamento = reservaAereoModel.getDescricaoMotivoCancelamento();
        this.regraReserva = reservaAereoModel.getRegraReserva();
        this.sistema = reservaAereoModel.getSistema();
        this.companhiaAerea = reservaAereoModel.getCompanhiaAerea();
        this.codgCompanhiaAerea = reservaAereoModel.getCodgCompanhiaAerea();
        this.agencia = reservaAereoModel.getAgencia();

        this.nomeUnidade = reservaAereoModel.getNomeUnidade();
        this.usuarioCriacao = reservaAereoModel.getUsuarioCriacao2();
        this.usuarioCancelamento = reservaAereoModel.getUsuarioCancelamento();
        this.trechos = reservaAereoModel.getTrechos();
        this.contatos = reservaAereoModel.getContatos();
        this.passageiros = reservaAereoModel.getPassageiros();

        this.tarifaGeral = reservaAereoModel.getTarifaGeral();
        this.taxaEmbarqueGeral = reservaAereoModel.getTaxaEmbarqueGeral();
        this.taxaAssento = reservaAereoModel.getTaxaAssento();
        this.taxaDUGeral = reservaAereoModel.getTaxaDUGeral();
        this.taxaRAVGeral = reservaAereoModel.getTaxaRAVGeral();
        this.taxaRCGeral = reservaAereoModel.getTaxaRCGeral();
        this.taxaTxCombustivelGeral = reservaAereoModel.getTaxaTxCombustivelGeral();
        this.valorTotalReserva = reservaAereoModel.getValorTotalReserva();

        this.formasPagamentos = reservaAereoModel.getFormasPagamentos();
        this.recebimento = reservaAereoModel.getRecebimento();
        this.recebimentos = reservaAereoModel.getRecebimentos();
        this.pagamento = reservaAereoModel.getPagamento();
        this.msg = reservaAereoModel.getMsg();
        this.historico = reservaAereoModel.getHistorico();
        this.reservaPacote = reservaAereoModel.getReservaPacote();
    }
}
