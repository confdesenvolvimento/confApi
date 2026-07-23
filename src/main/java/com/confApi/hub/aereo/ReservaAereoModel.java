package com.confApi.hub.aereo;

import com.confApi.aereo.dto.ConsultarLocalizadorResponse;
import com.confApi.aereo.dto.Reserva;
import com.confApi.aereo.eNums.StatusBilhete;
import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.bandeira.BandeiraService;
import com.confApi.db.confManager.companhiaAerea.CompanhiaAerea;
import com.confApi.db.confManager.historicoReserva.dto.HistoricoReserva;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.db.confManager.reservaPacote.ReservaPacote;
import com.confApi.db.confManager.reservaValor.ReservaValor;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.endPoints.contato.ContatoResponse;
import com.confApi.endPoints.formaPagamento.FormaPagamentoResponse;
import com.confApi.endPoints.historicoReserva.HistoricoReservaResponse;
import com.confApi.endPoints.passageiro.PassageiroResponse;
import com.confApi.endPoints.recebimento.RecebimentoResponse;
import com.confApi.endPoints.reservaAereo.ReservaAereoResponse;
import com.confApi.endPoints.trechoReserva.TrechoReservaResponse;
import com.confApi.hub.aereo.dto.Contato;
import com.confApi.hub.aereo.dto.DocumentoPassageiro;
import com.confApi.hub.aereo.dto.Passageiro;
import com.confApi.hub.aereo.dto.TrechoReserva;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
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

    private String fonte="CONF_HUB";

    public ReservaAereoModel(ReservaAereoModel reservaAereoModel) {
        this.codgReservaAereoDB = reservaAereoModel.getCodgReservaAereoDB();
        this.localizador = reservaAereoModel.getLocalizador();
        this.statusReserva = reservaAereoModel.getStatusReserva();
        this.dataCriacao = reservaAereoModel.getDataCriacao();
        this.dataEmissao = reservaAereoModel.getDataEmissao();
        this.prazoReserva = reservaAereoModel.getPrazoReserva();
        this.dataCancelamento = reservaAereoModel.getDataCancelamento();
        this.descMotivoCancelamento = reservaAereoModel.getDescMotivoCancelamento();
        this.regraReserva = reservaAereoModel.getRegraReserva();
        this.sistema = reservaAereoModel.getSistema();
        this.companhiaAerea = reservaAereoModel.getCompanhiaAerea();
        this.codgCompanhiaAerea = reservaAereoModel.getCodgCompanhiaAerea();
        this.nomeAgencia = reservaAereoModel.getNomeAgencia();
        this.agencia = reservaAereoModel.getAgencia();
        this.nomeUnidade = reservaAereoModel.getNomeUnidade();
        this.usuarioCriacao = reservaAereoModel.getUsuarioCriacao();
        this.usuarioCriacao2 = reservaAereoModel.getUsuarioCriacao2();
        this.usuarioCancelamento = reservaAereoModel.getUsuarioCancelamento();
        this.trechos = reservaAereoModel.getTrechos();
        this.contatos = reservaAereoModel.getContatos();
        this.passageiros = reservaAereoModel.getPassageiros();
        this.motivoCancelamento = reservaAereoModel.getMotivoCancelamento();
        this.descricaoMotivoCancelamento = reservaAereoModel.getDescricaoMotivoCancelamento();
        this.isCancelarTktsAtivos = reservaAereoModel.getIsCancelarTktsAtivos();
        this.formaPagamentoSelecionada = reservaAereoModel.getFormaPagamentoSelecionada();
        this.formasPagamentos = reservaAereoModel.getFormasPagamentos();
        this.recebimento = reservaAereoModel.getRecebimento();
        this.recebimentos = reservaAereoModel.getRecebimentos();
        this.pagamento = reservaAereoModel.getPagamento();
        this.isEmitido = reservaAereoModel.getIsEmitido();
        this.tarifaGeral = reservaAereoModel.getTarifaGeral();
        this.tarifaNetGeral = reservaAereoModel.getTarifaNetGeral();
        this.taxaEmbarqueGeral = reservaAereoModel.getTaxaEmbarqueGeral();
        this.taxaDUGeral = reservaAereoModel.getTaxaDUGeral();
        this.taxaRAVGeral = reservaAereoModel.getTaxaRAVGeral();
        this.taxaRCGeral = reservaAereoModel.getTaxaRCGeral();
        this.taxaAssento = reservaAereoModel.getTaxaAssento();
        this.taxaTxCombustivelGeral = reservaAereoModel.getTaxaTxCombustivelGeral();
        this.valorTotalReserva = reservaAereoModel.getValorTotalReserva();
        this.isExibirTkt = reservaAereoModel.getIsExibirTkt();
        this.isExibirBtnCancelarTkt = reservaAereoModel.getIsExibirBtnCancelarTkt();
        this.isExibirBtnCancelarReserva = reservaAereoModel.getIsExibirBtnCancelarReserva();
        this.isExibirBtnMarcarAssento = reservaAereoModel.getIsExibirBtnMarcarAssento();
        this.isExibitBtnImprimir = reservaAereoModel.getIsExibitBtnImprimir();
        this.isExibirRav = reservaAereoModel.getIsExibirRav();
        this.isExibirRC = reservaAereoModel.getIsExibirRC();
        this.isExibirTxCombustivel = reservaAereoModel.getIsExibirTxCombustivel();
        this.msg = reservaAereoModel.getMsg();
        this.historico = reservaAereoModel.getHistorico();
        this.reservaPacote = reservaAereoModel.getReservaPacote();
        this.isPacote = reservaAereoModel.getIsPacote();
        this.fonte = reservaAereoModel.getFonte();
    }

    public ReservaAereoModel(ConsultarLocalizadorResponse consultarLocalizadorResponse, ReservaAereo reservaDB) {
        this();

        if (consultarLocalizadorResponse == null
                || consultarLocalizadorResponse.getReservas() == null
            || consultarLocalizadorResponse.getReservas().isEmpty()) {
            return;
        }

        Reserva reservaApi = consultarLocalizadorResponse.getReservas().get(0);

        this.localizador = reservaApi.getLocalizador();
        this.sistema = reservaApi.getSistema();
        this.dataCriacao = reservaApi.getDataCriacao();
        this.dataEmissao = reservaApi.getDataEmissao();
        this.statusReserva = reservaApi.getStatus();

        if (reservaApi.getViagens() != null && !reservaApi.getViagens().isEmpty()
                && reservaApi.getViagens().get(0).getCompanhia() != null) {
            this.companhiaAerea = reservaApi.getViagens().get(0).getCompanhia().getDescricao();
        }

        this.trechos = reservaApi.getViagens();

        this.contatos = new ArrayList<>();
        if (reservaApi.getContatos() != null) {
            for (Contato contato : reservaApi.getContatos()) {
                ContatoModel contatoModel = new ContatoModel(contato);
                this.contatos.add(contatoModel);
            }
        }

        this.passageiros = new ArrayList<>();
        if (reservaApi.getPassageiros() != null) {
            for (Passageiro passageiro : reservaApi.getPassageiros()) {
                PassageiroModel passageiroModel = new PassageiroModel(passageiro, reservaApi);
                this.passageiros.add(passageiroModel);
            }
        }

        if (reservaDB != null) {

            preencherIdsVoosFromDB(reservaDB);

            this.usuarioCriacao = reservaDB.getCodgUsuarioCriacao() != null
                    ? reservaDB.getCodgUsuarioCriacao().getLoginUsuario()
                    : null;

            this.usuarioCriacao2 = reservaDB.getCodgUsuarioCriacao();

            if (reservaDB.getCodgReservaAereo() != null) {
                this.codgReservaAereoDB = Long.valueOf(reservaDB.getCodgReservaAereo());
            }

            this.dataCriacao = reservaDB.getDataCriacao();

            if (this.localizador == null) {
                this.localizador = reservaDB.getLocalizador();
            }

            if (reservaDB.getDataEmissao() != null) {
                this.dataEmissao = reservaDB.getDataEmissao();
            }

            this.agencia = reservaDB.getCodgAgencia();
            this.codgCompanhiaAerea = reservaDB.getCodgCompanhiaAerea();

            if (reservaDB.getCodgReservaPacote() != null) {
                this.isPacote = true;
                this.reservaPacote = reservaDB.getCodgReservaPacote();
            } else {
                this.isPacote = false;
            }

            if (reservaDB.getDataCancelamento() != null) {
                this.dataCancelamento = reservaDB.getDataCancelamento();
            }

            if (reservaDB.getFonte() != null) {
                if (reservaDB.getFonte() == 1) {
                    this.fonte = "CONF_HUB";
                } else if (reservaDB.getFonte() == 0) {
                    this.fonte = "CONF_APP";
                } else if (reservaDB.getFonte() == 2) {
                    this.fonte = "PORTAL";
                }
            }

            this.recebimentos = new ArrayList<>();
            if (reservaDB.getRecebimentos() != null && !reservaDB.getRecebimentos().isEmpty()) {
                for (Recebimento recebimento1 : reservaDB.getRecebimentos()) {
                    RecebimentoModel recebimentoModel = new RecebimentoModel(recebimento1);
                    this.recebimentos.add(recebimentoModel);
                }
            }

            for (com.confApi.db.confManager.passageiro.Passageiro passageiroDB : reservaDB.getPassageiros()) {

                if (this.passageiros != null) {
                    for (PassageiroModel passageiroModel : this.passageiros) {

                        if (passageiroModel.getNome() != null
                                && passageiroModel.getSobrenome() != null
                                && passageiroDB.getNomePassageiro() != null
                                && passageiroDB.getSobrenomePassageiro() != null
                                && passageiroModel.getNome().equalsIgnoreCase(passageiroDB.getNomePassageiro())
                                && passageiroModel.getSobrenome().equalsIgnoreCase(passageiroDB.getSobrenomePassageiro())) {

                            passageiroModel.setCodgPassageiroDb(passageiroDB.getCodgPassageiro());

                            if (passageiroModel.getTelefone() == null) {
                                ContatoModel contato = new ContatoModel();
                                contato.setEmail(passageiroDB.getEmail());
                                contato.setNumeroTelefone(passageiroDB.getCelular());
                                passageiroModel.setTelefone(contato);
                            } else {
                                passageiroModel.getTelefone().setEmail(passageiroDB.getEmail());
                                passageiroModel.getTelefone().setNumeroTelefone(passageiroDB.getCelular());
                            }

                            if (passageiroModel.getDocumento() == null) {
                                DocumentoPassageiro documento = new DocumentoPassageiro();
                                documento.setNumero(passageiroDB.getCpf());
                                passageiroModel.setDocumento(documento);
                            } else if (passageiroDB.getCpf() != null) {
                                passageiroModel.getDocumento().setNumero(passageiroDB.getCpf());
                            }

                            if (passageiroDB.getSexo() != null) {
                                passageiroModel.setSexo(passageiroDB.getSexo() == 1 ? "M" : "F");
                            }

                            List<ReservaValoresAereo> valoresAntigos = passageiroModel.getValores();
                            passageiroModel.setValores(new ArrayList<>());

                            if (passageiroDB.getReservaValores() != null) {
                                for (ReservaValor reservaValorDB : passageiroDB.getReservaValores()) {

                                    ReservaValoresAereo reservaValoresAereo = new ReservaValoresAereo();

                                    reservaValoresAereo.setPercMkp(reservaValorDB.getPercMkp());
                                    reservaValoresAereo.setTaxaDu(reservaValorDB.getValorDu());
                                    reservaValoresAereo.setValorMkp(reservaValorDB.getValorMkp());
                                    reservaValoresAereo.setValorTarifa(reservaValorDB.getValorTarifa());
                                    reservaValoresAereo.setValorTarifaNet(reservaValorDB.getValorTarifaNet());
                                    reservaValoresAereo.setValorTaxaEmbarque(reservaValorDB.getValorTaxaEmbarque());
                                    reservaValoresAereo.setValorTxCombustivel(reservaValorDB.getValorTaxaCombustivel());
                                    reservaValoresAereo.setTaxaAssento(reservaValorDB.getValorAssento());

                                    if (valoresAntigos != null && !valoresAntigos.isEmpty()) {
                                        reservaValoresAereo.setTaxaRav(valoresAntigos.get(0).getTaxaRav());
                                        reservaValoresAereo.setTaxaRc(valoresAntigos.get(0).getTaxaRc());
                                    } else {
                                        reservaValoresAereo.setTaxaRav(reservaValorDB.getValorRav());
                                        reservaValoresAereo.setTaxaRc(reservaValorDB.getValorRc());
                                    }

                                    Double taxaRav = reservaValoresAereo.getTaxaRav() != null
                                            ? reservaValoresAereo.getTaxaRav()
                                            : 0.0;

                                    Double taxaRc = reservaValoresAereo.getTaxaRc() != null
                                            ? reservaValoresAereo.getTaxaRc()
                                            : 0.0;

                                    this.taxaRAVGeral = this.taxaRAVGeral + taxaRav;
                                    this.taxaRCGeral = this.taxaRCGeral + taxaRc;

                                    if (this.taxaTxCombustivelGeral != null && this.taxaTxCombustivelGeral > 0.0) {
                                        this.isExibirTxCombustivel = true;
                                    }

                                    if (this.taxaRAVGeral != null && this.taxaRAVGeral > 0.0) {
                                        this.isExibirRav = true;
                                    }

                                    if (this.taxaRCGeral != null && this.taxaRCGeral > 0.0) {
                                        this.isExibirRC = true;
                                    }

                                    passageiroModel.getValores().add(reservaValoresAereo);
                                }
                            }

                            if (passageiroDB.getBilhetes() != null) {
                                if (passageiroModel.getBilhetes() == null) {
                                    passageiroModel.setBilhetes(new ArrayList<>());
                                }

                                for (com.confApi.db.confManager.bilhete.BilheteAereo bilheteDB : passageiroDB.getBilhetes()) {

                                    if (bilheteDB.getNumrBilhete() == null) {
                                        continue;
                                    }

                                    Boolean isExisteFornecedor = false;

                                    for (BilheteModel bilheteModel : passageiroModel.getBilhetes()) {
                                        if (bilheteModel.getNumeroBilhete() != null
                                                && bilheteModel.getNumeroBilhete().equalsIgnoreCase(bilheteDB.getNumrBilhete())) {
                                            isExisteFornecedor = true;
                                            break;
                                        }
                                    }

                                    if (!isExisteFornecedor) {
                                        BilheteModel b = new BilheteModel();

                                        b.setDataCancelamento(bilheteDB.getDataCancelamento());
                                        b.setDataEmissao(bilheteDB.getDataEmissao());
                                        b.setNumeroBilhete(bilheteDB.getNumrBilhete());

                                        if (bilheteDB.getStatus() != null && bilheteDB.getStatus() == 1) {
                                            b.setStatus(StatusBilhete.Ativo.statusBilhete);
                                        } else if (bilheteDB.getStatus() != null && bilheteDB.getStatus() == 0) {
                                            b.setStatus(StatusBilhete.Cancelado.statusBilhete);
                                        } else {
                                            b.setStatus(StatusBilhete.Indefinido.statusBilhete);
                                        }

                                        passageiroModel.getBilhetes().add(b);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public ReservaAereoModel(ReservaAereoResponse reservaAereoResponse) {
        this.codgReservaAereoDB = reservaAereoResponse.getCodgReservaAereoDB();
        this.localizador = reservaAereoResponse.getLocalizador();
        this.statusReserva = reservaAereoResponse.getStatusReserva();
        this.dataCriacao = reservaAereoResponse.getDataCriacao();
        this.dataEmissao = reservaAereoResponse.getDataEmissao();
        this.prazoReserva = reservaAereoResponse.getPrazoReserva();
        this.dataCancelamento = reservaAereoResponse.getDataCancelamento();
        this.descMotivoCancelamento = reservaAereoResponse.getDescMotivoCancelamento();
        this.regraReserva = reservaAereoResponse.getRegraReserva();
        this.sistema = reservaAereoResponse.getSistema();
        this.companhiaAerea = reservaAereoResponse.getCompanhiaAerea();
        this.codgCompanhiaAerea = new CompanhiaAerea(reservaAereoResponse.getCodgCompanhiaAerea());
        this.nomeAgencia = reservaAereoResponse.getNomeAgencia();
        this.agencia = reservaAereoResponse.getAgencia() != null ? new Agencia(reservaAereoResponse.getAgencia()) : null;
        this.nomeUnidade = reservaAereoResponse.getNomeUnidade();
        this.usuarioCriacao = reservaAereoResponse.getUsuarioCriacao();
        this.usuarioCriacao2 = new Usuario(reservaAereoResponse.getUsuarioCriacao2());
        this.usuarioCancelamento = reservaAereoResponse.getUsuarioCancelamento();
        this.trechos = new ArrayList<>();
        for(TrechoReservaResponse trechoReserva : reservaAereoResponse.getTrechos()){
            this.trechos.add(new TrechoReserva(trechoReserva));
        }
        this.contatos = new ArrayList<>();
        for (ContatoResponse contato : reservaAereoResponse.getContatos()) {
            this.contatos.add(new ContatoModel(contato));
        }
        this.passageiros = new ArrayList<>();
        for (PassageiroResponse passageiro : reservaAereoResponse.getPassageiros()) {
            this.passageiros.add(new PassageiroModel(passageiro));
        }
        this.motivoCancelamento = reservaAereoResponse.getMotivoCancelamento();
        this.descricaoMotivoCancelamento = reservaAereoResponse.getDescricaoMotivoCancelamento();
        this.isCancelarTktsAtivos = reservaAereoResponse.getIsCancelarTktsAtivos();
        this.formaPagamentoSelecionada = new FormaPagamentoModel(reservaAereoResponse.getFormaPagamentoSelecionada());
        this.formasPagamentos = new ArrayList<>();
        for(FormaPagamentoResponse formaPagamento : reservaAereoResponse.getFormasPagamentos()){
            this.formasPagamentos.add(new FormaPagamentoModel(formaPagamento));
        }
        this.recebimento = reservaAereoResponse.getRecebimento() != null ? new RecebimentoModel(reservaAereoResponse.getRecebimento()) : null;
        this.recebimentos = new ArrayList<>();
        for(RecebimentoResponse recebimento : reservaAereoResponse.getRecebimentos()){
            this.recebimentos.add(new RecebimentoModel(recebimento));
        }
        this.pagamento = reservaAereoResponse.getPagamento() != null ? new PagamentoModel(reservaAereoResponse.getPagamento()) : null;
        this.isEmitido = reservaAereoResponse.getIsEmitido();
        this.tarifaGeral = reservaAereoResponse.getTarifaGeral();
        this.tarifaNetGeral = reservaAereoResponse.getTarifaNetGeral();
        this.taxaEmbarqueGeral = reservaAereoResponse.getTaxaEmbarqueGeral();
        this.taxaDUGeral = reservaAereoResponse.getTaxaDUGeral();
        this.taxaRAVGeral = reservaAereoResponse.getTaxaRAVGeral();
        this.taxaRCGeral = reservaAereoResponse.getTaxaRCGeral();
        this.taxaAssento = reservaAereoResponse.getTaxaAssento();
        this.taxaTxCombustivelGeral = reservaAereoResponse.getTaxaTxCombustivelGeral();
        this.valorTotalReserva = reservaAereoResponse.getValorTotalReserva();
        this.isExibirTkt = reservaAereoResponse.getIsExibirTkt();
        this.isExibirBtnCancelarTkt = reservaAereoResponse.getIsExibirBtnCancelarTkt();
        this.isExibirBtnCancelarReserva = reservaAereoResponse.getIsExibirBtnCancelarReserva();
        this.isExibirBtnMarcarAssento = reservaAereoResponse.getIsExibirBtnMarcarAssento();
        this.isExibitBtnImprimir = reservaAereoResponse.getIsExibitBtnImprimir();
        this.isExibirRav = reservaAereoResponse.getIsExibirRav();
        this.isExibirRC = reservaAereoResponse.getIsExibirRC();
        this.isExibirTxCombustivel = reservaAereoResponse.getIsExibirTxCombustivel();
        this.msg = reservaAereoResponse.getMsg();
        this.historico = new ArrayList<>();
        if(reservaAereoResponse.getHistorico() != null) {
            for (HistoricoReservaResponse historicoReserva : reservaAereoResponse.getHistorico()) {
                this.historico.add(new HistoricoReserva(historicoReserva));
            }
        }
        this.reservaPacote = reservaAereoResponse.getReservaPacote() != null ? new ReservaPacote(reservaAereoResponse.getReservaPacote()) : null;
        this.isPacote = reservaAereoResponse.getIsPacote();
    }

    public void listarCartoes(FormaPagamentoModel formaPagamentoModel) {

        if (formaPagamentoModel.getBandeiras() == null) {
            formaPagamentoModel.setBandeiras(new ArrayList<>());
        } else {
            formaPagamentoModel.getBandeiras().clear();
        }
        formaPagamentoModel.getBandeiras().addAll(new BandeiraService().findByAll());
    }

    private void preencherIdsVoosFromDB(ReservaAereo reservaDB) {
        if (reservaDB == null
                || reservaDB.getTrechos() == null
                || reservaDB.getTrechos().isEmpty()
                || this.trechos == null
                || this.trechos.isEmpty()) {
            return;
        }

        for (TrechoReserva trechoApi : this.trechos) {
            if (trechoApi.getVoos() == null || trechoApi.getVoos().isEmpty()) {
                continue;
            }

            for (com.confApi.hub.aereo.dto.Voo vooApi : trechoApi.getVoos()) {
                if (vooApi == null || vooApi.getNumeroVoo() == null) {
                    continue;
                }

                for (com.confApi.db.confManager.trecho.Trecho trechoDB : reservaDB.getTrechos()) {
                    if (trechoDB.getVoos() == null || trechoDB.getVoos().isEmpty()) {
                        continue;
                    }

                    for (com.confApi.db.confManager.voo.Voo vooDB : trechoDB.getVoos()) {
                        if (vooDB == null) {
                            continue;
                        }

                        if (isMesmoVoo(vooApi, vooDB)) {
                            vooApi.setId(vooDB.getCodgVoo());
                            break;
                        }
                    }

                    if (vooApi.getId() != null) {
                        break;
                    }
                }
            }
        }
    }

    private boolean isMesmoVoo(com.confApi.hub.aereo.dto.Voo vooApi,
                               com.confApi.db.confManager.voo.Voo vooDB) {

        if (vooApi.getNumeroVoo() == null || vooDB.getNumeroVoo() == null) {
            return false;
        }

        return vooApi.getNumeroVoo().equalsIgnoreCase(vooDB.getNumeroVoo());
    }
}
