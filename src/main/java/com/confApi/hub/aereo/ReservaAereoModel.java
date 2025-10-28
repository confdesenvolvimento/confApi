package com.confApi.hub.aereo;

import com.confApi.db.confManager.agencia.Agencia;
import com.confApi.db.confManager.bandeira.BandeiraService;
import com.confApi.db.confManager.companhiaAerea.CompanhiaAerea;
import com.confApi.db.confManager.historicoReserva.HistoricoReserva;
import com.confApi.db.confManager.reservaPacote.ReservaPacote;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.hub.aereo.dto.TrechoReserva;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class ReservaAereoModel implements Serializable {

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
    private String nomeAgencia;
    private Agencia agencia;
    private String nomeUnidade;
    private String usuarioCriacao;
    private Usuario usuarioCriacao2;
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
              //  UtilDebug.sysError("Forma de pagamento Selecionada: " + recebimento.getCodgFormaPagamento());
                listarCartoes(recebimento.getFormaDePagamento());
                recebimento.setCartaoSelecionado(new CartaoModel());
            }
        }

    }

    public void listarCartoes(FormaPagamentoModel formaPagamentoModel) {

        if (formaPagamentoModel.getBandeiras() == null) {
            formaPagamentoModel.setBandeiras(new ArrayList<>());
        } else {
            formaPagamentoModel.getBandeiras().clear();
        }
        formaPagamentoModel.getBandeiras().addAll(new BandeiraService().findByAll());
    }

    public RecebimentoModel getRecebimento() {
        return recebimento;
    }

    public void setRecebimento(RecebimentoModel recebimento) {
        this.recebimento = recebimento;
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

    public PagamentoModel getPagamento() {
        return pagamento;
    }

    public void setPagamento(PagamentoModel pagamento) {
        this.pagamento = pagamento;
    }

    public Long getCodgReservaAereoDB() {
        return codgReservaAereoDB;
    }

    public void setCodgReservaAereoDB(Long codgReservaAereoDB) {
        this.codgReservaAereoDB = codgReservaAereoDB;
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

    public Boolean getIsCancelarTktsAtivos() {
        return isCancelarTktsAtivos;
    }

    public void setIsCancelarTktsAtivos(Boolean isCancelarTktsAtivos) {
        this.isCancelarTktsAtivos = isCancelarTktsAtivos;
    }

    public String getLocalizador() {
        return localizador;
    }

    public void setLocalizador(String localizador) {
        this.localizador = localizador;
    }

    public String getStatusReserva() {
        return statusReserva;
    }

    public void setStatusReserva(String statusReserva) {
        this.statusReserva = statusReserva;
    }

    public Date getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(Date dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public Date getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(Date dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public Date getPrazoReserva() {
        return prazoReserva;
    }

    public void setPrazoReserva(Date prazoReserva) {
        this.prazoReserva = prazoReserva;
    }

    public Date getDataCancelamento() {
        return dataCancelamento;
    }

    public void setDataCancelamento(Date dataCancelamento) {
        this.dataCancelamento = dataCancelamento;
    }

    public String getDescMotivoCancelamento() {
        return descMotivoCancelamento;
    }

    public void setDescMotivoCancelamento(String descMotivoCancelamento) {
        this.descMotivoCancelamento = descMotivoCancelamento;
    }

    public String getRegraReserva() {
        return regraReserva;
    }

    public void setRegraReserva(String regraReserva) {
        this.regraReserva = regraReserva;
    }

    public String getSistema() {
        return sistema;
    }

    public void setSistema(String sistema) {
        this.sistema = sistema;
    }

    public String getCompanhiaAerea() {
        return companhiaAerea;
    }

    public void setCompanhiaAerea(String companhiaAerea) {
        this.companhiaAerea = companhiaAerea;
    }

    public String getNomeAgencia() {
        return nomeAgencia;
    }

    public void setNomeAgencia(String nomeAgencia) {
        this.nomeAgencia = nomeAgencia;
    }

    public Agencia getAgencia() {
        return agencia;
    }

    public void setAgencia(Agencia agencia) {
        this.agencia = agencia;
    }

    public String getNomeUnidade() {
        return nomeUnidade;
    }

    public void setNomeUnidade(String nomeUnidade) {
        this.nomeUnidade = nomeUnidade;
    }

    public String getUsuarioCriacao() {
        return usuarioCriacao;
    }

    public void setUsuarioCriacao(String usuarioCriacao) {
        this.usuarioCriacao = usuarioCriacao;
    }

    public String getUsuarioCancelamento() {
        return usuarioCancelamento;
    }

    public void setUsuarioCancelamento(String usuarioCancelamento) {
        this.usuarioCancelamento = usuarioCancelamento;
    }

    public List<TrechoReserva> getTrechos() {
        return trechos;
    }

    public void setTrechos(List<TrechoReserva> trechos) {
        this.trechos = trechos;
    }

    public List<ContatoModel> getContatos() {
        return contatos;
    }

    public void setContatos(List<ContatoModel> contatos) {
        this.contatos = contatos;
    }

    public List<PassageiroModel> getPassageiros() {
        return passageiros;
    }

    public void setPassageiros(List<PassageiroModel> passageiros) {
        this.passageiros = passageiros;
    }

    public Boolean getIsEmitido() {
        return isEmitido;
    }

    public void setIsEmitido(Boolean isEmitido) {
        this.isEmitido = isEmitido;
    }

    public Double getValorTotalReserva() {
        return valorTotalReserva;
    }

    public void setValorTotalReserva(Double valorTotalReserva) {
        this.valorTotalReserva = valorTotalReserva;
    }

    @Override
    public String toString() {
        return "ReservaAereoModel{" + "codgReservaAereoDB=" + codgReservaAereoDB + ", localizador=" + localizador + ", statusReserva=" + statusReserva + ", dataCriacao=" + dataCriacao + ", dataEmissao=" + dataEmissao + ", prazoReserva=" + prazoReserva + ", dataCancelamento=" + dataCancelamento + ", descMotivoCancelamento=" + descMotivoCancelamento + ", regraReserva=" + regraReserva + ", sistema=" + sistema + ", companhiaAerea=" + companhiaAerea + ", nomeAgencia=" + nomeAgencia + ", nomeUnidade=" + nomeUnidade + ", usuarioCriacao=" + usuarioCriacao + ", usuarioCancelamento=" + usuarioCancelamento + ", trechos=" + trechos + ", contatos=" + contatos + ", passageiros=" + passageiros + ", motivoCancelamento=" + motivoCancelamento + ", descricaoMotivoCancelamento=" + descricaoMotivoCancelamento + ", isCancelarTktsAtivos=" + isCancelarTktsAtivos + ", formaPagamentoSelecionada=" + formaPagamentoSelecionada + ", formasPagamentos=" + formasPagamentos + ", recebimento=" + recebimento + ", pagamento=" + pagamento + ", isEmitido=" + isEmitido + ", valorTotalReserva=" + valorTotalReserva + '}';
    }

    public Double getTarifaGeral() {
        return tarifaGeral;
    }

    public void setTarifaGeral(Double tarifaGeral) {
        this.tarifaGeral = tarifaGeral;
    }

    public Double getTarifaNetGeral() {
        return tarifaNetGeral;
    }

    public void setTarifaNetGeral(Double tarifaNetGeral) {
        this.tarifaNetGeral = tarifaNetGeral;
    }

    public Double getTaxaEmbarqueGeral() {
        return taxaEmbarqueGeral;
    }

    public void setTaxaEmbarqueGeral(Double taxaEmbarqueGeral) {
        this.taxaEmbarqueGeral = taxaEmbarqueGeral;
    }

    public Double getTaxaDUGeral() {
        return taxaDUGeral;
    }

    public void setTaxaDUGeral(Double taxaDUGeral) {
        this.taxaDUGeral = taxaDUGeral;
    }

    public Double getTaxaRAVGeral() {
        return taxaRAVGeral;
    }

    public void setTaxaRAVGeral(Double taxaRAVGeral) {
        this.taxaRAVGeral = taxaRAVGeral;
    }

    public Double getTaxaRCGeral() {
        return taxaRCGeral;
    }

    public void setTaxaRCGeral(Double taxaRCGeral) {
        this.taxaRCGeral = taxaRCGeral;
    }

    public Double getTaxaTxCombustivelGeral() {
        return taxaTxCombustivelGeral;
    }

    public void setTaxaTxCombustivelGeral(Double taxaTxCombustivelGeral) {
        this.taxaTxCombustivelGeral = taxaTxCombustivelGeral;
    }

    public List<RecebimentoModel> getRecebimentos() {
        return recebimentos;
    }

    public void setRecebimentos(List<RecebimentoModel> recebimentos) {
        this.recebimentos = recebimentos;
    }

    public Boolean getIsExibirTkt() {
        return isExibirTkt;
    }

    public void setIsExibirTkt(Boolean isExibirTkt) {
        this.isExibirTkt = isExibirTkt;
    }

    public Boolean getIsExibirBtnCancelarTkt() {
        return isExibirBtnCancelarTkt;
    }

    public void setIsExibirBtnCancelarTkt(Boolean isExibirBtnCancelarTkt) {
        this.isExibirBtnCancelarTkt = isExibirBtnCancelarTkt;
    }

    public Boolean getIsExibirBtnCancelarReserva() {
        return isExibirBtnCancelarReserva;
    }

    public void setIsExibirBtnCancelarReserva(Boolean isExibirBtnCancelarReserva) {
        this.isExibirBtnCancelarReserva = isExibirBtnCancelarReserva;
    }

    public Boolean getIsExibirBtnMarcarAssento() {
        return isExibirBtnMarcarAssento;
    }

    public void setIsExibirBtnMarcarAssento(Boolean isExibirBtnMarcarAssento) {
        this.isExibirBtnMarcarAssento = isExibirBtnMarcarAssento;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public List<HistoricoReserva> getHistorico() {
        return historico;
    }

    public void setHistorico(List<HistoricoReserva> historico) {
        this.historico = historico;
    }

    public Boolean getIsExibitBtnImprimir() {
        return isExibitBtnImprimir;
    }

    public void setIsExibitBtnImprimir(Boolean isExibitBtnImprimir) {
        this.isExibitBtnImprimir = isExibitBtnImprimir;
    }

    public Boolean getIsExibirRav() {
        return isExibirRav;
    }

    public void setIsExibirRav(Boolean isExibirRav) {
        this.isExibirRav = isExibirRav;
    }

    public Boolean getIsExibirRC() {
        return isExibirRC;
    }

    public void setIsExibirRC(Boolean isExibirRC) {
        this.isExibirRC = isExibirRC;
    }

    public Boolean getIsExibirTxCombustivel() {
        return isExibirTxCombustivel;
    }

    public void setIsExibirTxCombustivel(Boolean isExibirTxCombustivel) {
        this.isExibirTxCombustivel = isExibirTxCombustivel;
    }

    public Double getTaxaAssento() {
        return taxaAssento;
    }

    public void setTaxaAssento(Double taxaAssento) {
        this.taxaAssento = taxaAssento;
    }

    public ReservaPacote getReservaPacote() {
        return reservaPacote;
    }

    public void setReservaPacote(ReservaPacote reservaPacote) {
        this.reservaPacote = reservaPacote;
    }

    public Boolean getIsPacote() {
        return isPacote;
    }

    public void setIsPacote(Boolean isPacote) {
        this.isPacote = isPacote;
    }

}
