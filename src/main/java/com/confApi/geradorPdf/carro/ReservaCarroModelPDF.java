package com.confApi.geradorPdf.carro;

import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.db.confManager.carro.*;
import com.confApi.db.confManager.usuario.Usuario;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ReservaCarroModelPDF implements Serializable {

    private Integer codgReservaCarro;
    private Usuario usuario;
    private String localizadorSistema;
    private String localizadorLocadora;
    private String voucher;
    private Agencia agencia;
    private Integer statusReserva;
    private Integer statusPagamentoFornecedor;
    private Integer statusPagamentoCliente;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataEmissao;
    private LocalDateTime prazoEmissaoCliente;
    private LocalDateTime dataCancelamento;
    private Usuario usuarioCancelamento;
    private Double valorTotalReservaMarkup;
    private Double valorTarifaMarkup;
    private Double valorTaxas;
    private Double valorExtras;
    private LocalDateTime prazoCancelamento;
    private String observacaoInterna;
    private String observacaoPublica;
    private LocalDateTime dataRetirada;
    private LocalDateTime dataRetorno;
    private String horaRetirada;
    private String horaRetorno;
    private String descricaoMotivoCancelamento;
    private Double valorMultaCancelamento;
    private String codgLojaRetirada;
    private String cidadeRetirada;
    private String descricaoLojaRetirada;
    private String codgLojaDevolucao;
    private String cidadeDevolucao;
    private String descricaoLojaDevolucao;
    private String descricaoEnderecoRetirada;
    private String descricaoEnderecoDevolucao;
    private String fonte;

    // Dados do carro
    private String imagemCarro;
    private String companhiaCarroLogo;
    private String modelo;
    private String tipoGrupo;
    private String companhiaCarroName;
    private Boolean arCondicionado;
    private Integer numeroPassageiros;
    private Integer quantidadeBagagens;

    // Dados do condutor
    private String nomeCondutor;
    private String documentoCondutor;
    private String telefoneCondutor;
    private String emailCondutor;

    //adicionais
    private List<AdicionalCarroPDF> adicionais = new ArrayList<>();

    private String formaPagamento;
    private LocalDateTime dataPagamento;

    public ReservaCarroModelPDF() {
    }

    public ReservaCarroModelPDF(CarroReserva carroReserva) {
        this(null,
                carroReserva,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public ReservaCarroModelPDF(CarroReserva carroReserva, Carro carro, CarroValor carroValor, CarroCondutor carroCondutor, List<CarroItem> itens) {
        this(
                null,
                carroReserva,
                carro,
                carroValor,
                carroCondutor,
                itens,
                null,
                null,
                null
        );
    }

    public ReservaCarroModelPDF(Integer codgReservaCarro, CarroReserva carroReserva, Carro carro, CarroValor carroValor, CarroCondutor carroCondutor, List<CarroItem> itens, String companhiaCarroLogo, String formaPagamento, LocalDateTime dataPagamento) {
        this.codgReservaCarro = codgReservaCarro;
        this.adicionais = new ArrayList<>();
        this.companhiaCarroLogo = companhiaCarroLogo;
        this.formaPagamento = formaPagamento;
        this.dataPagamento = dataPagamento;

        /*
         * Dados gerais da reserva
         */
        if (carroReserva != null) {
            this.usuario = carroReserva.getUsuario();
            this.localizadorSistema = carroReserva.getLocalizadorSistema();
            this.localizadorLocadora = carroReserva.getLocalizadorLocadora();
            this.voucher = carroReserva.getVoucher();
            this.agencia = carroReserva.getAgencia();

            this.statusReserva = carroReserva.getStatusReserva();
            this.statusPagamentoFornecedor =
                    carroReserva.getStatusPagamentoFornecedor();
            this.statusPagamentoCliente =
                    carroReserva.getStatusPagamentoCliente();

            this.dataCriacao = carroReserva.getDataCriacao();
            this.dataEmissao = carroReserva.getDataEmissao();
            this.prazoEmissaoCliente =
                    carroReserva.getPrazoEmissaoCliente();

            this.dataCancelamento =
                    carroReserva.getDataCancelamento();
            this.usuarioCancelamento =
                    carroReserva.getUsuarioCancelamento();

            this.valorTotalReservaMarkup =
                    carroReserva.getValorTotalReservaMarkup();

            this.prazoCancelamento =
                    carroReserva.getPrazoCancelamento();

            this.observacaoInterna =
                    carroReserva.getObservacaoInterna();
            this.observacaoPublica =
                    carroReserva.getObservacaoPublica();

            this.dataRetirada = carroReserva.getDataRetirada();
            this.dataRetorno = carroReserva.getDataRetorno();
            this.horaRetirada = carroReserva.getHoraRetirada();
            this.horaRetorno = carroReserva.getHoraRetorno();

            this.descricaoMotivoCancelamento =
                    carroReserva.getDescricaoMotivoCancelamento();
            this.valorMultaCancelamento =
                    carroReserva.getValorMultaCancelamento();

            this.codgLojaRetirada =
                    carroReserva.getCodgLojaRetirada();
            this.cidadeRetirada =
                    carroReserva.getCidadeRetirada();
            this.descricaoLojaRetirada =
                    carroReserva.getDescricaoLojaRetirada();
            this.descricaoEnderecoRetirada =
                    carroReserva.getDescricaoEnderecoRetirada();

            this.codgLojaDevolucao =
                    carroReserva.getCodgLojaDevolucao();
            this.cidadeDevolucao =
                    carroReserva.getCidadeDevolucao();
            this.descricaoLojaDevolucao =
                    carroReserva.getDescricaoLojaDevolucao();
            this.descricaoEnderecoDevolucao =
                    carroReserva.getDescricaoEnderecoDevolucao();

            this.fonte = carroReserva.getFonte();
        }

        /*
         * Dados do veículo
         */
        if (carro != null) {
            this.modelo = carro.getDescModeloCarro();

            this.tipoGrupo =
                    carro.getDescricaoGrupo() != null
                            && !carro.getDescricaoGrupo().trim().isEmpty()
                            ? carro.getDescricaoGrupo()
                            : carro.getIdGrupo();

            this.companhiaCarroName = carro.getFornecedorNome();
            this.arCondicionado = carro.getArCondicionado();
            this.numeroPassageiros = carro.getQtdPassageiros();
            this.quantidadeBagagens = carro.getQtdBagagens();
        }

        /*
         * Valores da reserva
         *
         * Prioriza os valores em BRL. Caso não existam,
         * utiliza os valores na moeda original.
         */
        if (carroValor != null) {
            Double totalMarkup =
                    carroValor.getValorTotalReservaMarkupBrl() != null
                            ? carroValor.getValorTotalReservaMarkupBrl()
                            : carroValor.getValorTotalReservaMarkup();

            if (totalMarkup != null) {
                this.valorTotalReservaMarkup = totalMarkup;
            }

            this.valorTarifaMarkup =
                    carroValor.getValorTarifaMarkupBrl() != null
                            ? carroValor.getValorTarifaMarkupBrl()
                            : carroValor.getValorTarifaMarkup();

            this.valorTaxas =
                    carroValor.getValorTaxasBrl() != null
                            ? carroValor.getValorTaxasBrl()
                            : carroValor.getValorTaxas();
        }

        /*
         * Dados do condutor
         */
        if (carroCondutor != null) {
            String nome = carroCondutor.getNome() != null
                    ? carroCondutor.getNome().trim()
                    : "";

            String sobrenome = carroCondutor.getSobrenome() != null
                    ? carroCondutor.getSobrenome().trim()
                    : "";

            String nomeCompleto = (nome + " " + sobrenome).trim();

            this.nomeCondutor = !nomeCompleto.isEmpty()
                    ? nomeCompleto
                    : null;

            this.documentoCondutor =
                    carroCondutor.getNumrDocumento();

            this.emailCondutor =
                    carroCondutor.getEmail();

            StringBuilder telefone = new StringBuilder();

            if (carroCondutor.getNumrCodgPaisTelefone() != null
                    && !carroCondutor.getNumrCodgPaisTelefone()
                    .trim()
                    .isEmpty()) {

                telefone.append("+")
                        .append(
                                carroCondutor
                                        .getNumrCodgPaisTelefone()
                                        .trim()
                        );
            }

            if (carroCondutor.getNumrCodgAreaTelefone() != null
                    && !carroCondutor.getNumrCodgAreaTelefone()
                    .trim()
                    .isEmpty()) {

                if (telefone.length() > 0) {
                    telefone.append(" ");
                }

                telefone.append("(")
                        .append(
                                carroCondutor
                                        .getNumrCodgAreaTelefone()
                                        .trim()
                        )
                        .append(")");
            }

            if (carroCondutor.getNumrRelefone() != null
                    && !carroCondutor.getNumrRelefone()
                    .trim()
                    .isEmpty()) {

                if (telefone.length() > 0) {
                    telefone.append(" ");
                }

                telefone.append(
                        carroCondutor
                                .getNumrRelefone()
                                .trim()
                );
            }

            this.telefoneCondutor =
                    telefone.length() > 0
                            ? telefone.toString()
                            : null;
        }

        /*
         * Proteções e acessórios
         */
        if (itens != null && !itens.isEmpty()) {
            double totalAdicionais = 0.0;

            for (CarroItem item : itens) {
                if (item == null) {
                    continue;
                }

                AdicionalCarroPDF adicional =
                        new AdicionalCarroPDF();

                adicional.setDescricao(
                        item.getDescricao() != null
                                && !item.getDescricao().trim().isEmpty()
                                ? item.getDescricao()
                                : item.getCodgItem()
                );

                StringBuilder detalhes = new StringBuilder();

                if (item.getDescricaoPagmentoItem() != null
                        && !item.getDescricaoPagmentoItem()
                        .trim()
                        .isEmpty()) {

                    detalhes.append(
                            item.getDescricaoPagmentoItem().trim()
                    );
                }

                if (item.getQuantidade() != null) {
                    if (detalhes.length() > 0) {
                        detalhes.append(" | ");
                    }

                    detalhes.append("Quantidade: ")
                            .append(item.getQuantidade());
                }

                adicional.setDetalhes(
                        detalhes.length() > 0
                                ? detalhes.toString()
                                : null
                );

                Double totalItem;

                if (item.getValorMarkupBrl() != null) {
                    totalItem = item.getValorMarkupBrl();
                } else if (item.getValorMarkup() != null) {
                    totalItem = item.getValorMarkup();
                } else if (item.getValorNetBrl() != null) {
                    totalItem = item.getValorNetBrl();
                } else {
                    totalItem = item.getValorNet();
                }

                adicional.setTotal(totalItem);
                this.adicionais.add(adicional);

                if (totalItem != null) {
                    totalAdicionais += totalItem;
                }
            }

            this.valorExtras = totalAdicionais;
        }
    }
}
