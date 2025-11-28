package com.confApi.endPoints.reservaAereo;


import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.endPoints.agencia.Agencia;
import com.confApi.endPoints.bilhete.BilheteResponse;
import com.confApi.endPoints.companhiaAerea.CompanhiaAereaResponse;
import com.confApi.endPoints.contato.ContatoResponse;
import com.confApi.endPoints.formaPagamento.FormaPagamentoResponse;
import com.confApi.endPoints.historicoReserva.HistoricoReservaResponse;
import com.confApi.endPoints.pagamento.PagamentoResponse;
import com.confApi.endPoints.passageiro.PassageiroResponse;
import com.confApi.endPoints.recebimento.RecebimentoResponse;
import com.confApi.endPoints.reservaPacote.ReservaPacoteResponse;
import com.confApi.endPoints.trechoReserva.TrechoReservaResponse;
import com.confApi.endPoints.usuario.UsuarioResponse;
import com.confApi.hub.aereo.ConsultarLocalizadorResponseHub;
import com.confApi.hub.aereo.PesquisaResponseHub;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class ReservaAereoResponse {
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
    private CompanhiaAereaResponse codgCompanhiaAerea;
    private String nomeAgencia;
    private Agencia agencia;
    private String nomeUnidade;
    private String usuarioCriacao;
    private UsuarioResponse usuarioCriacao2;
    private String usuarioCancelamento;
    private List<TrechoReservaResponse> trechos;

    private List<ContatoResponse> contatos;
    private List<PassageiroResponse> passageiros;
    private String motivoCancelamento = "Desistencia";
    private String descricaoMotivoCancelamento = "Desistencia";
    private Boolean isCancelarTktsAtivos = false;
    private FormaPagamentoResponse formaPagamentoSelecionada = new FormaPagamentoResponse();
    private List<FormaPagamentoResponse> formasPagamentos;
    private RecebimentoResponse recebimento;
    private List<RecebimentoResponse> recebimentos;
    private PagamentoResponse pagamento;
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
    private List<HistoricoReservaResponse> historico;
    private ReservaPacoteResponse reservaPacote;
    private Boolean isPacote = false;

    public ReservaAereoResponse(ConsultarLocalizadorResponseHub pesquisaResponseHubList,
                                ReservaAereo pesquisaResponseDb) {

        // garante listas inicializadas
        this.trechos = new ArrayList<>();
        this.contatos = new ArrayList<>();
        this.passageiros = new ArrayList<>();
        this.formasPagamentos = new ArrayList<>();
        this.recebimentos = new ArrayList<>();

        // 1) preenche a partir da API (hub) – equivalente ao populaReservaFromApi
        System.out.println("TESTE pesquisaResponseHubList: " + pesquisaResponseHubList);
        if (pesquisaResponseHubList != null) {
            preencherComHub(pesquisaResponseHubList);
        }

        // 2) overlay com dados do banco – equivalente ao populaReservaFromDB
        if (pesquisaResponseDb != null) {
            preencherComDb(pesquisaResponseDb);
        }
    }

    private void preencherComHub(ConsultarLocalizadorResponseHub hub) {
        if (hub.getReservas() == null || hub.getReservas().isEmpty()) {
            return;
        }

        var reservaApi = hub.getReservas().get(0);

        this.localizador = reservaApi.getLocalizador();
        this.sistema = reservaApi.getSistema() != null ? reservaApi.getSistema().toString() : null;
        this.dataCriacao = reservaApi.getDataCriacao();
        this.dataEmissao = reservaApi.getDataEmissao();
        this.statusReserva = reservaApi.getStatus();

        if (reservaApi.getViagens() != null && !reservaApi.getViagens().isEmpty()) {
            var viagem = reservaApi.getViagens().get(0);
            if (viagem.getCompanhia() != null) {
                this.companhiaAerea = viagem.getCompanhia().getDescricao();
                // se quiser já setar codgCompanhiaAerea:
                // this.codgCompanhiaAerea = new CompanhiaAereaResponse(viagem.getCompanhia().getCodigoIata(), viagem.getCompanhia().getDescricao());
            }
        }

        // Prazo de emissão -> prazoReserva (mesma lógica do legado com convertDataApi/prazoEmissao)
        if (reservaApi.getPrazoEmissao() != null) {
            this.prazoReserva = reservaApi.getPrazoEmissao();
        }

        // Contatos
        if (reservaApi.getContatos() != null && !reservaApi.getContatos().isEmpty()) {
            for (var c : reservaApi.getContatos()) {
                ContatoResponse cr = new ContatoResponse();
                cr.setNome(c.getNome());
                cr.setEmail(c.getEmail());
                cr.setNumeroDDD(c.getNumeroDDD());
                cr.setNumeroDDI(c.getNumeroDDI());
                cr.setNumeroTelefone(c.getNumeroTelefone());
                this.contatos.add(cr);
            }
        }

        // Passageiros
        System.out.println("reservaApi.getPassageiros(): " + reservaApi.getPassageiros().size());
        if (reservaApi.getPassageiros() != null && !reservaApi.getPassageiros().isEmpty()) {
            System.out.println("TESTE");
            for (var paxApi : reservaApi.getPassageiros()) {
                PassageiroResponse p = new PassageiroResponse();
                p.setNome(paxApi.getNome());
                p.setSobrenome(paxApi.getSobrenome());
                p.setFaixaEtaria(paxApi.getFaixaEtaria());

                if (paxApi.getDocumento() != null) {
                    p.getDocumento().setNumero(paxApi.getDocumento().getNumero());
                    p.getDocumento().setNacionalidade(paxApi.getDocumento().getNacionalidade());
                }

                if (paxApi.getNascimento() != null) {
                    p.setNascimento(paxApi.getNascimento().toString());
                }

                // Bilhetes
                if (paxApi.getBilhetes() != null && !paxApi.getBilhetes().isEmpty()) {
                    paxApi.getBilhetes().forEach(b -> {
                        BilheteResponse bilheteResponse = new BilheteResponse(b);
                        p.getBilhetes().add(bilheteResponse);
                    });
                    this.isExibirTkt = true;
                }
                System.out.println("PASSAGEIROS: " + p);
                this.passageiros.add(p);
            }
        }

        // Trechos (voos)
        if (reservaApi.getViagens() != null && !reservaApi.getViagens().isEmpty()) {
            for (var trechoApi : reservaApi.getViagens()) {
                TrechoReservaResponse t = new TrechoReservaResponse(trechoApi);
                this.trechos.add(t);
            }
        }

        // Valores / totais (se existirem no hub, similar ao seu código isVendaWooba)
        if (reservaApi.getValorReserva() != null &&
                reservaApi.getValorReserva().getValorBase() != null) {

            var vb = reservaApi.getValorReserva().getValorBase();
            this.valorTotalReserva = safeDouble(vb.getTotal());
            this.tarifaGeral = safeDouble(vb.getTarifa());
            this.taxaEmbarqueGeral = safeDouble(vb.getTaxaEmbarque());
            this.taxaDUGeral = safeDouble(vb.getTaxaDU());
            this.taxaAssento = safeDouble(vb.getTaxaAssento());
        }

    }

    /* ============================================================
       Preencher com dados do DB (equivalente ao populaReservaFromDB)
       ============================================================ */
    private void preencherComDb(ReservaAereo db) {

        this.codgReservaAereoDB = db.getCodgReservaAereo().longValue();

        // Se o DB tiver valores mais confiáveis de datas, sobrescreve
        if (db.getDataCriacao() != null) {
            this.dataCriacao = db.getDataCriacao();
        }
        if (db.getDataEmissao() != null) {
            this.dataEmissao = db.getDataEmissao();
            this.isEmitido = true;
        }
        if (db.getDataCancelamento() != null) {
            this.dataCancelamento = db.getDataCancelamento();
        }

        // Agência / unidade / usuário criação
        if (db.getCodgAgencia() != null) {
            this.agencia = new Agencia();
            this.agencia.setCodgAgencia(db.getCodgAgencia().getCodgAgencia());
            this.agencia.setNomeAgencia(db.getCodgAgencia().getNomeAgencia());
            this.nomeAgencia = db.getCodgAgencia().getNomeAgencia();
        }

        if (db.getCodgUsuarioCriacao() != null) {
            this.usuarioCriacao = db.getCodgUsuarioCriacao().getLoginUsuario();
            this.usuarioCriacao2 = new UsuarioResponse(db.getCodgUsuarioCriacao());
        }

        // Companhia aérea vinda do DB (se quiser sobrescrever ou complementar)
        if (db.getCodgCompanhiaAerea() != null) {
            this.codgCompanhiaAerea = new CompanhiaAereaResponse(db.getCodgCompanhiaAerea());
        }

        // Pacote
        if (db.getCodgReservaPacote() != null) {
            this.isPacote = true;
            this.reservaPacote = new ReservaPacoteResponse(db.getCodgReservaPacote());
        }

        // Passageiros – cruzando pelo nome/sobrenome
        if (db.getPassageiros() != null && !db.getPassageiros().isEmpty()
                && this.passageiros != null && !this.passageiros.isEmpty()) {

            db.getPassageiros().forEach(paxDb -> {

                // procura o passageiro correspondente vindo do HUB
                PassageiroResponse pResp = this.passageiros.stream()
                        .filter(p -> equalsIgnoreCase(p.getNome(), paxDb.getNomePassageiro())
                                && equalsIgnoreCase(p.getSobrenome(), paxDb.getSobrenomePassageiro()))
                        .findFirst()
                        .orElse(null);

                if (pResp == null) {
                    // se não achar pelo hub, cria um novo só com dados do DB
                    pResp = new PassageiroResponse();
                    pResp.setNome(paxDb.getNomePassageiro());
                    pResp.setSobrenome(paxDb.getSobrenomePassageiro());
                    this.passageiros.add(pResp);
                }

                // completa dados do passageiro com o que vem do banco
                if (pResp.getDocumento() == null && paxDb.getCpf() != null) {
                    pResp.getDocumento().setNumero(paxDb.getCpf());
                }

                if (paxDb.getSexo() != null) {
                    pResp.setSexo(paxDb.getSexo() == 1 ? "M" : "F");
                }

                // valores / tarifas
                if (paxDb.getReservaValores() != null) {
                    paxDb.getReservaValores().forEach(reservaValorDB -> {

                        // atualiza totais gerais
                        double tarifa = safeDouble(reservaValorDB.getValorTarifa());
                        double tarifaNet = safeDouble(reservaValorDB.getValorTarifaNet());
                        double taxaEmb = safeDouble(reservaValorDB.getValorTaxaEmbarque());
                        double taxaDu = safeDouble(reservaValorDB.getValorDu());
                        double taxaRav = safeDouble(reservaValorDB.getValorRav());
                        double taxaRc = safeDouble(reservaValorDB.getValorRc());
                        double taxaComb = safeDouble(reservaValorDB.getValorTaxaCombustivel());
                        double taxaAssento = safeDouble(reservaValorDB.getValorAssento());

                        this.valorTotalReserva += tarifa + tarifaNet + taxaEmb + taxaDu + taxaRav + taxaRc + taxaComb + taxaAssento;
                        this.tarifaGeral += tarifa;
                        this.tarifaNetGeral += tarifaNet;
                        this.taxaEmbarqueGeral += taxaEmb;
                        this.taxaDUGeral += taxaDu;
                        this.taxaRAVGeral += taxaRav;
                        this.taxaRCGeral += taxaRc;
                        this.taxaTxCombustivelGeral += taxaComb;
                        this.taxaAssento += taxaAssento;

                        if (this.taxaTxCombustivelGeral > 0.0) {
                            this.isExibirTxCombustivel = true;
                        }
                        if (this.taxaRAVGeral > 0.0) {
                            this.isExibirRav = true;
                        }
                        if (this.taxaRCGeral > 0.0) {
                            this.isExibirRC = true;
                        }
                    });
                }

                // Bilhetes do DB – evitando duplicar bilhetes já vindos da API
                if (paxDb.getBilhetes() != null) {
                    paxDb.getBilhetes().forEach(bilheteDb -> {
                        this.isExibirTkt = true;
                    });
                }
            });
        }

        // Recebimentos
        if (db.getRecebimentos() != null && !db.getRecebimentos().isEmpty()) {
            db.getRecebimentos().forEach(rdb -> {
                RecebimentoResponse r = new RecebimentoResponse();
                r.setCodgFormaPagamento(rdb.getCodgFormaPagto().getCodgFormaPagto());
                r.setNomeFormaPagamento(rdb.getCodgFormaPagto().getNomeFormaPagto());
                r.setValorPagamento(rdb.getValrRecebimento());
                r.setValorEntrada(rdb.getValrEntrada());
                r.setStatusRecebimento(rdb.getStatus());
                r.setDataRecebimento(rdb.getDataRecebimento());
                r.setCodgCodgRecebimento(rdb.getCodgRecebimento());
                r.setLink(rdb.getLink());
                // se houver dados de cartão, mapeie aqui também

                this.recebimentos.add(r);
            });
        }
    }

    /* ================== helpers ================== */

    private boolean equalsIgnoreCase(String a, String b) {
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }

    private double safeDouble(Double v) {
        return v == null ? 0.0 : v;
    }

    // mesmo conceito do convertDataApi do legado – ajuste para o formato real
    private Date convertDataApi(String dataApi) {
        // exemplo para "/Date(1771453500000-0300)/"
        try {
            String millisStr = dataApi.replaceAll("[^0-9]", "");
            long millis = Long.parseLong(millisStr);
            return new Date(millis);
        } catch (Exception e) {
            return null;
        }
    }

    // conversão do prazoEmissao (string no padrão "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Date converterPrazoEmissao(String prazoEmissao) {
        try {
            String formattedDate = prazoEmissao.replaceAll(":(?=[0-9]{2}$)", "");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            LocalDateTime ldt = LocalDateTime.parse(formattedDate, formatter);
            ZonedDateTime zdt = ldt.atZone(ZoneId.of("America/Sao_Paulo"));
            return Date.from(zdt.toInstant());
        } catch (Exception e) {
            return null;
        }
    }
}

