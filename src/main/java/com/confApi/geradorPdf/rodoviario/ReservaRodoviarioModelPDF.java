package com.confApi.geradorPdf.rodoviario;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class ReservaRodoviarioModelPDF implements Serializable {

    private static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final DateTimeFormatter HORA =
            DateTimeFormatter.ofPattern("HH:mm");

    // Dados gerais
    private Integer codgReservaRodoviario;
    private Integer codgAgencia;
    private Integer codgSistema;
    private String localizador;
    private Integer status;

    // Datas no formato utilizado pelo comprovante
    private String dataCriacao;
    private String dataEmissao;
    private String dataCancelamento;

    private String descricaoMotivoCancelamento;
    private String observacaoPublica;
    private String fonte;

    private AgenciaPDF agencia;

    private List<TrechoPDF> trechos = new ArrayList<>();

    private ValoresPDF valores = new ValoresPDF();

    private List<RecebimentoPDF> recebimentos = new ArrayList<>();

    public ReservaRodoviarioModelPDF() {
    }

    public ReservaRodoviarioModelPDF(
            JsonNode detalheManager
    ) {
        this(detalheManager, null);
    }

    /**
     * Recebe o detalhe completo do Manager.
     *
     * Não recebe o payload da compra nem o JSON do
     * comprovante que já está pronto para envio.
     *
     * usuarioConfDto é opcional e permite complementar
     * a agência quando seu ID corresponde ao da reserva.
     */
    public ReservaRodoviarioModelPDF(
            JsonNode detalheManager,
            JsonNode usuarioConfDto
    ) {
        if (detalheManager == null
            || !detalheManager.isObject()
            || !detalheManager.path("reserva").isObject()) {

            throw new IllegalArgumentException(
                    "Informe o detalhe completo do Manager."
            );
        }

        for (String lista : List.of(
                "trechos",
                "passageiros",
                "assentos",
                "bilhetes"
        )) {
            if (!detalheManager.path(lista).isArray()) {
                throw new IllegalArgumentException(
                        "O detalhe do Manager não contém a lista "
                        + lista
                        + "."
                );
            }
        }

        /*
         * Dados gerais da reserva
         */
        JsonNode reserva = detalheManager.path("reserva");

        this.codgReservaRodoviario =
                inteiro(reserva, "codgReservaRodoviario");

        this.codgAgencia =
                inteiro(reserva, "codgAgencia");

        this.codgSistema =
                inteiro(reserva, "codgSistema");

        this.localizador =
                texto(reserva, "localizador");

        this.status =
                inteiro(reserva, "status");

        this.dataCriacao = dataTexto(
                texto(reserva, "dataCriacao"),
                "dataCriacao"
        );

        this.dataEmissao = dataTexto(
                texto(reserva, "dataEmissao"),
                "dataEmissao"
        );

        this.dataCancelamento = dataTexto(
                texto(reserva, "dataCancelamento"),
                "dataCancelamento"
        );

        this.descricaoMotivoCancelamento =
                texto(reserva, "descMotivoCancelamento");

        this.observacaoPublica =
                texto(reserva, "observacaoPublica");

        this.fonte =
                texto(reserva, "fonte", "origemReserva");

        if (this.codgReservaRodoviario == null
            || this.localizador == null) {

            throw new IllegalArgumentException(
                    "Detalhe sem ID ou localizador da reserva."
            );
        }

        /*
         * Agência
         *
         * Não atribui a agência do usuário a uma reserva
         * pertencente a outra agência.
         */
        JsonNode agenciaOrigem = reserva.path("agencia");
        JsonNode agenciaUsuario = null;

        if (usuarioConfDto != null
            && usuarioConfDto.path("agencia").isObject()
            && this.codgAgencia != null
            && this.codgAgencia.equals(
                inteiro(
                        usuarioConfDto.path("agencia"),
                        "codgAgencia"
                )
        )) {
            agenciaUsuario = usuarioConfDto.path("agencia");
        }

        this.agencia = new AgenciaPDF();

        this.agencia.setCodgAgencia(this.codgAgencia);

        String nomeAgencia = texto(agenciaOrigem, "nomeAgencia");
        if (nomeAgencia == null) nomeAgencia = texto(reserva, "nomeAgencia");
        if (nomeAgencia == null) nomeAgencia = texto(agenciaUsuario, "nomeAgencia");
        this.agencia.setNomeAgencia(nomeAgencia);

        String logomarca = texto(agenciaOrigem, "logomarca");
        if (logomarca == null) logomarca = texto(reserva, "logomarca");
        if (logomarca == null) logomarca = texto(agenciaUsuario, "logomarca");
        this.agencia.setLogomarca(logomarca);

        /*
         * Trechos
         */
        Map<Integer, TrechoPDF> trechosPorId =
                new LinkedHashMap<>();

        for (JsonNode trecho : detalheManager.path("trechos")) {
            if (trecho == null || trecho.isNull()) {
                continue;
            }

            Integer id =
                    inteiro(trecho, "codgRodoviarioTrecho");

            if (id == null || trechosPorId.containsKey(id)) {
                throw new IllegalArgumentException(
                        "Trecho sem ID ou com ID duplicado."
                );
            }

            TrechoPDF item = new TrechoPDF();

            item.setCodgRodoviarioTrecho(id);

            item.setCodgCidadeOrigem(
                    inteiro(trecho, "codgCidadeOrigem")
            );

            item.setCodgCidadeDestino(
                    inteiro(trecho, "codgCidadeDestino")
            );

            item.setNomeEmpresa(
                    texto(trecho, "nomeEmpresa", "viacao")
            );

            item.setUrlLogo(
                    texto(trecho, "urlLogo")
            );

            item.setCategoria(
                    texto(trecho, "categoria")
            );

            item.setNomeOrigem(
                    texto(
                            trecho,
                            "nomeOrigem",
                            "origemDescricao",
                            "origem"
                    )
            );

            item.setNomeDestino(
                    texto(
                            trecho,
                            "nomeDestino",
                            "destinoDescricao",
                            "destino"
                    )
            );

            item.setDataPartida(
                    dataTexto(
                            texto(trecho, "dataPartida"),
                            "dataPartida"
                    )
            );

            item.setHoraPartida(
                    horaTexto(
                            texto(trecho, "horaPartida"),
                            "horaPartida"
                    )
            );

            item.setDataChegada(
                    dataTexto(
                            texto(trecho, "dataChegada"),
                            "dataChegada"
                    )
            );

            item.setHoraChegada(
                    horaTexto(
                            texto(trecho, "horaChegada"),
                            "horaChegada"
                    )
            );

            trechosPorId.put(id, item);
        }

        if (trechosPorId.isEmpty()) {
            throw new IllegalArgumentException(
                    "Reserva sem trechos para montar o comprovante."
            );
        }

        /*
         * Passageiros
         *
         * Relaciona pelo ID do trecho, não pela posição
         * dos objetos nas listas.
         */
        Map<Integer, PassageiroPDF> passageirosPorId =
                new LinkedHashMap<>();

        Map<Integer, Integer> trechoDoPassageiro =
                new HashMap<>();

        for (JsonNode passageiro : detalheManager.path("passageiros")) {
            if (passageiro == null || passageiro.isNull()) {
                continue;
            }

            Integer id =
                    inteiro(passageiro, "codgPassageiro");

            Integer trechoId =
                    inteiro(passageiro, "codgRodoviarioTrecho");

            if (id == null
                || passageirosPorId.containsKey(id)
                || trechoId == null
                || !trechosPorId.containsKey(trechoId)) {

                throw new IllegalArgumentException(
                        "Passageiro sem vínculo único com um trecho da reserva."
                );
            }

            PassageiroPDF item = new PassageiroPDF();

            item.setCodgPassageiro(id);

            String nome = texto(
                    passageiro,
                    "nomePassageiro",
                    "nome"
            );

            String sobrenome = texto(
                    passageiro,
                    "sobrenomePassageiro"
            );

            item.setNome(
                    (
                            (nome == null ? "" : nome)
                            + " "
                            + (sobrenome == null ? "" : sobrenome)
                    ).trim()
            );

            String documento = texto(
                    passageiro,
                    "cpf",
                    "documento"
            );

            String digitos = documento == null
                    ? ""
                    : documento.replaceAll("\\D", "");

            item.setDocumentoMascarado(
                    digitos.length() == 11
                            ? "***.***.***-" + digitos.substring(9)
                            : (documento == null
                            ? null
                            : "Documento informado")
            );

            passageirosPorId.put(id, item);
            trechoDoPassageiro.put(id, trechoId);

            trechosPorId.get(trechoId)
                    .getPassageiros()
                    .add(item);
        }

        /*
         * Poltronas
         */
        Map<Integer, Set<String>> poltronasPorPassageiro =
                new LinkedHashMap<>();

        for (JsonNode assento : detalheManager.path("assentos")) {
            if (assento == null || assento.isNull()) {
                continue;
            }

            Integer passageiroId = inteiro(
                    assento,
                    "codgRodoviarioPassageiro"
            );

            conferirVinculo(
                    passageiroId,
                    inteiro(assento, "codgRodoviarioTrecho"),
                    trechoDoPassageiro,
                    "Assento"
            );

            String numero = texto(assento, "numeroAssento");

            if (numero != null) {
                poltronasPorPassageiro
                        .computeIfAbsent(
                                passageiroId,
                                chave -> new LinkedHashSet<>()
                        )
                        .add(numero);
            }
        }

        for (Map.Entry<Integer, PassageiroPDF> item
                : passageirosPorId.entrySet()) {

            Set<String> poltronas =
                    poltronasPorPassageiro.getOrDefault(
                            item.getKey(),
                            Collections.emptySet()
                    );

            item.getValue().setPoltronas(
                    String.join(", ", poltronas)
            );
        }

        /*
         * Bilhetes
         */
        for (JsonNode bilhete : detalheManager.path("bilhetes")) {
            if (bilhete == null || bilhete.isNull()) {
                continue;
            }

            Integer passageiroId = inteiro(
                    bilhete,
                    "codgRodoviarioPassageiro"
            );

            conferirVinculo(
                    passageiroId,
                    inteiro(bilhete, "codgRodoviarioTrecho"),
                    trechoDoPassageiro,
                    "Bilhete"
            );

            BilhetePDF item = new BilhetePDF();

            item.setNumeroBilhete(
                    texto(
                            bilhete,
                            "numerBilhete",
                            "numeroBilhete",
                            "numero"
                    )
            );

            item.setLocalizadorBilhete(
                    texto(bilhete, "localizadorBilhete")
            );

            item.setStatus(
                    inteiro(bilhete, "status")
            );

            passageirosPorId.get(passageiroId)
                    .getBilhetes()
                    .add(item);
        }

        this.trechos = new ArrayList<>(trechosPorId.values());

        /*
         * Valores
         *
         * Mantém os valores recebidos. Não recalcula o
         * total nem substitui o total de venda pelo NET.
         */
        JsonNode valoresOrigem = detalheManager.path("valores");

        BigDecimal tarifa = decimal(valoresOrigem, "valorTarifa");
        if (tarifa == null || tarifa.signum() <= 0) {
            BigDecimal tarifaNet = decimal(valoresOrigem, "valorTarifaNet");
            if (tarifaNet != null && tarifaNet.signum() > 0) {
                tarifa = tarifaNet;
            }
        }
        this.valores.setValorTarifa(tarifa);

        this.valores.setValorTaxaEmbarque(
                decimal(valoresOrigem, "valorTaxaEmbarque")
        );

        this.valores.setDemaisTaxas(
                decimal(valoresOrigem, "demaisTaxas")
        );

        this.valores.setValorSeguro(
                decimal(valoresOrigem, "valorSeguro")
        );

        this.valores.setTaxaConveniencia(
                decimal(valoresOrigem, "taxaConveniencia")
        );

        this.valores.setValorFee(
                decimal(valoresOrigem, "valorFee")
        );

        this.valores.setValorTotalMkp(
                decimal(valoresOrigem, "valorTotalMkp")
        );

        /*
         * Recebimentos
         */
        JsonNode recebimentosOrigem =
                detalheManager.path("recebimentos");

        if (recebimentosOrigem.isArray()) {
            for (JsonNode recebimento : recebimentosOrigem) {
                if (recebimento == null || !recebimento.isObject()) {
                    continue;
                }

                RecebimentoPDF item = new RecebimentoPDF();

                item.setDataRecebimento(
                        dataSomente(
                                texto(
                                        recebimento,
                                        "dataRecebimento",
                                        "dataPagamento"
                                ),
                                "dataRecebimento"
                        )
                );

                item.setNomeFormaPagamento(nomeFormaPagamento(recebimento));

                String codigo = texto(recebimento, "status", "statusRecebimento");
                String descricao = texto(recebimento, "statusDescricao", "descricaoStatus");
                if ("1".equals(codigo)) {
                    item.setStatusDescricao("Confirmado");
                } else if ("0".equals(codigo)) {
                    item.setStatusDescricao("Cancelado");
                } else if ("2".equals(codigo)) {
                    item.setStatusDescricao("Aguardando");
                } else if ("Registrado".equalsIgnoreCase(descricao)
                           || "Pago".equalsIgnoreCase(descricao)) {
                    item.setStatusDescricao("Confirmado");
                } else {
                    item.setStatusDescricao(descricao != null ? descricao
                            : codigo != null ? codigo : "Não informado");
                }

                item.setValorPagamento(
                        decimal(
                                recebimento,
                                "valrRecebimento",
                                "valorPagamento",
                                "valor",
                                "valorRecebido",
                                "valorPago"
                        )
                );

                this.recebimentos.add(item);
            }
        }
    }

    /*
     * Métodos privados utilizados somente pelo construtor.
     */
    private static void conferirVinculo(
            Integer passageiroId,
            Integer trechoId,
            Map<Integer, Integer> vinculos,
            String tipo
    ) {
        if (passageiroId == null
            || !vinculos.containsKey(passageiroId)
            || (
                    trechoId != null
                    && !trechoId.equals(
                            vinculos.get(passageiroId)
                    )
            )) {

            throw new IllegalArgumentException(
                    tipo
                    + " sem vínculo consistente com passageiro/trecho."
            );
        }
    }

    private static String texto(
            JsonNode no,
            String... campos
    ) {
        if (no == null) {
            return null;
        }

        for (String campo : campos) {
            JsonNode valor = no.path(campo);

            if (valor.isValueNode()
                && !valor.isNull()
                && !valor.asText().isBlank()) {

                return valor.asText().trim();
            }
        }

        return null;
    }

    private static Integer inteiro(
            JsonNode no,
            String campo
    ) {
        String valor = texto(no, campo);

        if (valor == null) {
            return null;
        }

        try {
            return Integer.valueOf(valor);

        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Código inválido no campo " + campo + ".",
                    exception
            );
        }
    }

    private static BigDecimal decimal(
            JsonNode no,
            String... campos
    ) {
        String valor = texto(no, campos);

        if (valor == null) {
            return null;
        }

        try {
            return new BigDecimal(valor);

        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Valor inválido no campo " + campos[0] + ".",
                    exception
            );
        }
    }

    private static String nomeFormaPagamento(JsonNode origem) {
        if (origem == null || !origem.isObject()) {
            return null;
        }
        String nome = texto(origem, "nomeFormaPagamento", "descricaoFormaPagamento",
                "nomeFormaPagto", "descFormaPagto", "descricao", "nome");
        if (nome != null) {
            return nome;
        }
        for (String campo : List.of("formaDePagamento", "formaPagamento",
                "codgFormaPagto", "codgFormaPagamento", "codigo")) {
            JsonNode forma = origem.path(campo);
            if (forma.isObject()) {
                nome = nomeFormaPagamento(forma);
                if (nome != null) {
                    return nome;
                }
            } else if (forma.isValueNode() && !forma.isNull()) {
                String valor = forma.asText().trim();
                if (!valor.isEmpty() && !valor.matches("[0-9]+")) {
                    return valor;
                }
                switch (valor) {
                    case "1": return "Faturado";
                    case "2": return "Cartão";
                    case "5": return "Link de pagamento";
                    default: break;
                }
            }
        }
        return null;
    }

    private static String dataSomente(String valor, String campo) {
        String formatada = dataTexto(valor, campo);
        return formatada == null ? null : formatada.substring(0, 10);
    }

    private static String dataTexto(
            String valor,
            String campo
    ) {
        if (valor == null) {
            return null;
        }

        try {
            if (valor.length() == 10) {
                return LocalDate.parse(valor).format(DATA);
            }

            try {
                return OffsetDateTime.parse(valor)
                        .format(DATA_HORA);

            } catch (DateTimeParseException semOffset) {
                return LocalDateTime.parse(valor)
                        .format(DATA_HORA);
            }

        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Data inválida no campo "
                    + campo
                    + "; esperado ISO-8601 no detalhe do Manager.",
                    exception
            );
        }
    }

    private static String horaTexto(
            String valor,
            String campo
    ) {
        if (valor == null) {
            return null;
        }

        try {
            return LocalTime.parse(valor).format(HORA);

        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Horário inválido no campo " + campo + ".",
                    exception
            );
        }
    }

    @JsonIgnore
    public String getStatusDescricao() {
        if (status == null) {
            return "NÃO INFORMADO";
        }

        return switch (status) {
            case 0 -> "EMITIDA";
            case 1 -> "CONFIRMADA";
            case 2 -> "CANCELADA";
            default -> "STATUS " + status;
        };
    }

    @Data
    public static class AgenciaPDF implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer codgAgencia;
        private String nomeAgencia;
        private String logomarca;
    }

    @Data
    public static class TrechoPDF implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer codgRodoviarioTrecho;
        private Integer codgCidadeOrigem;
        private Integer codgCidadeDestino;

        private String nomeEmpresa;
        private String urlLogo;
        private String categoria;

        private String nomeOrigem;
        private String nomeDestino;

        private String dataPartida;
        private String horaPartida;
        private String dataChegada;
        private String horaChegada;

        private List<PassageiroPDF> passageiros =
                new ArrayList<>();
    }

    @Data
    public static class PassageiroPDF implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer codgPassageiro;
        private String nome;
        private String documentoMascarado;
        private String poltronas;

        private List<BilhetePDF> bilhetes =
                new ArrayList<>();
    }

    @Data
    public static class BilhetePDF implements Serializable {

        private static final long serialVersionUID = 1L;

        private String numeroBilhete;
        private String localizadorBilhete;
        private Integer status;

        @JsonIgnore
        public String getStatusDescricao() {
            if (status == null) {
                return "Não informado";
            }

            return switch (status) {
                case 1 -> "Confirmado";
                case 2 -> "Cancelado";
                default -> "Status " + status;
            };
        }
    }

    @Data
    public static class ValoresPDF implements Serializable {

        private static final long serialVersionUID = 1L;

        private BigDecimal valorTarifa;
        private BigDecimal valorTaxaEmbarque;
        private BigDecimal demaisTaxas;
        private BigDecimal valorSeguro;
        private BigDecimal taxaConveniencia;
        private BigDecimal valorFee;
        private BigDecimal valorTotalMkp;
    }

    @Data
    public static class RecebimentoPDF implements Serializable {

        private static final long serialVersionUID = 1L;

        private String dataRecebimento;
        private String nomeFormaPagamento;
        private String statusDescricao;
        private BigDecimal valorPagamento;
    }
}
