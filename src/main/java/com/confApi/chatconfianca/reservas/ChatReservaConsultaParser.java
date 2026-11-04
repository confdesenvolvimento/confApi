package com.confApi.chatconfianca.reservas;

import com.confApi.chatconfianca.dto.reserva.ReservaAereaRecenteItem;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Interpretacao deterministica de consultas, nunca de autorizacao ou execucao de escrita. */
public final class ChatReservaConsultaParser {
    public record Pedido(ReservaConsultaFiltros filtros, String foco, String pergunta, boolean ampliarSeVazio) { }
    private static final String FLAGS = "(?iu)";
    private static final Pattern PNR = Pattern.compile(FLAGS + "\\b(?:localizador|pnr)\\s*(?:n[ºo.]?\\s*)?[:#]?\\s*([a-z0-9]{5,20})\\b");
    private static final Pattern NOME = Pattern.compile(FLAGS + "\\b(?:nome\\s+(?:d[ao]\\s+)?(?:cliente|passageir[ao])?\\s*(?:[ée]\\s+|de\\s+)?|(?:passageir[ao]|cliente)\\s+(?:chamad[ao]\\s+|de\\s+nome\\s+|[ée]\\s+)?)((?:[\\p{L}][\\p{L}'’-]*)(?:\\s+[\\p{L}][\\p{L}'’-]*){0,7})");
    private ChatReservaConsultaParser() { }

    public static Pedido interpretar(String mensagem, ReservaConsultaEstado anterior) {
        if (mensagem == null || mensagem.isBlank() || mensagem.length() > 2000) return null;
        String t = normalizar(mensagem);
        if (mudancaDeAssunto(t)) return null;
        if (t.matches(".*\\bnao (?:quero |preciso |desejo |deve )?(?:listar|liste|buscar|busque|consultar|consulte|carregar|carregue|mostrar|mostre)\\b.*")) return null;
        boolean temContexto = anterior != null && anterior.getFiltros() != null;
        boolean vendas = palavra(t, "vendas?|vendi|vendidos?|vendidas?");
        boolean reservas = palavra(t, "reservas?|localizador(?:es)?|bilhetes?|pnr");
        boolean reservasContaveis = palavra(t, "reservas?|localizador(?:es)?|pnr");
        boolean passageiros = palavra(t, "passageiros?|passageiras?|paxs?");
        boolean bilhetes = palavra(t, "bilhetes?");
        boolean ultima = palavra(t, "ultim[ao]s?|recentes?");
        boolean contar = palavra(t, "quantos|quantas|quantidade|total") && (vendas || reservasContaveis
                || (temContexto && t.matches("(?:e )?(?:sao |foram )?(?:quantos|quantas|qual (?:o )?total)[.!?]?")))
                && ((!passageiros && !bilhetes) || t.matches(".*\\b(?:quantos|quantas|total(?: de)?|quantidade(?: de)?) (?:localizadores|reservas|vendas)\\b.*"));
        boolean detalhe = palavra(t, "carreg(?:ue|ar|a)|abr(?:a|ir)|mostr(?:e|ar|a)|detalhes?|dados|consult(?:e|ar|a)");
        boolean listagem = palavra(t, "liste|listar|lista|listagem") || t.matches(".*\\b(?:mostre|mostrar|traga|traz|veja) (?:todas|todos|as reservas|os localizadores|as vendas|os resultados)\\b.*");
        boolean companhia = palavra(t, "companhia|cia|aerea");
        boolean trecho = palavra(t, "trechos?|rotas?|embarque|voos?|origem|destino");
        boolean pronome = palavra(t, "del[ae]|nel[ae]|ness[ae]|dess[ae]|ess[ae]|est[ae]|daquel[ae]") || t.matches("(?:e )?(?:o |a )?(?:trecho|companhia|cia|embarque|bilhete|localizador)\\??");
        boolean referenciaGenerica = t.matches("(?:(?:e|qual|quais|quantos|quantas|qual e|quais sao) )?(?:(?:o|a|os|as) )?(?:nome (?:dos|das) )?(?:dados|detalhes|companhia|cia|trechos?|rotas?|embarque|bilhetes?|passageiros?)(?: (?:sao|tem))?[.!?]?")
                || t.matches("(?:(?:carregue|carregar|abra|mostre|consulte) )?(?:(?:o|a|os|as) )?(?:dados|detalhes|reserva|localizador)[.!?]?");
        boolean outraRota = t.matches(".*\\b(outra rota|outro destino|todas as rotas|qualquer rota)\\b.*");
        boolean paginaSeguinte = t.matches("(?:mostre |veja |abra )?(?:a )?(?:proxima pagina|pagina seguinte|mais resultados)[.!?]?");
        boolean paginaAnterior = t.matches("(?:volte |mostre |veja )?(?:a )?(?:pagina anterior|anterior pagina)[.!?]?");
        String nome = extrairNome(mensagem, t, anterior, detalhe, companhia);
        Matcher localizador = PNR.matcher(mensagem);
        String loc = localizador.find() && !comando(localizador.group(1)) ? localizador.group(1).toUpperCase(Locale.ROOT) : null;
        if (ultima && loc != null && normalizar(loc).matches("(?:vendid[ao]|emitid[ao]|criad[ao]|gerad[ao]|registrad[ao])")) loc = null;
        if (loc == null && nome == null) {
            Matcher codigoDigitado = Pattern.compile("(?i)\\b(?=[a-z0-9]{5,20}\\b)(?=[a-z0-9]*[a-z])(?=[a-z0-9]*[0-9])[a-z0-9]+\\b").matcher(mensagem);
            if (codigoDigitado.find()) loc = codigoDigitado.group().toUpperCase(Locale.ROOT);
        }
        String destino = capturarLugar(mensagem, "(?:para\\s+(?:o\\s+destino\\s+)?|destino\\s*(?:de\\s+|[ée]\\s+|:\\s*)?)");
        String origem = capturarLugar(mensagem, "(?:saindo\\s+de\\s+|origem\\s*(?:de\\s+|[ée]\\s+|:\\s*)?)");
        String cia = extrairCompanhia(mensagem);
        Integer selecionada = selecionar(mensagem, t, anterior);
        boolean selecaoCurta = t.matches("(?:a|o|essa|esse|aquela|aquele) (?:da|do|de|para) .*");
        if (selecionada != null && selecaoCurta) { nome = null; destino = null; origem = null; cia = null; }
        if (temContexto && selecionada == null && selecaoCurta && cia == null && nome == null) {
            destino = capturarLugar(mensagem, "(?:(?:a|o|essa|esse|aquela|aquele)\\s+(?:da|do|de|para)\\s+)");
        }
        boolean referenciaOrdinal = temContexto && (palavra(t, "primeir[ao]|segund[ao]|terceir[ao]|quart[ao]|quint[ao]|sext[ao]|setim[ao]|oitav[ao]|non[ao]|decim[ao]|opcao|item|numero") || t.matches("\\d{1,2}[.!?]?"));
        if (selecionada == null && temContexto && !listagem && (pronome || referenciaGenerica) && nome == null && loc == null && destino == null && cia == null) {
            selecionada = anterior.getSelecionadaId();
            if (selecionada == null && anterior.getItens() != null && anterior.getItens().size() == 1) selecionada = anterior.getItens().get(0).getReservaId();
        }
        boolean periodo = temPeriodo(t);
        boolean escopo = (ultima && (vendas || reservas)) || contar || nome != null || loc != null || selecionada != null
                || (reservas && detalhe && (destino != null || origem != null))
                || (temContexto && (pronome || detalhe || listagem || companhia || trecho || passageiros || bilhetes || outraRota || periodo || destino != null || origem != null || cia != null || paginaSeguinte || paginaAnterior || referenciaOrdinal))
                || ((vendas || reservas) && palavra(t, "list(?:e|ar|a)|busc(?:ar|a|ue)|pesquis(?:ar|a|e)|consult(?:ar|a|e)|mostr(?:ar|a|e)|minhas?|meus?|emitid[ao]s?"));
        if (!escopo) return null;

        boolean novoConjunto = (ultima || (palavra(t, "minhas?|meus?") && !pronome)) && (vendas || reservas);
        ReservaConsultaFiltros f = temContexto && !novoConjunto && loc == null ? anterior.getFiltros().copy() : new ReservaConsultaFiltros();
        f.setReservaId(null);
        f.setPage(0);
        f.setSize(novoConjunto && palavra(t, "ultim[ao]") ? 1 : 10);
        if (temContexto && (paginaSeguinte || paginaAnterior)) {
            f.setPage(Math.max(0, Objects.requireNonNullElse(anterior.getFiltros().getPage(), 0) + (paginaSeguinte ? 1 : -1)));
            f.setSize(Objects.requireNonNullElse(anterior.getFiltros().getSize(), 10));
        }
        String foco = contar ? "CONTAGEM" : companhia ? "CIA" : passageiros && nome == null ? "PASSAGEIROS" : bilhetes && !novoConjunto ? "BILHETES" : trecho && (pronome || selecionada != null || nome != null) ? "TRECHOS" : novoConjunto && f.getSize() == 1 ? "DETALHES" : "LISTA";
        String pergunta = null;
        if (vendas || palavra(t, "emitid[ao]s?")) {
            f.setSituacao("EMITIDAS"); f.setTipoData("EMISSAO");
        } else if (novoConjunto && reservas) {
            f.setSituacao("TODAS"); f.setTipoData("CRIACAO");
        }
        Matcher quantidade = Pattern.compile("\\bultim[ao]s?\\s+(\\d{1,3})\\s+(?:vendas?|reservas?|localizadores?|bilhetes?)\\b").matcher(t);
        if (quantidade.find()) f.setSize(Math.min(50, Math.max(1, Integer.parseInt(quantidade.group(1)))));
        if (destino != null) f.setDestino(destino);
        if (origem != null) f.setOrigem(origem);
        if (cia != null) f.setCompanhia(cia);
        if (outraRota) { f.setDestino(null); f.setOrigem(null); }
        if (nome != null) { f.setNomePassageiro(nome); f.setLocalizador(null); }
        if (loc != null) { f.setLocalizador(loc); f.setNomePassageiro(null); }
        if (selecionada != null && nome == null && loc == null && destino == null && origem == null && cia == null && !periodo && !outraRota && !contar) {
            f.setReservaId(selecionada);
            foco = companhia ? "CIA" : passageiros && nome == null ? "PASSAGEIROS" : bilhetes ? "BILHETES" : trecho ? "TRECHOS" : "DETALHES";
        } else if (nome != null || loc != null) {
            if (!contar && !List.of("CIA", "TRECHOS", "PASSAGEIROS", "BILHETES").contains(foco)) foco = "DETALHES";
        } else if ((pronome || detalhe || companhia || trecho || passageiros || bilhetes) && temContexto && !listagem && destino == null && origem == null && cia == null && !periodo && !outraRota && !contar && !novoConjunto) {
            // Sem correspondencia unica, o orquestrador apresenta alternativas em vez de escolher aleatoriamente.
            foco = "LISTA";
            pergunta = "Qual das reservas encontradas voce deseja consultar? Informe o localizador ou o numero da opcao.";
        }
        if (referenciaOrdinal && selecionada == null) pergunta = "Nao encontrei essa opcao na ultima pagina. Informe uma das opcoes exibidas ou o localizador.";
        String periodoInvalido = aplicarPeriodo(mensagem, t, f);
        boolean ampliar = nome != null && (f.getDestino() != null || f.getOrigem() != null);
        return new Pedido(f, foco, periodoInvalido == null ? pergunta : periodoInvalido, ampliar);
    }

    static boolean mudancaDeAssunto(String texto) {
        String t = normalizar(texto);
        return palavra(t, "faturas?|boletos?|faturamento|financeiro|limite|credito|pagar|pagamentos?|bagagem|bagagens|hote(?:l|is)|carros?|seguros?|pacotes?|familias?|alertas?|regras?|multas?|penalidades?|reembols(?:o|ar)|remarca(?:cao|r)|simular|simule|simulacao|simulacoes|cancelar|cancele|emitir|emita|atendente|humano")
                || (palavra(t, "vendi|vendas?|vendidos?") && palavra(t, "reais|r\\$|valor|faturado|dinheiro"))
                || t.matches(".*\\b(?:pesquis(?:ar|e|a)|busc(?:ar|a|ue)|procure|consult(?:ar|e|a)) (?:novos? |outros? )?voos?\\b.*")
                || t.matches(".*\\b(?:disponibilidade|menor(?:es)? (?:preco|valor)|passagem para comprar|comprar passagem)\\b.*");
    }

    public static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private static String extrairNome(String original, String t, ReservaConsultaEstado estado, boolean detalhe, boolean companhia) {
        if (t.matches(".*\\bnome (?:dos|das|de) passageiros?\\b.*")) return null;
        Matcher m = NOME.matcher(original);
        if (m.find()) {
            String nome = limparNome(m.group(1));
            if (nome != null) return nome;
        }
        Matcher da = Pattern.compile(FLAGS + "\\b(?:dados|detalhes|reservas?|vendas?|bilhetes?|companhia|cia)\\s+(?:d[ao]|de)\\s+([\\p{L}][\\p{L}'’-]*(?:\\s+[\\p{L}][\\p{L}'’-]*){0,6})").matcher(original);
        if (da.find()) {
            String nome = limparNome(da.group(1));
            if (nome != null && !nome.matches("(?iu)(?:Fortaleza|Recife|Manaus|Cuiab[aá]|S[aã]o Paulo|Rio de Janeiro|Bras[ií]lia|Latam|Gol|Azul|TAP|Avianca)")) return nome;
        }
        // Palavras curtas so viram nomes se houver passageiro correspondente no ultimo resultado autorizado.
        if (estado != null && estado.getItens() != null) {
            for (var item : estado.getItens()) if (item != null && item.getPassageiros() != null) for (String nome : item.getPassageiros()) {
                String n = normalizar(nome);
                if (n.isBlank()) continue;
                for (String parte : n.split(" ")) {
                    if (parte.length() >= 3 && !comando(parte) && palavra(t, Pattern.quote(parte))) {
                        Matcher noOriginal = Pattern.compile(FLAGS + "[\\p{L}][\\p{L}'’-]*").matcher(original);
                        while (noOriginal.find()) if (normalizar(noOriginal.group()).equals(parte)) return noOriginal.group();
                    }
                }
            }
        }
        // "Tem alguma com o nome Nazare?" nao exige o usuario conhecer o localizador.
        Matcher comNome = Pattern.compile(FLAGS + "\\b(?:com\\s+(?:o\\s+)?nome\\s+(?:de\\s+)?|chamad[ao]\\s+)([\\p{L}][\\p{L}'’-]*(?:\\s+[\\p{L}][\\p{L}'’-]*){0,6})").matcher(original);
        if (comNome.find()) return limparNome(comNome.group(1));
        Matcher comprou = Pattern.compile(FLAGS + "(?=\\b(?:a|o)\\s+([\\p{L}][\\p{L}'’-]*(?:\\s+[\\p{L}][\\p{L}'’-]*){0,5})\\s+(?:comprou|viajou|embarcou)\\b)").matcher(original);
        while (comprou.find()) { String pessoa = limparNome(comprou.group(1)); if (pessoa != null) return pessoa; }
        return null;
    }

    private static String limparNome(String valor) {
        String nome = valor.replaceAll("(?iu)\\s+(?:para|que|com|no|na|em|pelo|pela|por|qual|comprou|viajou|tem|foi|esta|está|vendeu|vendido|vendida|entre|nessas?|nesses?|aqui)\\b.*$", "").replaceAll("[?.!,;:].*$", "").trim();
        if (nome.length() < 2 || nome.length() > 100 || nome.split("\\s+").length > 8) return null;
        String n = normalizar(nome);
        if (comando(n) || n.matches("(?:minha|meu|minhas|meus|sua|suo|suas|dele|dela|essa|esse|esta|este|aquela|aquele|todas|todos|dos|das|de|do|da|e|os|as)(?: .*)?")) return null;
        return nome;
    }

    private static String capturarLugar(String original, String prefixo) {
        Matcher m = Pattern.compile(FLAGS + "\\b" + prefixo + "([\\p{L}][\\p{L}'’.-]*(?:\\s+[\\p{L}][\\p{L}'’.-]*){0,5})").matcher(original);
        if (!m.find()) return null;
        String lugar = m.group(1).replaceAll("(?iu)\\s+(?:com|no|na|em|pela|pelo|por\\s+(?:favor|gentileza)|da\\s+companhia|de\\s+companhia|que|tem|e\\s+(?:tem|qual|a\\s+cia|o\\s+nome)|nos|nas|entre|nessas?|nesses?|aqui|hoje|ontem|este|esse|ano|mes|mês)\\b.*$", "")
                .replaceAll("(?iu)\\s+de\\s+\\d.*$", "").replaceAll("[?.!,;:].*$", "").replaceAll("(?iu)\\s+(?:de|at[eé]|a)$", "").trim();
        return lugar.length() > 1 && lugar.length() <= 100 && !comando(lugar) ? lugar : null;
    }

    private static String extrairCompanhia(String original) {
        String t = normalizar(original);
        if (t.matches(".*\\bqual (?:a )?(?:companhia|cia)(?: aerea)?\\b.*")) return null;
        Matcher m = Pattern.compile(FLAGS + "\\b(?:companhia|cia)\\s*(?:a[ée]rea\\s*)?(?:[ée]\\s+|da\\s+|:\\s*)?([\\p{L}][\\p{L} -]{1,40})").matcher(original);
        if (m.find()) {
            String valor = limparNome(m.group(1));
            if (valor != null && !normalizar(valor).matches("(?:a|ela|qual|que|onde|comprou|dela|da|do|de)(?: .*)?")) return valor;
        }
        Matcher conhecida = Pattern.compile(FLAGS + "\\b(?:da|pela|com|na)\\s+(LATAM|GOL|AZUL|TAP|AVIANCA|AIR FRANCE|BRITISH AIRWAYS|AMERICAN AIRLINES)\\b").matcher(original);
        return conhecida.find() ? conhecida.group(1) : null;
    }

    private static Integer selecionar(String original, String t, ReservaConsultaEstado estado) {
        if (estado == null || estado.getItens() == null || estado.getItens().isEmpty()) return null;
        List<ReservaAereaRecenteItem> itens = estado.getItens();
        Integer ordem = null;
        String[] ordinais = {"primeir[ao]", "segund[ao]", "terceir[ao]", "quart[ao]", "quint[ao]", "sext[ao]", "setim[ao]", "oitav[ao]", "non[ao]", "decim[ao]"};
        for (int i = 0; i < ordinais.length; i++) if (palavra(t, ordinais[i])) ordem = i;
        Matcher n = Pattern.compile("^(?:(?:a|o|reserva|numero|opcao|item)\\s*)?(\\d{1,2})(?:[ªºa])?[.!?]?$|\\b(?:opcao|numero|item)\\s+(\\d{1,2})\\b").matcher(t);
        if (n.find()) ordem = Integer.parseInt(n.group(1) == null ? n.group(2) : n.group(1)) - 1;
        if (ordem != null) return ordem >= 0 && ordem < itens.size() ? itens.get(ordem).getReservaId() : null;
        String pnr = t.replaceAll("[?.!]$", "");
        List<Integer> encontrados = new ArrayList<>();
        for (var item : itens) {
            if (item == null || item.getReservaId() == null) continue;
            String codigo = normalizar(item.getLocalizador());
            if (!codigo.isBlank() && palavra(t, Pattern.quote(codigo))) { encontrados.add(item.getReservaId()); continue; }
            boolean combina = false;
            String companhia = normalizar(Objects.toString(item.getCompanhiaNome(), "") + " " + Objects.toString(item.getCompanhiaIata(), ""));
            if (companhia.length() > 2 && t.matches(".*\\b(?:a|o|essa|esse|aquela|aquele) (?:da|do|de) .*") && companhia.split(" ").length > 0) {
                for (String parte : companhia.split(" ")) if (parte.length() >= 2 && palavra(t, Pattern.quote(parte))) combina = true;
            }
            if (item.getVoos() != null) for (var voo : item.getVoos()) if (voo != null) {
                String destino = normalizar(voo.getDestino());
                if (!destino.isBlank() && t.matches(".*\\b(?:a|o|essa|esse|aquela|aquele) (?:da|do|de|para) .*") && palavra(t, Pattern.quote(destino))) combina = true;
            }
            if (combina) encontrados.add(item.getReservaId());
        }
        return encontrados.stream().distinct().count() == 1 ? encontrados.get(0) : null;
    }

    private static boolean comando(String valor) {
        return normalizar(valor).matches("(?:nome|cliente|passageiro|passageira|reserva|reservas|localizador|localizadores|venda|vendas|dados|detalhes|ultima|ultimo|ultimas|ultimos|criado|criada|recente|recentes|qual|companhia|cia|trecho|destino|origem|sim|nao|dela|dele|voo|aerea|aereo|para|buscar|carregar|minha|meu|isso|esta|esse|essa|dos|das|de|do|da|e|os|as)");
    }
    private static boolean palavra(String texto, String regex) { return Pattern.compile("\\b(?:" + regex + ")\\b").matcher(texto).find(); }
    private static boolean temPeriodo(String t) { return palavra(t, "hoje|ontem|mes(?:es)?|anos?|dias?|semanas?|semestres?|trimestres?|bimestres?|janeiro|fevereiro|marco|abril|maio|junho|julho|agosto|setembro|outubro|novembro|dezembro") || t.matches(".*(?:\\d{1,2}/\\d{1,2}|\\d{4}-\\d{2}-\\d{2}|\\b20\\d{2}\\b).*"); }

    private static String aplicarPeriodo(String original, String t, ReservaConsultaFiltros f) {
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Cuiaba"));
        Matcher datas = Pattern.compile("\\b(\\d{1,2}/\\d{1,2}/\\d{4}|\\d{4}-\\d{2}-\\d{2})\\b").matcher(original);
        List<LocalDate> encontradas = new ArrayList<>();
        while (datas.find()) try { encontradas.add(LocalDate.parse(datas.group(1), datas.group(1).contains("/") ? DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT) : DateTimeFormatter.ISO_LOCAL_DATE)); } catch (Exception ignored) { return "Qual o periodo correto da consulta? Informe datas validas no formato dia/mes/ano."; }
        if (!encontradas.isEmpty()) {
            LocalDate inicio = encontradas.get(0), fim = encontradas.size() > 1 ? encontradas.get(1) : encontradas.get(0);
            if (encontradas.size() > 2 || fim.isBefore(inicio)) return "Qual o periodo correto da consulta? A data final precisa ser igual ou posterior a inicial.";
            f.setInicio(inicio); f.setFim(fim); return null;
        }
        if (palavra(t, "hoje")) { f.setInicio(hoje); f.setFim(hoje); return null; }
        if (palavra(t, "ontem")) { f.setInicio(hoje.minusDays(1)); f.setFim(hoje.minusDays(1)); return null; }
        Matcher ultimos = Pattern.compile("\\bultim[ao]s? (\\d{1,3}) dias?\\b").matcher(t);
        if (ultimos.find()) { int dias = Integer.parseInt(ultimos.group(1)); if (dias < 1 || dias > 366) return "Informe um periodo entre 1 e 366 dias para esta consulta."; f.setInicio(hoje.minusDays(dias - 1L)); f.setFim(hoje); return null; }
        if (palavra(t, "mes passado")) { LocalDate mes = hoje.minusMonths(1).withDayOfMonth(1); f.setInicio(mes); f.setFim(mes.plusMonths(1).minusDays(1)); return null; }
        if (palavra(t, "este mes|esse mes|neste mes|nesse mes")) { f.setInicio(hoje.withDayOfMonth(1)); f.setFim(hoje); return null; }
        Matcher ano = Pattern.compile("\\b(20\\d{2})\\b").matcher(t);
        Integer anoExplicito = ano.find() ? Integer.parseInt(ano.group(1)) : null;
        String[] meses = {"janeiro", "fevereiro", "marco", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"};
        int mesesMencionados = 0;
        for (String mes : meses) if (palavra(t, mes)) mesesMencionados++;
        if (mesesMencionados > 1) return "Para consultar entre meses diferentes, informe as datas inicial e final no formato dia/mes/ano.";
        for (int i = 0; i < meses.length; i++) if (palavra(t, meses[i])) { LocalDate mes = LocalDate.of(anoExplicito == null ? hoje.getYear() : anoExplicito, i + 1, 1); f.setInicio(mes); f.setFim(mes.plusMonths(1).minusDays(1)); return null; }
        if (anoExplicito != null || palavra(t, "ano passado|este ano|esse ano|neste ano|nesse ano")) {
            int a = anoExplicito != null ? anoExplicito : palavra(t, "ano passado") ? hoje.getYear() - 1 : hoje.getYear();
            f.setInicio(LocalDate.of(a, 1, 1)); f.setFim(a == hoje.getYear() ? hoje : LocalDate.of(a, 12, 31));
            return null;
        }
        return temPeriodo(t) ? "Qual o periodo da consulta? Informe a data inicial e final, ou diga este mes, mes passado ou este ano." : null;
    }
}
