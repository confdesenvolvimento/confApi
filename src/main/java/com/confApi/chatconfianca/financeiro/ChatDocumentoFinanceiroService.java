package com.confApi.chatconfianca.financeiro;

import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import com.confApi.db.confManager.faturas.FaturasService;
import com.confApi.db.confManager.faturas.dto.FaturaRQ;
import com.confApi.db.confManager.faturas.dto.FaturaSicaRQ;
import com.confApi.db.confManager.faturas.dto.FaturaSicaRS;
import com.confApi.exception.RegraDeNegocioException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Fachada financeira: a identificacao ERP e os valores nunca vem do cliente/LLM. */
@Service
public class ChatDocumentoFinanceiroService {
    public static final String FATURA_PDF = "FATURA_PDF";
    public static final String BOLETO_PDF = "BOLETO_PDF";
    public static final String CONCILIACAO_CSV = "CONCILIACAO_CSV";
    private static final Set<String> TIPOS = Set.of(FATURA_PDF, BOLETO_PDF, CONCILIACAO_CSV);
    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private final FaturasService faturas;
    private final Clock clock;
    private final ChatPermissaoFinanceiraService permissao;

    @Autowired
    public ChatDocumentoFinanceiroService(FaturasService faturas, ChatPermissaoFinanceiraService permissao) {
        this(faturas, permissao, Clock.systemDefaultZone());
    }

    ChatDocumentoFinanceiroService(FaturasService faturas, ChatPermissaoFinanceiraService permissao, Clock clock) {
        this.faturas = faturas;
        this.permissao = permissao;
        this.clock = clock;
    }

    /** Somente leitura. Revalida inclusive registros que a origem nao deveria retornar. */
    public FaturaSicaRS consultarFatura(SessaoChatResponse sessao, int numero) {
        int empresa = empresaDaSessao(sessao);
        if (numero <= 0) throw erro(400, "Informe um numero de fatura valido.");
        permissao.validar(sessao);
        FaturaSicaRQ pedido = new FaturaSicaRQ();
        pedido.setEmpfat(Integer.toString(empresa));
        pedido.setInvoiceNumber(numero);
        pedido.setInvoiceType("TODOS");
        pedido.setPagamento("AMBOS");
        pedido.setTipoData("TODAS");
        pedido.setDisabledAFaturar(false);
        final List<FaturaSicaRS> encontradas;
        try {
            encontradas = faturas.faturaSicaEstrita(pedido);
        } catch (RuntimeException ex) {
            throw erro(502, "Nao foi possivel validar a fatura no servico financeiro. Tente novamente.");
        }
        if (encontradas == null) throw respostaInvalida();
        for (FaturaSicaRS f : encontradas) {
            if (f == null || f.getEmpfat() != empresa || f.getNumfat() != numero
                    || !Double.isFinite(f.getValor()) || !Set.of("sim", "nao").contains(normalizar(f.getPago()))) {
                throw respostaInvalida();
            }
        }
        if (encontradas.isEmpty()) throw erro(404, "Fatura nao localizada para a agencia da sessao.");
        if (encontradas.size() != 1) throw respostaInvalida();
        return encontradas.get(0);
    }

    /** Disponibilidade estimada pelo cadastro; o arquivo ainda precisa ser confirmado na origem. */
    public List<String> tiposDisponiveis(FaturaSicaRS f) {
        List<String> tipos = new ArrayList<>();
        if (f == null || f.getNumfat() <= 0 || f.isCancelado()
                || normalizar(f.getSituacao()).contains("a faturar")) return List.of();
        String situacao = normalizar(f.getSituacao());
        boolean credito = situacao.contains("credito");
        boolean debito = situacao.contains("faturada") && !credito;
        boolean paga = "sim".equals(normalizar(f.getPago()));
        if (credito || debito || paga) {
            tipos.add(FATURA_PDF);
            tipos.add(CONCILIACAO_CSV);
        }
        LocalDate vencimento = data(f.getDataVen());
        if (debito && "nao".equals(normalizar(f.getPago()))
                && "nao".equals(normalizar(f.getFatVenc()))
                && vencimento != null && !vencimento.isBefore(LocalDate.now(clock))
                && Double.isFinite(f.getValor()) && f.getValor() > 0) {
            tipos.add(BOLETO_PDF);
        }
        return List.copyOf(tipos);
    }

    /** Chamado exclusivamente pelo POST explicito de download, nunca ao listar/ajudar. */
    public Documento baixar(SessaoChatResponse sessao, int numero, String tipo) {
        if (!TIPOS.contains(tipo == null ? "" : tipo)) throw erro(400, "Tipo de documento financeiro invalido.");
        FaturaSicaRS fatura = consultarFatura(sessao, numero);
        if (!tiposDisponiveis(fatura).contains(tipo)) {
            throw erro(409, BOLETO_PDF.equals(tipo)
                    ? "Boleto indisponivel para esta situacao da fatura. Consulte o atendimento financeiro."
                    : "Documento indisponivel para esta situacao da fatura.");
        }
        FaturaRQ pedido = new FaturaRQ(fatura.getNumfat(), Integer.toString(empresaDaSessao(sessao)),
                fatura.getCondicaoPagamento(), fatura.getValor(), fatura.getDataVen());
        final byte[] conteudo;
        try {
            conteudo = switch (tipo) {
                case FATURA_PDF -> faturas.downloadPdfFatura(pedido);
                case BOLETO_PDF -> faturas.downloadPdfFaturaBoleto(pedido);
                default -> faturas.downloadCsvFatura(pedido);
            };
        } catch (RuntimeException ex) {
            throw erro(502, "Nao foi possivel obter o documento financeiro. Tente novamente.");
        }
        validarArquivo(conteudo, CONCILIACAO_CSV.equals(tipo));
        String prefixo = BOLETO_PDF.equals(tipo) ? "boleto-" : CONCILIACAO_CSV.equals(tipo) ? "conciliacao-" : "fatura-";
        return new Documento(conteudo, CONCILIACAO_CSV.equals(tipo) ? "text/csv" : "application/pdf",
                prefixo + numero + (CONCILIACAO_CSV.equals(tipo) ? ".csv" : ".pdf"));
    }

    private static void validarArquivo(byte[] bytes, boolean csv) {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) throw respostaInvalida();
        if (!csv) {
            String inicio = new String(bytes, 0, Math.min(bytes.length, 16), StandardCharsets.US_ASCII);
            String fim = new String(bytes, Math.max(0, bytes.length - 2048), Math.min(bytes.length, 2048), StandardCharsets.US_ASCII);
            if (!inicio.startsWith("%PDF-") || !fim.contains("%%EOF")) throw respostaInvalida();
            return;
        }
        // Preserva o encoding do exportador legado. Nao modifica valores da conciliacao.
        String bruto = new String(bytes, StandardCharsets.ISO_8859_1);
        if (bruto.chars().anyMatch(c -> c < 32 && c != '\n' && c != '\r' && c != '\t')) throw respostaInvalida();
        String texto = bruto.trim();
        if (texto.startsWith("\u00ef\u00bb\u00bf")) texto = texto.substring(3).trim();
        String minusculo = texto.toLowerCase(Locale.ROOT);
        if (texto.isEmpty() || minusculo.startsWith("<") || texto.startsWith("{") || texto.startsWith("[")
                || minusculo.contains("<html") || minusculo.contains("<!doctype")
                || texto.chars().noneMatch(Character::isLetterOrDigit)
                || !(texto.contains(";") || texto.contains(",") || texto.contains("\t"))) {
            throw respostaInvalida();
        }
    }

    private static int empresaDaSessao(SessaoChatResponse sessao) {
        if (sessao == null || sessao.getUsuario() == null || sessao.getUsuario().getCodgUsuario() == null
                || sessao.getUsuario().getCodgUsuario() <= 0 || sessao.getAgencia() == null
                || sessao.getAgencia().getCodgAgencia() == null || sessao.getAgencia().getCodgAgencia() <= 0) {
            throw erro(403, "Sessao e agencia obrigatorias para acessar documentos financeiros.");
        }
        String erp = sessao.getAgencia().getCodgSistemaBackoffice();
        if (erp == null || !erp.matches("\\d{1,10}")) throw erro(403, "Agencia sem identificacao financeira valida.");
        try {
            int numero = Integer.parseInt(erp);
            if (numero > 0) return numero;
        } catch (NumberFormatException ignored) { }
        throw erro(403, "Agencia sem identificacao financeira valida.");
    }

    private static LocalDate data(String valor) {
        try { return valor == null ? null : LocalDate.parse(valor.trim(), DATA); }
        catch (DateTimeParseException ex) { return null; }
    }

    private static String normalizar(String valor) {
        return valor == null ? "" : Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").trim().toLowerCase(Locale.ROOT);
    }

    private static RegraDeNegocioException respostaInvalida() {
        return erro(502, "O servico financeiro nao retornou um documento ou cadastro valido. Tente novamente pela area Financeiro.");
    }

    private static RegraDeNegocioException erro(int status, String mensagem) {
        return new RegraDeNegocioException(status, mensagem);
    }

    public record Documento(byte[] conteudo, String contentType, String nomeArquivo) { }
}
