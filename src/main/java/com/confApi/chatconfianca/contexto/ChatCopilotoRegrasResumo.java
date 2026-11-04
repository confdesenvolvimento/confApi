package com.confApi.chatconfianca.contexto;

import com.confApi.aereo.dto.regrasAereas.RegrasAereasReservaResponse;
import com.confApi.db.confManager.regraAereaAlteracao.dto.RegraAereaAlteracaoConsultaResponse;
import com.confApi.db.confManager.regraAereaReembolso.dto.RegraAereaReembolsoConsultaResponse;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Apresenta fatos estruturados da consulta, sem calcular multas ou usar texto livre da origem. */
final class ChatCopilotoRegrasResumo {
    private static final Set<String> MOMENTOS = Set.of("ANTES_EMBARQUE", "APOS_EMBARQUE", "NO_SHOW");
    private static final Set<String> REEMBOLSO = Set.of("VALIDACAO_MANUAL", "REEMBOLSO_COM_MULTA",
            "REEMBOLSO_PERMITIDO", "SOMENTE_TAXAS_REEMBOLSAVEIS", "NAO_REEMBOLSAVEL");
    private static final Set<String> ALTERACAO = Set.of("NAO_PERMITIDA", "PERMITIDA_COM_MULTA", "PERMITIDA_SEM_MULTA");

    private ChatCopilotoRegrasResumo() { }

    static Resultado montar(RegrasAereasReservaResponse dados, String pergunta, String localizador) {
        Foco foco = Foco.da(pergunta);
        Set<Item> itens = new LinkedHashSet<>();
        var reembolsos = dados.getReembolsos() == null || dados.getReembolsos().isEmpty()
                ? dados.getReembolso() == null ? List.<RegraAereaReembolsoConsultaResponse>of()
                    : List.of(dados.getReembolso()) : dados.getReembolsos();
        if (foco.reembolso()) for (var resposta : reembolsos) {
            if (resposta == null || resposta.getRegra() == null || !REEMBOLSO.contains(chave(resposta.getStatus()))) continue;
            var r = resposta.getRegra();
            String momento = momento(r.getMomento(), resposta.getMatch());
            if (!foco.inclui(momento)) continue;
            String status = chave(resposta.getStatus());
            boolean manual = "VALIDACAO_MANUAL".equals(status) || Boolean.TRUE.equals(r.getRequerValidacaoManual());
            String permissao = manual ? "Exige validação manual; reembolso não confirmado."
                    : switch (status) {
                        case "REEMBOLSO_COM_MULTA", "REEMBOLSO_PERMITIDO" -> "Permitido nas condições da tarifa.";
                        case "SOMENTE_TAXAS_REEMBOLSAVEIS" -> "Somente as taxas de embarque; sem reembolso da tarifa.";
                        default -> "Tarifa não reembolsável.";
                    };
            itens.add(new Item("Reembolso", momento, permissao,
                    multa(consistente(status, r.getAplicaMulta()), r.getTipoMulta(), r.getMoedaMulta(),
                            r.getValorMultaFixo(), r.getPercentualMulta(), r.getPorPassageiro(), r.getPorTrecho(), false),
                    Boolean.TRUE.equals(r.getRequerSimulacao()) ? List.of("Exige simulação.") : List.of()));
        }

        var alteracoes = dados.getAlteracoes() == null || dados.getAlteracoes().isEmpty()
                ? dados.getAlteracao() == null ? List.<RegraAereaAlteracaoConsultaResponse>of()
                    : List.of(dados.getAlteracao()) : dados.getAlteracoes();
        if (foco.alteracao()) for (var resposta : alteracoes) {
            if (resposta == null || resposta.getRegra() == null || !ALTERACAO.contains(chave(resposta.getStatus()))) continue;
            var r = resposta.getRegra();
            String momento = momento(r.getMomento(), resposta.getMatch());
            if (!foco.inclui(momento)) continue;
            List<String> condicoes = new ArrayList<>();
            if (Boolean.TRUE.equals(r.getCobraDiferencaTarifaria())) condicoes.add("Pode haver diferença tarifária.");
            if (Boolean.TRUE.equals(r.getExigeRemarcacaoConjunta())) condicoes.add("Exige remarcação conjunta dos trechos aplicáveis.");
            itens.add(new Item("Remarcação", momento,
                    "NAO_PERMITIDA".equals(chave(resposta.getStatus())) ? "Não permitida pela regra." : "Permitida nas condições da tarifa.",
                    multa(consistente(resposta.getStatus(), r.getAplicaMulta()), r.getTipoMulta(), r.getMoedaMulta(),
                            r.getValorMultaFixo(), r.getPercentualMulta(), r.getPorPassageiro(), r.getPorTrecho(),
                            r.getValorMultaFixoEuropa() != null || r.getValorMultaFixoDemaisInternacionais() != null),
                    List.copyOf(condicoes)));
        }

        String titulo = foco.momentos().equals(Set.of("NO_SHOW")) ? "No-show" : "Regras da tarifa";
        StringBuilder texto = new StringBuilder("**").append(titulo).append(" — reserva ").append(localizador).append("**\n\n");
        if (itens.isEmpty()) {
            texto.append("Não localizei uma regra aplicável").append(foco.descricao()).append(" nos dados consultados.\n\n")
                    .append("Isso não significa isenção de multa ou permissão para alterar/reembolsar. Confirme com o atendimento.");
            return new Resultado(texto.toString(), false);
        }
        for (Item item : itens) {
            texto.append("**").append(item.operacao()).append(rotuloMomento(item.momento())).append("**\n\n")
                    .append("- **Multa:** ").append(item.multa()).append('\n')
                    .append("- **Condição:** ").append(item.permissao()).append('\n');
            if (!item.condicoes().isEmpty()) texto.append("- **Atenção:** ").append(String.join(" ", item.condicoes())).append('\n');
            texto.append('\n');
        }
        // Uma regra de reembolso sem multa nunca confirma isenção para remarcação (ou vice-versa).
        for (String operacao : foco.reembolso() && foco.alteracao() ? List.of("Reembolso", "Remarcação") : List.<String>of()) {
            if (itens.stream().noneMatch(i -> operacao.equals(i.operacao())))
                texto.append(operacao).append(": regra não localizada").append(foco.descricao()).append(".\n\n");
        }
        texto.append("Confirme encargos e valor final na simulação/atendimento. Nenhuma operação foi executada.");
        return new Resultado(texto.toString(), true);
    }

    private static String multa(Boolean aplica, String tipo, String moeda, BigDecimal fixa, BigDecimal percentual,
            Boolean passageiro, Boolean trecho, boolean regional) {
        if (aplica == null) return "Não informada com segurança nesta regra.";
        if (!aplica) return "Não aplicada nesta regra.";
        String por = Boolean.TRUE.equals(passageiro) && Boolean.TRUE.equals(trecho) ? " por passageiro e por trecho"
                : Boolean.TRUE.equals(passageiro) ? " por passageiro" : Boolean.TRUE.equals(trecho) ? " por trecho" : "";
        boolean fixaValida = fixa != null && fixa.signum() >= 0 && moeda != null && moeda.matches("[A-Z]{3}");
        boolean percentualValido = percentual != null && percentual.signum() >= 0 && percentual.compareTo(BigDecimal.valueOf(100)) <= 0;
        String t = chave(tipo);
        if ("PERCENTUAL".equals(t) && percentualValido)
            return numero(percentual) + "% sobre a base prevista na regra" + por + ".";
        if (!regional && Set.of("FIXO", "FIXA", "VALOR_FIXO").contains(t) && fixaValida)
            return moeda + " " + dinheiro(fixa) + por + ".";
        if (!regional && Set.of("MENOR_ENTRE_FIXO_E_PERCENTUAL", "MAIOR_ENTRE_FIXO_E_PERCENTUAL").contains(t) && fixaValida && percentualValido)
            return (t.startsWith("MENOR") ? "Menor" : "Maior") + " entre " + moeda + " " + dinheiro(fixa)
                    + " e " + numero(percentual) + "% sobre a base prevista na regra" + por + ".";
        return "Prevista; valor a confirmar na simulação/atendimento.";
    }

    private static Boolean consistente(String status, Boolean aplica) {
        String s = chave(status);
        if (Set.of("REEMBOLSO_COM_MULTA", "PERMITIDA_COM_MULTA").contains(s) && !Boolean.TRUE.equals(aplica)) return null;
        if (Set.of("REEMBOLSO_PERMITIDO", "PERMITIDA_SEM_MULTA").contains(s) && Boolean.TRUE.equals(aplica)) return null;
        return aplica;
    }

    private static String momento(String regra, Map<String, Object> match) {
        String r = chave(regra);
        Object valor = match == null ? null : match.get("momento");
        String solicitado = valor instanceof String ? chave((String) valor) : "";
        if (MOMENTOS.contains(r)) return MOMENTOS.contains(solicitado) && !r.equals(solicitado) ? "" : r;
        // Regra genérica só é associada ao no-show se a origem confirmar o momento da consulta.
        if (Set.of("", "QUALQUER_MOMENTO", "TODOS", "AMBOS", "ANY").contains(r) && MOMENTOS.contains(solicitado)) return solicitado;
        return "";
    }

    private static String rotuloMomento(String momento) {
        return switch (momento) {
            case "ANTES_EMBARQUE" -> " antes do embarque";
            case "APOS_EMBARQUE" -> " após o embarque";
            case "NO_SHOW" -> " no no-show";
            default -> " (momento não informado)";
        };
    }

    private static String dinheiro(BigDecimal valor) {
        NumberFormat f = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"));
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(Math.max(2, valor.scale()));
        return f.format(valor);
    }
    private static String numero(BigDecimal valor) { return valor.stripTrailingZeros().toPlainString().replace('.', ','); }
    private static String chave(String valor) { return valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT); }
    private static String normalizar(String valor) {
        return valor == null ? "" : Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    record Resultado(String texto, boolean temRegras) { }
    private record Item(String operacao, String momento, String permissao, String multa, List<String> condicoes) { }
    private record Foco(Set<String> momentos, boolean reembolso, boolean alteracao) {
        static Foco da(String pergunta) {
            String p = normalizar(pergunta);
            Set<String> momentos = new LinkedHashSet<>();
            if (p.matches(".*\\bno[ -]?show\\b.*")) momentos.add("NO_SHOW");
            if (p.matches(".*\\bantes\\b.{0,30}\\b(?:embarque|voo)\\b.*")) momentos.add("ANTES_EMBARQUE");
            if (p.matches(".*\\b(?:apos|depois)\\b.{0,30}\\b(?:embarque|voo)\\b.*")) momentos.add("APOS_EMBARQUE");
            boolean reembolso = p.matches(".*\\breembols[a-z]*\\b.*");
            boolean alteracao = p.matches(".*\\b(?:remarc[a-z]*|alter[a-z]*|reemiss[a-z]*)\\b.*");
            return new Foco(Set.copyOf(momentos), reembolso || !alteracao, alteracao || !reembolso);
        }
        boolean inclui(String momento) { return momentos.isEmpty() || momentos.contains(momento); }
        String descricao() {
            if (momentos.isEmpty()) return "";
            return " para " + String.join(" e ", momentos.stream().sorted().map(m -> switch (m) {
                case "NO_SHOW" -> "no-show";
                case "ANTES_EMBARQUE" -> "antes do embarque";
                default -> "após o embarque";
            }).toList());
        }
    }
}
