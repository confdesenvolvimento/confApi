package com.confApi.chatconfianca.financeiro;

import com.confApi.db.confManager.faturas.dto.FaturaSicaRQ;
import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Filtros da consulta financeira; nunca recebe ou determina a agencia do usuario. */
public final class ChatFaturasFiltros {
    public static final Set<String> PARAMETROS = Set.of("faturaPagamento", "faturaTipoData",
            "faturaInicio", "faturaFim", "faturaProduto", "faturaNumero", "faturaDocumento");
    private static final DateTimeFormatter DATA_API = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATA_ENTRADA = DateTimeFormatter.ofPattern("d/M/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final String MESES = "janeiro|fevereiro|marco|abril|maio|junho|julho|agosto|setembro|outubro|novembro|dezembro";
    private static final List<String> NOMES_MESES = List.of(MESES.split("\\|"));
    private static final Pattern DATAS = Pattern.compile("(?<!\\d)(?:\\d{4}-\\d{2}-\\d{2}|\\d{1,2}/\\d{1,2}/\\d{4})(?!\\d)");
    private static final Pattern MES_NOME = Pattern.compile("\\b(" + MESES + ")(?:\\s+(?:de\\s+)?(\\d{4}))?\\b");
    private static final Pattern MES_NUMERO = Pattern.compile("(?<![\\d/-])(?:(\\d{4})-(\\d{2})|(\\d{1,2})/(\\d{4}))(?![\\d/-])");
    private static final Pattern NUMERO = Pattern.compile("\\b(?:fatura|boleto)(?:\\s+(?:numero|n[ou]?|de\\s+numero))?\\s*[:#º.]?\\s*(\\d+)\\b");

    private ChatFaturasFiltros() { }

    public record Consulta(FaturaSicaRQ request, String pergunta, Map<String, String> parametros) { }

    public static boolean pedidoExplicito(String texto) {
        String t = normalizar(texto);
        return !foraDeConsulta(t) && (t.matches(".*\\b(?:faturas?|boletos?)\\b.*")
                || t.matches(".*\\bhistorico\\s+financeiro\\b.*"));
    }

    /** Apenas fragmentos financeiros: uma nova pergunta sobre outro produto nao herda filtros. */
    public static boolean continuacao(String texto) {
        String t = normalizar(texto);
        if (t.isBlank() || t.length() > 220 || foraDeConsulta(t)) return false;
        if (pedidoExplicito(t)) return true;
        if (t.matches("(?:consultar|buscar|pesquisar|mostrar|listar|tentar)(?: novamente| de novo)?")) return true;
        if (t.matches("(?:como|onde)\\s+(?:eu\\s+)?(?:(?:posso|consigo)\\s+)?(?:pego|baixo|baixar|obtenho|obter|acesso|acessar)(?:\\s+(?:(?:a|o)\\s+)?(?:ela|ele|isso|pdf|documento|arquivo|boleto|csv))?(?:\\s+em\\s+(?:pdf|csv))?[?.!]*")) return true;
        if (t.matches("(?:me\\s+)?(?:mande|envie|mostre|quero|preciso)(?:\\s+(?:de|do|da))?\\s+(?:(?:o|a)\\s+)?(?:pdf|csv|boleto|documento|arquivo)[?.!]*")) return true;
        String resto = t.replaceAll("\\b(?:" + MESES + ")\\b", " ")
                .replaceAll("\\b(?:e|as?|os?|de|do|da|dos|das|em|no|na|nos|nas|para|por|entre|ate|desde|com|sem|somente|apenas|quero|ver|mostrar|listar|consultar|buscar|pesquisar|numero|n|agora|tambem|todas?|todos?|ambos|pagas?|pagos?|quitadas?|quitados?|abertas?|abertos?|pendentes?|historico|financeiro|aereas?|aereos?|terrestres?|emissao|emitidas?|emitidos?|vencimento|vencimentos|vencidas?|vencidos?|data|datas|periodo|dias?|mes|meses|ano|anos|ultimos?|ultimas?|passado|passada|anterior|este|esse|neste|nesse|deste|desse|atual|hoje|pdf|csv|boleto|boletos|documento|conciliacao|planilha)\\b", " ")
                .replaceAll("[\\d\\s.,;:!?/#º-]", "");
        return resto.isEmpty() && t.matches(".*(?:\\d|paga|pago|quitad|abert|pendent|historico|aere|terrestre|emissa|emitid|venc|data|periodo|mes|ano|hoje|pdf|csv|boleto|documento|conciliacao|planilha|tod[oa]s|ambos).*" );
    }

    public static Map<String, String> extrair(String texto, Map<String, String> anteriores, LocalDate hoje) {
        Map<String, String> p = filtrar(anteriores);
        String t = normalizar(texto);
        boolean negarPago = t.matches(".*\\bnao\\s+(?:(?:as|os)\\s+)?pag[oa]s?\\b.*");
        boolean negarAberto = t.matches(".*\\bnao\\s+(?:(?:as|os)\\s+)?abert[oa]s?\\b.*");
        boolean aberto = (t.matches(".*\\b(?:abert[oa]s?|pendentes?)\\b.*") || negarPago) && !negarAberto;
        boolean pago = (t.matches(".*\\b(?:pag[oa]s?|quitad[oa]s?)\\b.*") || negarAberto) && !negarPago;
        boolean ambos = t.matches(".*\\bambos\\b.*")
                || (!aberto && !pago && (t.matches(".*\\bhistorico\\b.*")
                || t.matches(".*\\b(?:todas|todos)\\s+(?:as\\s+|os\\s+)?(?:faturas|boletos)\\b.*")))
                || t.matches("(?:e\\s+)?(?:todas|todos)(?:\\s+elas)?[?.!]*") || (aberto && pago);
        if (ambos) p.put("faturaPagamento", "AMBOS");
        else if (aberto) p.put("faturaPagamento", "ABERTO");
        else if (pago) p.put("faturaPagamento", "PAGO");
        if (negarPago && negarAberto) p.put("faturaPagamento", "INDEFINIDO");

        boolean aereo = t.matches(".*\\baere[oa]s?\\b.*");
        boolean terrestre = t.matches(".*\\bterrestres?\\b.*");
        if (aereo || terrestre) p.put("faturaProduto", aereo && terrestre ? "TODOS" : aereo ? "AEREO" : "TERRESTRE");
        if (t.matches(".*\\b(?:todos\\s+os\\s+produtos|ambos\\s+os\\s+produtos)\\b.*")) p.put("faturaProduto", "TODOS");
        Matcher unsupported = Pattern.compile("\\b(hotel|hoteis|carros?|seguros?|pacotes?)\\b").matcher(t);
        if (unsupported.find()) p.put("faturaProduto", unsupported.group(1).toUpperCase(Locale.ROOT));

        boolean emissao = t.matches(".*\\b(?:emissao|emitid[oa]s?)\\b.*");
        boolean vencimento = t.matches(".*\\b(?:vencimento|vencimentos|vencid[oa]s?)\\b.*");
        if (emissao && vencimento) p.put("faturaTipoData", "AMBAS");
        else if (emissao) p.put("faturaTipoData", "DATA_EMISSAO");
        else if (vencimento) p.put("faturaTipoData", "DATA_VENCIMENTO");
        if (t.matches(".*\\b(?:data\\s+(?:de\\s+|do\\s+)?pagamento|data\\s+(?:de\\s+)?quitacao|pagamento\\s+realizado|pagamentos\\s+realizados)\\b.*")) {
            p.put("faturaTipoData", "DATA_PAGAMENTO");
        }
        if (t.matches(".*\\b(?:pdf|segunda\\s+via)\\b.*")) p.put("faturaDocumento", "PDF");
        if (t.matches(".*\\b(?:csv|conciliacao|planilha)\\b.*")) p.put("faturaDocumento", "CSV");
        if (t.matches(".*\\bboletos?\\b.*")) p.put("faturaDocumento", "BOLETO");
        if (t.matches(".*\\b(?:onde\\s+baixar|como\\s+(?:pego|baixo|baixar)|baixar\\s+(?:a\\s+)?fatura)\\b.*"))
            p.putIfAbsent("faturaDocumento", "PDF");

        Matcher numero = NUMERO.matcher(t);
        if (numero.find()) p.put("faturaNumero", numero.group(1));
        else if (t.matches("(?:numero\\s*)?\\d{1,12}") && !t.matches("(?:19|20)\\d{2}")) {
            p.put("faturaNumero", t.replaceAll("\\D", ""));
        }

        extrairPeriodo(t, p, hoje);
        // Um novo periodo de "pagas em..." nao pode herdar silenciosamente o eixo
        // do turno anterior: a API nao disponibiliza a data do pagamento.
        if (pago && !emissao && !vencimento && temPeriodoExplicito(t)
                && !"DATA_PAGAMENTO".equals(p.get("faturaTipoData"))) p.remove("faturaTipoData");
        if (t.matches(".*\\bvencid[oa]s?\\b.*")) {
            if (!pago) p.put("faturaPagamento", "ABERTO");
            p.putIfAbsent("faturaInicio", hoje.minusYears(1).toString());
            String limite = hoje.minusDays(1).toString();
            p.putIfAbsent("faturaFim", limite);
            try {
                if (LocalDate.parse(p.get("faturaFim")).isAfter(hoje.minusDays(1))) p.put("faturaFim", limite);
            } catch (DateTimeException ex) { /* A validacao recusa a data invalida, sem executar a consulta. */ }
        }
        return p;
    }

    private static void extrairPeriodo(String t, Map<String, String> p, LocalDate hoje) {
        Matcher matcher = DATAS.matcher(t);
        List<String> datas = new ArrayList<>();
        while (matcher.find()) datas.add(normalizarData(matcher.group()));
        if (!datas.isEmpty()) {
            if (datas.size() > 2) {
                p.put("faturaInicio", "INVALIDA"); p.remove("faturaFim"); return;
            }
            if (datas.size() == 2) {
                p.put("faturaInicio", datas.get(0)); p.put("faturaFim", datas.get(1)); return;
            }
            if (t.matches(".*\\b(?:ate|final|fim)\\b.*")) p.put("faturaFim", datas.get(0));
            else if (t.matches(".*\\b(?:desde|inicio|inicial)\\b.*")) p.put("faturaInicio", datas.get(0));
            else { p.put("faturaInicio", datas.get(0)); p.remove("faturaFim"); }
            return;
        }
        if (Pattern.compile("(?<![\\d/-])\\d{1,2}[/-]\\d{1,2}(?![\\d/-])").matcher(t).find()) {
            p.put("faturaInicio", "INVALIDA"); p.remove("faturaFim"); return;
        }
        Matcher ultimosDias = Pattern.compile("\\bultim[oa]s?\\s+(\\d{1,5})\\s+dias?\\b").matcher(t);
        if (ultimosDias.find()) {
            int dias = Integer.parseInt(ultimosDias.group(1));
            p.put("faturaInicio", dias > 0 ? hoje.minusDays(dias - 1L).toString() : "INVALIDA");
            p.put("faturaFim", hoje.toString()); return;
        }
        if (t.matches(".*\\b(?:mes\\s+(?:passado|anterior)|ultimo\\s+mes)\\b.*")) {
            aplicarMes(p, YearMonth.from(hoje).minusMonths(1)); return;
        }
        if (t.matches(".*\\b(?:(?:este|esse|neste|nesse|deste|desse)\\s+mes|mes\\s+atual)\\b.*")) {
            aplicarMes(p, YearMonth.from(hoje)); return;
        }
        if (t.matches(".*\\b(?:ano\\s+(?:passado|anterior)|ultimo\\s+ano)\\b.*")) {
            aplicarAno(p, hoje.getYear() - 1); return;
        }
        if (t.matches(".*\\b(?:(?:este|esse|neste|nesse|deste|desse)\\s+ano|ano\\s+atual)\\b.*")) {
            aplicarAno(p, hoje.getYear()); return;
        }
        Matcher mesNome = MES_NOME.matcher(t);
        if (mesNome.find()) {
            int mesInicio = NOMES_MESES.indexOf(mesNome.group(1)) + 1;
            String anoInicio = mesNome.group(2);
            boolean temFim = mesNome.find();
            String anoFinal = temFim ? mesNome.group(2) : null;
            int ano = anoInicio != null ? Integer.parseInt(anoInicio)
                    : anoFinal != null ? Integer.parseInt(anoFinal) : hoje.getYear();
            YearMonth inicio = YearMonth.of(ano, mesInicio);
            aplicarMes(p, inicio);
            if (temFim) {
                int anoFim = anoFinal == null ? ano : Integer.parseInt(anoFinal);
                p.put("faturaFim", YearMonth.of(anoFim, NOMES_MESES.indexOf(mesNome.group(1)) + 1).atEndOfMonth().toString());
                if (mesNome.find()) { p.put("faturaInicio", "INVALIDA"); p.remove("faturaFim"); }
            }
            return;
        }
        Matcher mesNumero = MES_NUMERO.matcher(t);
        if (mesNumero.find()) {
            try {
                int ano = Integer.parseInt(mesNumero.group(1) != null ? mesNumero.group(1) : mesNumero.group(4));
                int mes = Integer.parseInt(mesNumero.group(2) != null ? mesNumero.group(2) : mesNumero.group(3));
                aplicarMes(p, YearMonth.of(ano, mes));
                if (mesNumero.find()) {
                    int anoFim = Integer.parseInt(mesNumero.group(1) != null ? mesNumero.group(1) : mesNumero.group(4));
                    int mesFim = Integer.parseInt(mesNumero.group(2) != null ? mesNumero.group(2) : mesNumero.group(3));
                    p.put("faturaFim", YearMonth.of(anoFim, mesFim).atEndOfMonth().toString());
                    if (mesNumero.find()) { p.put("faturaInicio", "INVALIDA"); p.remove("faturaFim"); }
                }
            } catch (DateTimeException ex) { p.put("faturaInicio", "INVALIDA"); p.remove("faturaFim"); }
            return;
        }
        Matcher ano = Pattern.compile("\\b(?:ano\\s+(?:de\\s+)?|em\\s+|de\\s+)((?:19|20)\\d{2})\\b").matcher(t);
        if (ano.find() || t.matches("(?:19|20)\\d{2}")) {
            aplicarAno(p, Integer.parseInt(t.matches("(?:19|20)\\d{2}") ? t : ano.group(1))); return;
        }
        if (t.matches(".*\\b(?:hoje)\\b.*")) {
            p.put("faturaInicio", hoje.toString()); p.put("faturaFim", hoje.toString()); return;
        }
        if (t.matches(".*\\b(?:semestres?|trimestres?|bimestres?|semanas?)\\b.*")) {
            p.put("faturaInicio", "INVALIDA"); p.remove("faturaFim");
        }
    }

    public static Consulta validar(Map<String, String> parametros, LocalDate hoje) {
        Map<String, String> p = filtrar(parametros);
        String pagamento = padrao(p, "faturaPagamento", "ABERTO");
        String produto = padrao(p, "faturaProduto", "TODOS");
        if (!Set.of("ABERTO", "PAGO", "AMBOS").contains(pagamento))
            return perguntar(p, "Voce quer consultar faturas em aberto, pagas ou ambas?");
        if (!Set.of("TODOS", "AEREO", "TERRESTRE").contains(produto))
            return perguntar(p, "A consulta de faturas permite todos os produtos, aereo ou terrestre. Qual dessas opcoes deseja?");
        String documento = p.get("faturaDocumento");
        if (documento != null && !Set.of("PDF", "CSV", "BOLETO").contains(documento))
            return perguntar(p, "Voce precisa do PDF da fatura, da conciliacao CSV ou de um boleto?");
        Integer numero = null;
        if (p.containsKey("faturaNumero")) {
            try { numero = Integer.valueOf(p.get("faturaNumero")); }
            catch (NumberFormatException ex) { return perguntar(p, "Informe um numero de fatura valido, somente com digitos."); }
            if (numero <= 0 || !p.get("faturaNumero").matches("\\d+"))
                return perguntar(p, "Informe um numero de fatura valido, maior que zero.");
        }
        String tipoData = p.get("faturaTipoData");
        if (tipoData != null && !Set.of("DATA_EMISSAO", "DATA_VENCIMENTO").contains(tipoData))
            return perguntar(p, "DATA_PAGAMENTO".equals(tipoData)
                    ? "A consulta nao disponibiliza a data do pagamento. Deseja filtrar pela data de emissao ou de vencimento da fatura?"
                    : "Escolha um tipo de periodo: data de emissao ou data de vencimento da fatura.");
        boolean inicioPresente = p.containsKey("faturaInicio"), fimPresente = p.containsKey("faturaFim");
        if (!inicioPresente && !fimPresente) {
            if (!"ABERTO".equals(pagamento) || numero != null)
                return perguntar(p, "Qual periodo deseja consultar? Informe as datas inicial e final e se o periodo corresponde a emissao ou ao vencimento.");
            p.put("faturaInicio", hoje.minusYears(1).toString());
            p.put("faturaFim", hoje.plusDays(30).toString());
            if (tipoData == null) tipoData = "DATA_VENCIMENTO";
            p.put("faturaTipoData", tipoData);
        }
        if (inicioPresente != fimPresente)
            return perguntar(p, "Informe as duas datas do periodo: inicial e final.");
        LocalDate inicio, fim;
        try { inicio = LocalDate.parse(p.get("faturaInicio")); fim = LocalDate.parse(p.get("faturaFim")); }
        catch (DateTimeException | NullPointerException ex) {
            return perguntar(p, "Informe datas validas para o inicio e o fim do periodo, por exemplo 01/09/2026 e 30/09/2026.");
        }
        if (fim.isBefore(inicio)) return perguntar(p, "A data final precisa ser igual ou posterior a data inicial. Qual periodo deseja?");
        if (ChronoUnit.DAYS.between(inicio, fim) + 1 > 400)
            return perguntar(p, "Para consultar o historico, selecione um periodo de ate 400 dias por vez.");
        if (tipoData == null) return perguntar(p, "Esse periodo corresponde a data de emissao ou a data de vencimento das faturas?");
        FaturaSicaRQ request = new FaturaSicaRQ();
        request.setPagamento(pagamento); request.setInvoiceType(produto); request.setInvoiceNumber(numero);
        request.setTipoData(tipoData); request.setDataInicio(DATA_API.format(inicio)); request.setDataFim(DATA_API.format(fim));
        request.setDisabledAFaturar(false);
        return new Consulta(request, null, Map.copyOf(p));
    }

    private static Consulta perguntar(Map<String, String> p, String pergunta) { return new Consulta(null, pergunta, Map.copyOf(p)); }
    private static Map<String, String> filtrar(Map<String, String> origem) {
        Map<String, String> p = new LinkedHashMap<>();
        if (origem != null) origem.forEach((k, v) -> { if (k != null && PARAMETROS.contains(k) && v != null) p.put(k, v.trim()); });
        return p;
    }
    private static String padrao(Map<String, String> p, String chave, String valor) {
        if (!p.containsKey(chave)) p.put(chave, valor);
        return p.get(chave);
    }
    private static String normalizar(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
    private static boolean foraDeConsulta(String t) {
        return t.matches(".*\\b(?:atendente|atendimento|humano|pessoa|contato|telefone|whatsapp|email|e-mail|cancelar|cancelamento|cancelada|canceladas|pagar|quitar|baixar\\s+(?:o\\s+)?pagamento|dar\\s+baixa|efetuar\\s+pagamento|registrar\\s+pagamento)\\b.*");
    }
    private static boolean temPeriodoExplicito(String t) {
        return DATAS.matcher(t).find() || MES_NOME.matcher(t).find() || MES_NUMERO.matcher(t).find()
                || t.matches(".*\\b(?:mes|meses|ano|anos|dias|hoje|semestre|trimestre|bimestre|semana)\\b.*")
                || Pattern.compile("(?<![\\d/-])\\d{1,2}[/-]\\d{1,2}(?![\\d/-])").matcher(t).find();
    }
    private static String normalizarData(String texto) {
        try { return texto.contains("/") ? LocalDate.parse(texto, DATA_ENTRADA).toString() : LocalDate.parse(texto).toString(); }
        catch (DateTimeException ex) { return "INVALIDA"; }
    }
    private static void aplicarMes(Map<String, String> p, YearMonth mes) {
        p.put("faturaInicio", mes.atDay(1).toString()); p.put("faturaFim", mes.atEndOfMonth().toString());
    }
    private static void aplicarAno(Map<String, String> p, int ano) {
        p.put("faturaInicio", LocalDate.of(ano, 1, 1).toString()); p.put("faturaFim", LocalDate.of(ano, 12, 31).toString());
    }
}
