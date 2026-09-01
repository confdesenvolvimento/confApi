package com.confApi.chatconfianca.intencao;

import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.chatgpt.service.ChatService;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class ChatConfiancaDecisaoIaService {
    public static final String TOOL_PESQUISAR_VOOS = "PESQUISAR_VOOS";
    public static final String TOOL_MELHORES_TARIFAS_IDA = "MELHORES_TARIFAS_IDA";
    public static final String TOOL_MELHORES_TARIFAS_IDA_VOLTA = "MELHORES_TARIFAS_IDA_VOLTA";
    public static final String TOOL_PESQUISAR_HOTEIS = "PESQUISAR_HOTEIS";

    private final ChatIntencaoShadowService intencaoService;
    private final ChatService chatService;
    private final ChatIntencaoShadowProperties properties;

    public ChatConfiancaDecisaoIaService(ChatIntencaoShadowService intencaoService,
                                         ChatService chatService,
                                         ChatIntencaoShadowProperties properties) {
        this.intencaoService = intencaoService;
        this.chatService = chatService;
        this.properties = properties;
    }

    public ChatConfiancaDecisaoIa decidir(String mensagem,
                                          Long departamentoExplicitoId,
                                          List<DepartamentoUnidade> departamentos,
                                          Integer codgUnidade,
                                          String baseAtual) {
        ChatIntencaoClassificacao classificacao =
                intencaoService.classificar(mensagem, codgUnidade, baseAtual);
        String keywordDeterministica =
                chatService.identificarKeywordOperacionalDeterministica(mensagem);
        String intencaoDeterministica = intencaoDaAcao(keywordDeterministica);
        String intencaoCatalogo = intencaoClassificada(classificacao);
        List<String> topicosLegados = detectarTopicos(mensagem);
        String intencaoLegada = topicosLegados.isEmpty()
                ? "orientacao_geral" : topicosLegados.get(0);
        String intencaoCandidata = intencaoDeterministica != null
                ? intencaoDeterministica : intencaoCatalogo;

        ChatConfiancaDecisaoIa decisao = new ChatConfiancaDecisaoIa();
        decisao.setUnificadaHabilitada(properties.isUnifiedDecisionEnabled());
        decisao.setCanarioHabilitado(properties.isUnifiedDecisionCanaryEnabled());
        decisao.setEscopoCanario(escopoCanario());
        decisao.setCanarioElegivel(intencaoPermitidaNoCanario(intencaoCandidata));
        decisao.setClassificacaoCatalogo(classificacao);
        decisao.setIntencaoLegada(intencaoLegada);

        boolean possuiDecisaoUnificada = properties.isUnifiedDecisionEnabled()
                && intencaoCandidata != null
                && decisao.isCanarioElegivel();
        if (possuiDecisaoUnificada) {
            preencherDecisaoUnificada(
                    decisao,
                    mensagem,
                    keywordDeterministica,
                    intencaoDeterministica,
                    intencaoCatalogo,
                    classificacao);
            rotearPorIntencao(decisao, departamentoExplicitoId, departamentos);
        } else {
            preencherFallbackLegado(
                    decisao,
                    mensagem,
                    topicosLegados,
                    classificacao,
                    intencaoCandidata);
            rotearLegado(decisao, departamentoExplicitoId, departamentos, mensagem);
        }
        return decisao;
    }

    private void preencherDecisaoUnificada(ChatConfiancaDecisaoIa decisao,
                                            String mensagem,
                                            String keywordDeterministica,
                                            String intencaoDeterministica,
                                            String intencaoCatalogo,
                                            ChatIntencaoClassificacao classificacao) {
        boolean comandoDeterministico = intencaoDeterministica != null;
        String intencao = comandoDeterministico ? intencaoDeterministica : intencaoCatalogo;
        decisao.setAplicada(true);
        decisao.setModo("UNIFICADA");
        decisao.setStatus(comandoDeterministico
                ? "COMANDO_DETERMINISTICO" : classificacao.getStatus());
        decisao.setFonte(comandoDeterministico
                ? "REGRA_DETERMINISTICA" : classificacao.getFonte());
        decisao.setIntencao(intencao);
        decisao.setAcao(comandoDeterministico
                ? keywordDeterministica : acaoDaIntencao(intencao, mensagem));
        decisao.setFerramenta(ferramentaDaIntencao(intencao));
        decisao.setTopicos(topicosDaIntencao(intencao));

        if (Objects.equals(normalizarCodigo(intencaoCatalogo), normalizarCodigo(intencao))
                && classificacao.getMemoriasDetalhadas() != null) {
            decisao.setMemorias(List.copyOf(classificacao.getMemoriasDetalhadas()));
        }
        decisao.setMotivo(comandoDeterministico
                ? "Comando operacional reconhecido antes da classificacao probabilistica."
                : "Intencao, memorias, acao e equipe derivadas da mesma classificacao do catalogo.");
    }

    private void preencherFallbackLegado(ChatConfiancaDecisaoIa decisao,
                                          String mensagem,
                                          List<String> topicosLegados,
                                          ChatIntencaoClassificacao classificacao,
                                          String intencaoCandidata) {
        decisao.setAplicada(false);
        boolean foraCanario = properties.isUnifiedDecisionEnabled()
                && intencaoCandidata != null
                && !decisao.isCanarioElegivel();
        decisao.setModo(foraCanario ? "LEGADO_FORA_CANARIO" : "LEGADO_FALLBACK");
        decisao.setStatus(classificacao == null ? "SEM_CLASSIFICACAO" : classificacao.getStatus());
        decisao.setFonte("HEURISTICA_LEGADA");
        decisao.setIntencao(decisao.getIntencaoLegada());
        decisao.setTopicos(new ArrayList<>(topicosLegados));
        if (foraCanario) {
            decisao.setMotivo("Intencao classificada fora do escopo do canario; comportamento legado preservado.");
        } else {
            decisao.setMotivo(properties.isUnifiedDecisionEnabled()
                    ? "Catalogo sem confianca suficiente; comportamento legado preservado."
                    : "Decisao unificada desabilitada; comportamento legado preservado.");
        }
    }

    private boolean intencaoPermitidaNoCanario(String intencao) {
        if (!properties.isUnifiedDecisionCanaryEnabled()) {
            return intencao != null;
        }
        if (isBlank(intencao)) {
            return false;
        }
        String codigo = normalizarCodigo(intencao);
        return escopoCanario().stream().anyMatch(prefixo ->
                "*".equals(prefixo) || codigo.startsWith(prefixo));
    }

    private List<String> escopoCanario() {
        if (!properties.isUnifiedDecisionCanaryEnabled()) {
            return List.of();
        }
        List<String> prefixos = properties.getUnifiedDecisionCanaryIntentionPrefixes();
        if (prefixos == null) {
            return List.of();
        }
        return prefixos.stream()
                .filter(Objects::nonNull)
                .map(this::normalizarCodigo)
                .filter(prefixo -> !prefixo.isEmpty())
                .distinct()
                .toList();
    }

    private void rotearPorIntencao(ChatConfiancaDecisaoIa decisao,
                                   Long departamentoExplicitoId,
                                   List<DepartamentoUnidade> departamentos) {
        List<DepartamentoUnidade> disponiveis = departamentosOuVazio(departamentos);
        DepartamentoUnidade explicito = departamentoExplicito(
                departamentoExplicitoId, disponiveis);
        if (explicito != null) {
            decisao.setDepartamento(explicito);
            decisao.setDepartamentoConfianca(100);
            decisao.setMotivo("Equipe escolhida explicitamente pelo usuario.");
            return;
        }
        List<String> aliases = aliasesDepartamento(decisao.getIntencao());
        if (aliases.isEmpty() || disponiveis.isEmpty()) {
            return;
        }

        DepartamentoUnidade melhor = null;
        int melhorScore = 0;
        boolean empate = false;
        for (DepartamentoUnidade departamento : disponiveis) {
            String texto = textoDepartamento(departamento);
            int score = aliases.stream().mapToInt(alias ->
                    contemExpressao(texto, normalizar(alias)) ? 10 : 0).sum();
            if (score > melhorScore) {
                melhor = departamento;
                melhorScore = score;
                empate = false;
            } else if (score > 0 && score == melhorScore) {
                empate = true;
            }
        }
        if (melhor == null || melhorScore == 0 || empate) {
            return;
        }
        int confiancaClassificacao = decisao.getClassificacaoCatalogo() == null
                || decisao.getClassificacaoCatalogo().getConfianca() == null
                ? 85 : decisao.getClassificacaoCatalogo().getConfianca();
        decisao.setDepartamento(melhor);
        decisao.setDepartamentoConfianca(Math.min(95, Math.max(75, confiancaClassificacao)));
    }

    private void rotearLegado(ChatConfiancaDecisaoIa decisao,
                              Long departamentoExplicitoId,
                              List<DepartamentoUnidade> departamentos,
                              String mensagem) {
        List<DepartamentoUnidade> disponiveis = departamentosOuVazio(departamentos);
        DepartamentoUnidade explicito = departamentoExplicito(
                departamentoExplicitoId, disponiveis);
        if (explicito != null) {
            decisao.setDepartamento(explicito);
            decisao.setDepartamentoConfianca(100);
            decisao.setMotivo("Equipe escolhida explicitamente pelo usuario.");
            return;
        }
        if (disponiveis.isEmpty()) {
            decisao.setMotivo("Nenhuma equipe humana elegivel para sugestao.");
            return;
        }

        DepartamentoUnidade melhor = null;
        int melhorScore = 0;
        int segundoScore = 0;
        boolean empate = false;
        for (DepartamentoUnidade departamento : disponiveis) {
            int score = scoreDepartamentoLegado(departamento, mensagem);
            if (score > melhorScore) {
                segundoScore = melhorScore;
                melhorScore = score;
                melhor = departamento;
                empate = false;
            } else if (score == melhorScore && score > 0) {
                empate = true;
            } else if (score > segundoScore) {
                segundoScore = score;
            }
        }
        if (melhor == null || melhorScore < 10 || empate) {
            decisao.setMotivo(empate
                    ? "Mais de uma equipe possui a mesma aderencia."
                    : "A mensagem ainda nao oferece confianca suficiente.");
            return;
        }
        decisao.setDepartamento(melhor);
        decisao.setDepartamentoConfianca(Math.min(95,
                70 + Math.min(20, melhorScore) + Math.min(5, melhorScore - segundoScore)));
        decisao.setMotivo("Sugestao calculada pelo fallback de roteamento legado.");
    }

    private String intencaoClassificada(ChatIntencaoClassificacao classificacao) {
        return classificacao != null
                && "CLASSIFICADA".equals(classificacao.getStatus())
                && !isBlank(classificacao.getCodigo())
                ? classificacao.getCodigo().trim() : null;
    }

    private String intencaoDaAcao(String acao) {
        if (acao == null) {
            return null;
        }
        return switch (acao) {
            case "limites" -> "financeiro.limites";
            case "faturas" -> "financeiro.faturas";
            case "boletos" -> "financeiro.boletos";
            case "ultimas_reservas_aereas", "ultimas_vendas" -> "aereo.reservas_recentes";
            case "checkin" -> "aereo.checkin";
            case "alertas" -> "aereo.alertas_tarifa";
            case "reserva_aerea_detalhes" -> "aereo.reserva_detalhes";
            case "reserva_aerea_regras", "selecionar_reserva_remarcacao", "simular_remarcacao" ->
                    "aereo.regra_tarifaria";
            default -> acao.startsWith("familias") ? "aereo.familias_tarifarias" : null;
        };
    }

    private String acaoDaIntencao(String intencao, String mensagem) {
        if (intencao == null) {
            return null;
        }
        return switch (normalizarCodigo(intencao)) {
            case "financeiro.limites" -> "limites";
            case "financeiro.faturas" -> "faturas";
            case "financeiro.boletos" -> "boletos";
            case "aereo.reservas_recentes" -> "ultimas_reservas_aereas";
            case "aereo.checkin" -> "checkin";
            case "aereo.alertas_tarifa" -> "alertas";
            case "aereo.reserva_detalhes" -> "reserva_aerea_detalhes";
            case "aereo.regra_tarifaria" -> "reserva_aerea_regras";
            case "aereo.familias_tarifarias" -> acaoFamilias(mensagem);
            default -> null;
        };
    }

    private String acaoFamilias(String mensagem) {
        String texto = normalizar(mensagem);
        if (contemExpressao(texto, "gol") || contemExpressao(texto, "g3")) {
            return "familias;GOL";
        }
        if (contemExpressao(texto, "latam") || contemExpressao(texto, "jj")
                || contemExpressao(texto, "la")) {
            return "familias;LATAM";
        }
        if (contemExpressao(texto, "azul") || contemExpressao(texto, "ad")) {
            return "familias;AZUL";
        }
        return null;
    }

    private String ferramentaDaIntencao(String intencao) {
        if (intencao == null) {
            return null;
        }
        return switch (normalizarCodigo(intencao)) {
            case "aereo.busca_voos" -> TOOL_PESQUISAR_VOOS;
            case "aereo.melhor_tarifa_ida" -> TOOL_MELHORES_TARIFAS_IDA;
            case "aereo.melhor_tarifa_ida_volta" -> TOOL_MELHORES_TARIFAS_IDA_VOLTA;
            case "hotel.busca_hospedagem" -> TOOL_PESQUISAR_HOTEIS;
            default -> null;
        };
    }

    private List<String> topicosDaIntencao(String intencao) {
        if (isBlank(intencao)) {
            return new ArrayList<>();
        }
        Set<String> topicos = new LinkedHashSet<>();
        String codigo = normalizarCodigo(intencao);
        int separador = codigo.indexOf('.');
        if (separador > 0) {
            topicos.add(codigo.substring(0, separador));
        }
        topicos.add(codigo);
        return new ArrayList<>(topicos);
    }

    private List<String> aliasesDepartamento(String intencao) {
        String codigo = normalizarCodigo(intencao);
        if (codigo.startsWith("financeiro.")) {
            return List.of("financeiro", "faturamento", "cobranca");
        }
        if (codigo.startsWith("aereo.")) {
            return List.of("aereo", "voos", "emissao");
        }
        if (codigo.startsWith("hotel.")) {
            return List.of("hotel", "hospedagem");
        }
        if ("institucional.suporte_ti".equals(codigo)) {
            return List.of("ti", "tecnologia", "suporte");
        }
        if ("institucional.atendimento_emergencial".equals(codigo)) {
            return List.of("plantao", "emergencia", "atendimento");
        }
        if (codigo.startsWith("institucional.")) {
            return List.of("atendimento", "relacionamento");
        }
        return List.of();
    }

    private int scoreDepartamentoLegado(DepartamentoUnidade departamento, String mensagem) {
        String texto = textoDepartamento(departamento);
        String entrada = normalizar(mensagem);
        int score = 0;
        for (String termo : entrada.split(" ")) {
            if (termo.length() > 3 && texto.contains(termo)) {
                score += 3;
            }
        }
        score += scorePorGrupo(texto, entrada, "financeiro", "limite", "fatura", "boleto",
                "cobranca", "pagamento", "bsp", "faturamento", "vencimento");
        score += scorePorGrupo(texto, entrada, "reembolso", "cancelamento", "devolucao", "estorno");
        score += scorePorGrupo(texto, entrada, "aereo", "voo", "localizador", "reserva",
                "bilhete", "emissao", "checkin");
        score += scorePorGrupo(texto, entrada, "hotel", "hospedagem", "diaria");
        score += scorePorGrupo(texto, entrada, "suporte", "sistema", "erro", "acesso",
                "senha", "tecnologia");
        return score;
    }

    private List<String> detectarTopicos(String mensagem) {
        String texto = normalizar(mensagem);
        Set<String> topicos = new LinkedHashSet<>();
        adicionarTopico(topicos, texto, "financeiro", "financeiro", "limite", "fatura", "boleto",
                "cobranca", "pagamento", "bsp", "faturamento", "vencimento");
        adicionarTopico(topicos, texto, "reembolso_cancelamento", "reembolso", "cancelamento",
                "devolucao", "estorno");
        adicionarTopico(topicos, texto, "aereo", "aereo", "voo", "localizador", "reserva",
                "bilhete", "emissao", "checkin");
        adicionarTopico(topicos, texto, "hotel", "hotel", "hospedagem", "diaria");
        adicionarTopico(topicos, texto, "suporte", "suporte", "sistema", "erro", "acesso",
                "senha", "tecnologia");
        return new ArrayList<>(topicos);
    }

    private void adicionarTopico(Set<String> topicos, String texto, String topico, String... termos) {
        for (String termo : termos) {
            if (texto.contains(termo)) {
                topicos.add(topico);
                return;
            }
        }
    }

    private int scorePorGrupo(String departamento, String entrada, String... termos) {
        boolean deptCombina = false;
        boolean entradaCombina = false;
        for (String termo : termos) {
            deptCombina = deptCombina || departamento.contains(termo);
            entradaCombina = entradaCombina || entrada.contains(termo);
        }
        return deptCombina && entradaCombina ? 10 : 0;
    }

    private DepartamentoUnidade departamentoExplicito(Long id, List<DepartamentoUnidade> departamentos) {
        if (id == null) {
            return null;
        }
        return departamentos.stream()
                .filter(item -> id.equals(item.getId()))
                .findFirst()
                .orElse(null);
    }

    private List<DepartamentoUnidade> departamentosOuVazio(List<DepartamentoUnidade> departamentos) {
        return departamentos == null ? List.of() : departamentos.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private String textoDepartamento(DepartamentoUnidade departamento) {
        return normalizar(Objects.toString(departamento.getNomeExibicao(), "") + " "
                + Objects.toString(departamento.getMensagemAbertura(), ""));
    }

    private boolean contemExpressao(String texto, String termo) {
        return (" " + texto + " ").contains(" " + termo + " ");
    }

    private String normalizarCodigo(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private boolean isBlank(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}
