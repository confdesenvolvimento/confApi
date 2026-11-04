package com.confApi.chatconfianca.operacional;

import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.chatconfianca.financeiro.ChatPermissaoFinanceiraService;
import com.confApi.db.confManager.faturas.FaturasService;
import com.confApi.db.confManager.faturas.dto.FaturaSicaRQ;
import com.confApi.db.confManager.faturas.dto.FaturaSicaRS;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Somente leitura: nao interpreta saldo, nao gera boleto e nao baixa documentos. */
@Service
public class ChatResumoOperacionalFinanceiroService {
    private static final String CODIGO = "VENCIMENTOS_FATURAS";
    private static final String TITULO = "Faturas com vencimento no periodo";
    private static final String FONTE = "SICA - consulta de faturas";
    private static final int LIMITE = 5;
    private static final int MAX_RETORNO = 10000;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final String COBERTURA = "Somente debitos faturados em aberto, nao cancelados, "
            + "com vencimento no periodo solicitado. A origem nao informa confirmacao de completude; "
            + "esta lista nao comprova ausencia de outras pendencias. Valores nao exibidos porque a origem nao informa a moeda.";
    private final FaturasService faturas;
    private final ChatPermissaoFinanceiraService permissao;
    @Value("${chat-confianca.resumo-operacional.financeiro-enabled:false}")
    private boolean enabled;

    @Autowired
    public ChatResumoOperacionalFinanceiroService(FaturasService faturas, ChatPermissaoFinanceiraService permissao) {
        this(faturas, permissao, false);
    }

    ChatResumoOperacionalFinanceiroService(FaturasService faturas, ChatPermissaoFinanceiraService permissao, boolean enabled) {
        this.faturas = faturas;
        this.permissao = permissao;
        this.enabled = enabled;
    }

    /** O orquestrador valida o escopo da sessao antes de chamar as fontes. */
    public ChatResumoOperacionalFonte consultarVencimentos(SessaoChatResponse sessao, LocalDate inicio, LocalDate fim) {
        if (!enabled) return indisponivel("A consulta financeira do resumo operacional nao esta habilitada.");
        if (!periodoValido(inicio, fim)) return indisponivel("O periodo financeiro precisa ter de um a sete dias validos.");
        Integer empresa = empresa(sessao);
        if (empresa == null) return indisponivel("Nao foi possivel confirmar a sessao e a identificacao financeira da agencia.");
        try {
            permissao.validar(sessao);
        } catch (RuntimeException ex) {
            // A fachada existente converte tanto negacao quanto falha tecnica em 403.
            // Nao afirmar falta de permissao nem ausencia de faturas sem essa distincao.
            return indisponivel("Nao foi possivel confirmar a permissao para consultar faturas neste momento.");
        }
        FaturaSicaRQ pedido = new FaturaSicaRQ();
        pedido.setEmpfat(Integer.toString(empresa));
        pedido.setInvoiceType("TODOS");
        pedido.setPagamento("ABERTO");
        pedido.setTipoData("DATA_VENCIMENTO");
        pedido.setDataInicio(DATA.format(inicio));
        pedido.setDataFim(DATA.format(fim));
        pedido.setDisabledAFaturar(false);
        final List<FaturaSicaRS> recebidas;
        try {
            recebidas = faturas.faturaSicaEstrita(pedido);
        } catch (RuntimeException ex) {
            return indisponivel("Nao foi possivel consultar as faturas na origem. Isso nao significa ausencia de pendencias.");
        }
        if (recebidas == null) return indisponivel("A origem financeira nao retornou uma lista valida.");
        if (recebidas.size() > MAX_RETORNO) return indisponivel("A origem financeira retornou dados alem do limite seguro para este resumo.");

        // Verificar a lista inteira ANTES de filtrar, ordenar ou limitar a exibicao.
        // Nunca ocultar uma falha de isolamento que esteja apos os primeiros itens.
        Map<Integer, Integer> ocorrencias = new HashMap<>();
        for (FaturaSicaRS fatura : recebidas) {
            if (fatura == null || fatura.getEmpfat() != empresa) {
                return indisponivel("Nao foi possivel validar a agencia dos registros financeiros retornados.");
            }
            if (fatura.getNumfat() > 0) ocorrencias.merge(fatura.getNumfat(), 1, Integer::sum);
        }

        List<FaturaSicaRS> elegiveis = new ArrayList<>();
        int incompletos = 0;
        for (FaturaSicaRS fatura : recebidas) {
            String situacao = normalizar(fatura.getSituacao());
            String pago = normalizar(fatura.getPago());
            // Exclusoes de natureza financeira: nao sao dividas nem falhas da consulta.
            if (fatura.isCancelado() || "sim".equals(pago) || situacao.contains("credito")
                    || situacao.contains("a faturar")) continue;
            LocalDate vencimento = data(fatura.getDataVen());
            if (fatura.getNumfat() <= 0 || ocorrencias.getOrDefault(fatura.getNumfat(), 0) != 1
                    || !"nao".equals(pago) || !debito(situacao) || vencimento == null
                    || !Double.isFinite(fatura.getValor()) || fatura.getValor() <= 0) {
                incompletos++;
                continue;
            }
            if (vencimento.isBefore(inicio) || vencimento.isAfter(fim)) continue;
            elegiveis.add(fatura);
        }
        elegiveis.sort(Comparator.comparing((FaturaSicaRS f) -> data(f.getDataVen()))
                .thenComparingInt(FaturaSicaRS::getNumfat));
        List<ChatResumoOperacionalFonte.Item> itens = new ArrayList<>();
        for (FaturaSicaRS fatura : elegiveis.subList(0, Math.min(elegiveis.size(), LIMITE))) {
            String produto = produto(fatura.getInvoiceType());
            String descricao = "Em aberto conforme a origem; valores nao exibidos (moeda nao informada).";
            if (produto != null) descricao = "Produto: " + ("AEREO".equals(produto) ? "aereo" : "terrestre") + ". " + descricao;
            itens.add(new ChatResumoOperacionalFonte.Item(Integer.toString(fatura.getNumfat()),
                    "Fatura " + fatura.getNumfat(), descricao, data(fatura.getDataVen()),
                    null, null, null, produto));
        }
        String cobertura = COBERTURA;
        if (incompletos > 0) cobertura += " Registros sem dados suficientes ou com numero duplicado foram omitidos; confira a Central de Faturas.";
        if (itens.isEmpty()) cobertura += " Nao foram retornados registros elegiveis para exibir neste resumo.";
        if (elegiveis.size() > LIMITE) cobertura += " Exibindo os primeiros " + LIMITE + " registros elegiveis por vencimento; consulte a Central de Faturas para detalhar.";
        return new ChatResumoOperacionalFonte(CODIGO, TITULO, ChatResumoOperacionalFonte.Status.INCOMPLETO,
                FONTE, cobertura, null, List.copyOf(itens), elegiveis.size() > LIMITE);
    }

    private static ChatResumoOperacionalFonte indisponivel(String motivo) {
        return new ChatResumoOperacionalFonte(CODIGO, TITULO, ChatResumoOperacionalFonte.Status.INDISPONIVEL,
                FONTE, motivo, null, List.of(), false);
    }

    private static boolean periodoValido(LocalDate inicio, LocalDate fim) {
        return inicio != null && fim != null && !fim.isBefore(inicio)
                && inicio.getYear() >= 1900 && fim.getYear() <= 2100
                && ChronoUnit.DAYS.between(inicio, fim) < 7;
    }

    private static Integer empresa(SessaoChatResponse sessao) {
        if (sessao == null || sessao.getUsuario() == null || sessao.getUsuario().getCodgUsuario() == null
                || sessao.getUsuario().getCodgUsuario() <= 0 || sessao.getAgencia() == null
                || sessao.getAgencia().getCodgAgencia() == null || sessao.getAgencia().getCodgAgencia() <= 0) return null;
        String erp = sessao.getAgencia().getCodgSistemaBackoffice();
        if (erp == null || !erp.matches("[0-9]{1,10}")) return null;
        try {
            int numero = Integer.parseInt(erp);
            return numero > 0 ? numero : null;
        } catch (NumberFormatException ex) { return null; }
    }

    private static boolean debito(String situacao) {
        return "faturada".equals(situacao) || "debito".equals(situacao) || "fatura debito".equals(situacao)
                || "faturada debito".equals(situacao);
    }

    private static String produto(String valor) {
        return switch (normalizar(valor)) {
            case "aereo", "aerea" -> "AEREO";
            case "terrestre" -> "TERRESTRE";
            default -> null;
        };
    }

    private static LocalDate data(String valor) {
        if (valor == null || !valor.matches("[0-9]{2}/[0-9]{2}/[0-9]{4}")) return null;
        try { return LocalDate.parse(valor, DATA); }
        catch (DateTimeParseException ex) { return null; }
    }

    private static String normalizar(String valor) {
        return Normalizer.normalize(valor == null ? "" : valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
