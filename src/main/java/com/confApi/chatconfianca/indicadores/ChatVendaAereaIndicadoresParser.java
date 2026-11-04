package com.confApi.chatconfianca.indicadores;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Comandos fechados de leitura. Nao interpreta identidade, SQL, metodos, URLs ou acoes comerciais. */
public final class ChatVendaAereaIndicadoresParser {
    public record Pedido(VendaAereaIndicadoresFiltros filtros, String foco, String pergunta, boolean continuar) { }
    public static final ZoneId ZONA_OPERACIONAL = ZoneId.of("America/Cuiaba");
    private static final String F = "(?iu)";
    private static final List<String> MESES = List.of("janeiro", "fevereiro", "marco", "abril", "maio", "junho",
            "julho", "agosto", "setembro", "outubro", "novembro", "dezembro");
    private static final String PERIODO = "Qual período deseja consultar? Informe datas inicial e final, ou diga este mês, mês passado ou este ano.";
    private ChatVendaAereaIndicadoresParser() { }

    public static Pedido interpretar(String mensagem, VendaAereaIndicadoresEstado anterior) {
        return interpretar(mensagem, anterior, LocalDate.now(ZONA_OPERACIONAL));
    }

    public static Pedido interpretar(String mensagem, VendaAereaIndicadoresEstado anterior, LocalDate hoje) {
        if (mensagem == null || mensagem.isBlank() || mensagem.length() > 2000 || hoje == null) return null;
        String t = normalizar(mensagem);
        if (mudancaDeAssunto(t) || t.matches(".*\\bnao (?:quero |preciso |desejo )?(?:consultar|ver|saber|listar|agrupar|separar|buscar)\\b.*")) return null;
        boolean contexto = anterior != null && anterior.getFiltros() != null;
        boolean venda = palavra(t, "vendas?|vendi|vendemos|vendid[oa]s?|emiti|emitimos|emitid[oa]s?|emissoes|emissao");
        boolean contavel = palavra(t, "localizadores|reservas|bilhetes|passageiros|passageiras");
        boolean agregado = (venda && (palavra(t, "quanto|quantos|quantas|quantidade|total|valor|reais|faturamento|soma|somatorio|indicadores|resumo|relatorio") || t.contains("r$")))
                || (venda && agrupamento(t));
        if(palavra(t,"reserva|bilhete|localizador|pnr")&&!contavel&&!palavra(t,"quantos|quantas|quantidade"))return null;
        // Ultima venda/reserva continua no fluxo factual existente, nao em um agregado financeiro.
        if (!agregado && t.matches(".*\\b(?:ultim[oa]s?|recentes?) (?:\\d+ )?(?:vendas?|reservas?|localizadores?)\\b.*")) return null;
        String destino = lugar(mensagem, "(?:para\\s+(?:o\\s+destino\\s+)?|destino\\s*(?:[ée]\\s+|de\\s+|:\\s*)?)");
        String origem = lugar(mensagem, "(?:saindo\\s+de\\s+|origem\\s*(?:[ée]\\s+|de\\s+|:\\s*)?)");
        boolean nome = temNome(t);
        boolean detalhes = palavra(t, "liste|listar|listagem|detalhe|detalhar|detalhes")
                || (contexto && nome) || (contexto && t.matches(".*\\b(?:mostre|mostrar|traga|traz|veja) (?:essas|estas|as|todas as) vendas\\b.*"));
        boolean paginaProxima = t.matches("(?:mostre |veja )?(?:a )?(?:proxima pagina|pagina seguinte|mais resultados)[.!?]?");
        boolean paginaAnterior = t.matches("(?:volte |mostre |veja )?(?:a )?pagina anterior[.!?]?");
        boolean continuidade = contexto && (agrupamento(t) || nome || detalhes || temPeriodo(t) || paginaProxima || paginaAnterior || destino != null || origem != null
                || t.matches(".*\\b(?:nessas|nesses|dessas|desses|essas|esses|mesmas|mesmos)\\b.*")
                || t.matches("(?:e )?(?:qual (?:o )?total|quanto|quantos|quantas)[.!?]?")
                || palavra(t,"valor|reais|faturamento") || t.contains("r$")
                || t.matches(".*\\b(?:filtre|filtrar|somente|apenas)\\b.*")
                || companhiaConhecida(t) != null);
        if (!agregado && !continuidade) return null;
        if (t.matches(".*\\b(?:abrir|abra|abre|carregue|carregar) (?:a |essa |esta )?reserva\\b.*")) return null;
        boolean retomar = continuidade && (!agregado || t.matches("^(?:e |agora |nessas |nesses |dessas |desses ).*")
                || t.matches(".*\\b(?:nessas|nesses|dessas|desses|mesmas|mesmos)\\b.*"));
        VendaAereaIndicadoresFiltros filtros = retomar ? copiar(anterior.getFiltros()) : new VendaAereaIndicadoresFiltros();
        filtros.setPage(0);
        filtros.setSize(10);
        filtros.setDetalhar(detalhes);
        if (filtros.getAgrupamento() == null) filtros.setAgrupamento("NENHUM");
        String foco = detalhes ? "DETALHES" : palavra(t, "bilhetes") && palavra(t, "quantos|quantidade|total") ? "BILHETES"
                : palavra(t, "passageiros|passageiras") && palavra(t, "quantos|quantas|quantidade|total") ? "PASSAGEIROS"
                : contavel && palavra(t, "quantos|quantas|quantidade|total") ? "LOCALIZADORES"
                : t.matches(".*\\bquanto (?:eu )?(?:vendi|vendemos)\\b.*") || palavra(t,"valor|reais|dinheiro|faturamento") || t.contains("r$") ? "VALOR" : "RESUMO";
        boolean novoObjetivo = detalhes || (contavel && palavra(t,"quantos|quantas|quantidade|total"))
                || "VALOR".equals(foco) || palavra(t,"resumo|relatorio|indicadores");
        if(retomar && !novoObjetivo && anterior.getFoco()!=null
                && Set.of("RESUMO","VALOR","LOCALIZADORES","BILHETES","PASSAGEIROS","DETALHES").contains(anterior.getFoco()))
            foco=anterior.getFoco();
        String pergunta = null;
        if (agrupamento(t)) {
            if (palavra(t, "por destino|por cidade|por rota|por passageiro|por usuario|por agente|por mes|por dia"))
                pergunta = "Nesta versão posso separar por companhia aérea. Destino pode ser usado como filtro. Deseja separar por companhia ou informar um destino?";
            else if (palavra(t, "por companhia|por cia|por empresa|por aerea")) filtros.setAgrupamento("COMPANHIA");
            else if (palavra(t, "sem agrupar|sem agrupamento|sem separar|total geral|junte|juntar")) filtros.setAgrupamento("NENHUM");
            else pergunta = "Deseja ver o total geral ou separar por companhia aérea?";
        }
        if (palavra(t, "lucro|comissao|margem|liquido|liquida"))
            pergunta = "Estes indicadores mostram as emissões e os valores registrados, não lucro, comissão ou receita líquida. Deseja consultar esses indicadores de vendas?";
        if (palavra(t, "outra agencia|todas as agencias|qualquer agencia|outras agencias"))
            pergunta = "A consulta usa somente a agência autenticada deste atendimento. Qual período deseja consultar para essa agência?";
        if (destino != null) filtros.setDestino(destino);
        if (origem != null) filtros.setOrigem(origem);
        String companhia = companhiaConhecida(t);
        if (companhia != null) filtros.setCompanhia(companhia);
        Matcher cia = Pattern.compile(F + "\\b(?:companhia|cia)\\s+(?:[ée]\\s+|da\\s+|:\\s*)?([\\p{L}][\\p{L} -]{1,45})").matcher(mensagem);
        if (companhia == null && !agrupamento(t) && cia.find()) {
            String valor = limpar(cia.group(1));
            if (valor != null && !normalizar(valor).matches("(?:aerea|aereas|da|de|qual|que)")) filtros.setCompanhia(valor);
        }
        String passageiro = extrairNome(mensagem);
        if (passageiro != null) filtros.setNomePassageiro(passageiro);
        else if (nome) pergunta = "Qual é o nome do passageiro que deseja localizar nessas vendas?";
        if (palavra(t, "todas as companhias|qualquer companhia|sem filtro de companhia")) filtros.setCompanhia(null);
        if (palavra(t, "todas as rotas|qualquer rota|sem filtro de destino")) { filtros.setDestino(null); filtros.setOrigem(null); }
        String erroPeriodo = periodo(mensagem, t, filtros, hoje);
        if (erroPeriodo != null) pergunta = erroPeriodo;
        if (filtros.getInicio() == null || filtros.getFim() == null) pergunta = PERIODO;
        if (paginaProxima || paginaAnterior) {
            filtros.setDetalhar(true); foco = "DETALHES";
            filtros.setPage(Math.max(0, (anterior.getFiltros().getPage() == null ? 0 : anterior.getFiltros().getPage()) + (paginaProxima ? 1 : -1)));
            filtros.setSize(anterior.getFiltros().getSize() == null ? 10 : anterior.getFiltros().getSize());
        }
        return new Pedido(filtros, foco, pergunta, retomar);
    }

    public static boolean mudancaDeAssunto(String texto) {
        String t = normalizar(texto);
        return palavra(t, "hote(?:l|is)|hospedagem|hospedes?|carros?|seguros?|pacotes?|faturas?|boletos?|financeiro|credito|limite|bsp|familias?|bagagens?|regras?|multas?|remarcacao|remarcar|simular|simulacao|cancelar|cancele|reembolsar|reembolso|atendente|humano|emergencia|plantao")
                || t.matches(".*\\b(?:emitir|emita|confirme|confirmar|pagar|pague|cancelamento)\\b.*")
                || t.matches(".*\\b(?:pesquise|pesquisar|busque|buscar|procure|consultar) (?:novos? |outros? )?voos?\\b.*");
    }

    public static String normalizar(String texto) {
        return texto == null ? "" : Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
    public static VendaAereaIndicadoresFiltros copiar(VendaAereaIndicadoresFiltros f) {
        var c = new VendaAereaIndicadoresFiltros();
        if (f == null) return c;
        c.setInicio(f.getInicio()); c.setFim(f.getFim()); c.setOrigem(f.getOrigem()); c.setDestino(f.getDestino());
        c.setCompanhia(f.getCompanhia()); c.setNomePassageiro(f.getNomePassageiro()); c.setAgrupamento(f.getAgrupamento());
        c.setPage(f.getPage()); c.setSize(f.getSize()); c.setDetalhar(f.isDetalhar()); return c;
    }
    private static boolean palavra(String t, String expressao) { return Pattern.compile("\\b(?:" + expressao + ")\\b").matcher(t).find(); }
    private static boolean agrupamento(String t) { return palavra(t, "separe|separar|separado|separadas|agrupe|agrupar|agrupado|agrupadas|por companhia|por cia|por empresa|por destino|por rota|total geral|sem agrupar|sem agrupamento|sem separar|junte|juntar"); }
    private static String companhiaConhecida(String t) { return palavra(t,"latam|tam|jj") || palavra(t,"(?:companhia|cia|pela|na|da) la") ? "LATAM" : palavra(t,"gol|g3") ? "GOL" : palavra(t,"azul") || palavra(t,"(?:companhia|cia|pela|na|da) ad") ? "AZUL" : null; }
    private static boolean temNome(String t) { return palavra(t, "nome|passageir[ao] chamad[ao]|cliente chamad[ao]") || t.matches(".*\\btem (?:a |o |alguma? |alguem (?:chamado |chamada )?)?[\\p{L}][\\p{L}'’-]*(?:[.!?])?"); }
    private static String extrairNome(String mensagem) {
        Matcher m = Pattern.compile(F + "\\b(?:nome\\s+(?:(?:d[ao]\\s+)?(?:cliente|passageir[ao])\\s+)?(?:[ée]\\s+|de\\s+)?|(?:passageir[ao]|cliente)\\s+chamad[ao]\\s+)([\\p{L}][\\p{L}'’-]*(?:\\s+[\\p{L}][\\p{L}'’-]*){0,6})").matcher(mensagem);
        if (!m.find()) {
            m=Pattern.compile(F + "\\btem\\s+(?:(?:a|o|alguma?|alguem)\\s+)?(?:chamad[ao]\\s+)?([\\p{L}][\\p{L}'’-]*(?:\\s+[\\p{L}][\\p{L}'’-]*){0,6})").matcher(mensagem);
            if(!m.find())return null;
        }
        String valor = limpar(m.group(1));
        return valor == null || normalizar(valor).matches("(?:algum|alguma|alguem|passageiro|passageira|cliente|reserva|venda|vendas|nessas|nesses|qual|que)") ? null : valor;
    }
    private static String lugar(String original, String prefixo) {
        Matcher m = Pattern.compile(F + "\\b" + prefixo + "([\\p{L}][\\p{L}'’-]*(?:\\s+[\\p{L}][\\p{L}'’-]*){0,7})").matcher(original);
        if (!m.find()) return null;
        String valor = limpar(m.group(1));
        return valor == null || normalizar(valor).matches("(?:companhia|cia|o|a|as|os|destino|origem|este|esse|neste|nesse)") ? null : valor;
    }
    private static String limpar(String valor) {
        String v = valor.replaceAll(F + "\\s+(?:para|com|no|na|nos|nas|em|pelo|pela|por|da\\s+companhia|de\\s+companhia|que|entre|hoje|ontem|este|esse|neste|nesse|ano|mes|mês|nessas?|nesses?|dessas?|desses?)\\b.*$", "")
                .replaceAll("[?.!,;:].*$", "").trim();
        return v.isBlank() || v.length() > 100 ? null : v;
    }
    private static boolean temPeriodo(String t) { return palavra(t, "hoje|ontem|mes|meses|ano|anos|dias|semana|semanas|semestre|trimestre|bimestre|" + String.join("|", MESES)) || t.matches(".*(?:\\d{1,2}/\\d{1,2}|\\d{4}-\\d{2}-\\d{2}|\\b20\\d{2}\\b).*"); }
    private static String periodo(String original, String t, VendaAereaIndicadoresFiltros f, LocalDate hoje) {
        List<LocalDate> datas = new ArrayList<>();
        Matcher d = Pattern.compile("(?<!\\d)(\\d{1,2}/\\d{1,2}/\\d{4}|\\d{4}-\\d{2}-\\d{2})(?!\\d)").matcher(original);
        while (d.find()) try { datas.add(LocalDate.parse(d.group(1), d.group(1).contains("/") ? DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT) : DateTimeFormatter.ISO_LOCAL_DATE)); }
        catch (RuntimeException ex) { return "Informe datas válidas no formato dia/mês/ano."; }
        LocalDate inicio = null, fim = null;
        if (!datas.isEmpty()) {
            if (datas.size() > 2) return "Informe somente as datas inicial e final da consulta.";
            inicio = datas.get(0); fim = datas.get(datas.size()-1);
        } else {
            Matcher n = Pattern.compile("\\bultimos? (\\d{1,4}) dias?\\b").matcher(t);
            Matcher mes = Pattern.compile("\\b("+String.join("|",MESES)+") (?:de )?(20\\d{2})\\b").matcher(t);
            Matcher ano = Pattern.compile("\\b(?:ano (?:de )?|em )?(20\\d{2})\\b").matcher(t);
            if (palavra(t,"hoje")) { inicio=hoje;fim=hoje; }
            else if (palavra(t,"ontem")) { inicio=hoje.minusDays(1);fim=inicio; }
            else if (n.find()) { int dias=Integer.parseInt(n.group(1)); if(dias<1||dias>366)return "Informe um período de 1 a 366 dias."; inicio=hoje.minusDays(dias-1L);fim=hoje; }
            else if (palavra(t,"mes passado|ultimo mes")) { YearMonth m=YearMonth.from(hoje).minusMonths(1);inicio=m.atDay(1);fim=m.atEndOfMonth(); }
            else if (palavra(t,"este mes|esse mes|neste mes|nesse mes")) { inicio=hoje.withDayOfMonth(1);fim=hoje; }
            else if (mes.find()) { YearMonth m=YearMonth.of(Integer.parseInt(mes.group(2)),MESES.indexOf(mes.group(1))+1);inicio=m.atDay(1);fim=m.atEndOfMonth(); }
            else if (palavra(t,"ano passado|ultimo ano")) { inicio=LocalDate.of(hoje.getYear()-1,1,1);fim=inicio.withMonth(12).withDayOfMonth(31); }
            else if (palavra(t,"este ano|esse ano|neste ano|nesse ano")) { inicio=hoje.withDayOfYear(1);fim=hoje; }
            else if (ano.find() && !palavra(t,String.join("|",MESES))) { inicio=LocalDate.of(Integer.parseInt(ano.group(1)),1,1);fim=inicio.withMonth(12).withDayOfMonth(31); }
            else if (temPeriodo(t)) return PERIODO;
        }
        if (inicio != null) {
            if(fim.isBefore(inicio))return "A data final precisa ser igual ou posterior à data inicial.";
            if(ChronoUnit.DAYS.between(inicio,fim)+1>366)return "Informe um período de até 366 dias; consultas maiores devem ser divididas por ano.";
            f.setInicio(inicio);f.setFim(fim);
        }
        return null;
    }
}
