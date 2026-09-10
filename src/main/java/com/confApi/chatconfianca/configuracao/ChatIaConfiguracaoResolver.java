package com.confApi.chatconfianca.configuracao;

import com.confApi.chatconfianca.configuracao.ChatIaConfiguracaoSnapshot.*;
import com.confApi.chatconfianca.v2.ChatV2Capability;
import java.time.LocalDate;
import java.util.*;
import lombok.Data;

/** Pure diagnostic resolver: no dependencies on an LLM, executor, repository or customer API. */
public class ChatIaConfiguracaoResolver {
    public record Contexto(Long conversaId, Long mensagemId, String intencaoAtual, String intencaoConsulta,
            String base, Integer unidade, Integer confianca, boolean humanoSolicitado,
            Set<Long> departamentosPermitidos, Set<Integer> memoriasAtuais, String resultadoAtual,
            String fonteDecisao, String fonteConfianca, String fonteIntencaoConsulta, Long mensagemOrigemContexto) {
        public Contexto(Long conversaId, Long mensagemId, String intencaoAtual, String intencaoConsulta,
                String base, Integer unidade, Integer confianca, boolean humanoSolicitado,
                Set<Long> departamentosPermitidos, Set<Integer> memoriasAtuais, String resultadoAtual,
                String fonteDecisao, String fonteConfianca) {
            this(conversaId, mensagemId, intencaoAtual, intencaoConsulta, base, unidade, confianca,
                    humanoSolicitado, departamentosPermitidos, memoriasAtuais, resultadoAtual,
                    fonteDecisao, fonteConfianca, "TURNO_ATUAL", null);
        }
        public Contexto {
            departamentosPermitidos = departamentosPermitidos == null ? Set.of() : Set.copyOf(departamentosPermitidos);
            memoriasAtuais = memoriasAtuais == null ? Set.of() : Set.copyOf(memoriasAtuais);
        }
    }

    @Data
    public static class Resultado {
        private final String versao = "ia-config-shadow-v1";
        private final boolean aplicada = false;
        private String status;
        private Long configuracaoId, configuracaoVersao, temaId, temaVersao, perfilId, perfilVersao;
        private Long departamentoSugeridoId;
        private Integer confiancaMinima, maxSugestoes, qtdInteracoesAtendente;
        private final String autorizacaoAcoes = "NAO_AVALIADA_PELO_SHADOW";
        private final List<String> motivos = new ArrayList<>();
        private final List<EtapaResultado> etapas = new ArrayList<>();
        private final List<MemoriaResultado> conhecimentos = new ArrayList<>();
        private final Set<Integer> memoriasPropostas = new LinkedHashSet<>();
    }
    public record EtapaResultado(Long id, Long versao, Integer ordem, String tipo, String condicao,
            Long acaoId, Long acaoVersao, String acaoCodigo, String status, boolean exigeConfirmacao) {}
    public record MemoriaResultado(Long vinculoId, Long versao, Integer memoriaId, Integer editorialVersao,
            List<String> motivos) {}

    public Resultado resolver(ChatIaConfiguracaoSnapshot snapshot, Contexto ctx, LocalDate hoje) {
        Resultado r = new Resultado();
        if (snapshot == null || !r.getVersao().equals(snapshot.getVersaoContrato())) {
            r.setStatus("CONTRATO_INCOMPATIVEL"); return r;
        }
        Configuracao c = snapshot.getConfiguracao();
        if (c == null) {
            r.setStatus(ctx.humanoSolicitado() ? "ESCOLHER_DEPARTAMENTO" : "SEM_CONFIGURACAO");
            r.getMotivos().add("INTENCAO_SEM_CADASTRO"); return r;
        }
        r.setConfiguracaoId(c.getId()); r.setConfiguracaoVersao(c.getVersao());
        r.setTemaId(c.getTemaId()); r.setTemaVersao(c.getTemaVersao());
        r.setPerfilId(c.getPerfilId()); r.setPerfilVersao(c.getPerfilVersao());
        r.setConfiancaMinima(c.getConfiancaMinima()); r.setMaxSugestoes(c.getMaxSugestoes());
        r.setQtdInteracoesAtendente(c.getQtdInteracoesAtendente());
        List<String> motivos = r.getMotivos();
        if (!Objects.equals(ctx.intencaoConsulta(), c.getIntencaoCodigo())) motivos.add("INTENCAO_DIVERGENTE");
        if (!ativo(c.getIntencaoStatus())) motivos.add("INTENCAO_LEGADA_INATIVA");
        if (!ativo(c.getStatus())) motivos.add("CONFIGURACAO_INATIVA");
        if (!ativo(c.getTemaStatus())) motivos.add("TEMA_INATIVO_OU_AUSENTE");
        if (c.getPerfilId() == null) motivos.add("PERFIL_NAO_VINCULADO");
        else {
            if (!ativo(c.getPerfilStatus())) motivos.add("PERFIL_INATIVO_OU_AUSENTE");
            if (!escopo(c.getPerfilEscopo(), c.getPerfilBase(), c.getPerfilUnidade(), ctx)) motivos.add("PERFIL_FORA_ESCOPO");
        }
        if (c.getConfiancaMinima() == null || c.getConfiancaMinima() < 0 || c.getConfiancaMinima() > 100)
            motivos.add("LIMIAR_CONFIANCA_INVALIDO");
        boolean cadeiaValida = motivos.isEmpty();
        boolean confiancaValida = ctx.confianca() != null && ctx.confianca() >= 0 && ctx.confianca() <= 100;
        if (!confiancaValida) motivos.add("CONFIANCA_NAO_DISPONIVEL_PARA_INTENCAO");
        else if (c.getConfiancaMinima() != null && ctx.confianca() < c.getConfiancaMinima()) motivos.add("CONFIANCA_INSUFICIENTE");
        boolean pronto = motivos.isEmpty();

        // A human request never waits for an API/memory query. A department is only a proposal.
        if (ctx.humanoSolicitado()) {
            if (pronto && c.getDepartamentoId() != null && ctx.departamentosPermitidos().contains(c.getDepartamentoId())) {
                r.setDepartamentoSugeridoId(c.getDepartamentoId()); r.setStatus("CONFIRMAR_DEPARTAMENTO");
            } else {
                r.setStatus("ESCOLHER_DEPARTAMENTO");
                if (c.getDepartamentoId() != null && !ctx.departamentosPermitidos().contains(c.getDepartamentoId()))
                    motivos.add("DEPARTAMENTO_NAO_DISPONIVEL_NA_SESSAO");
            }
            return r;
        }

        List<Conhecimento> conhecimentos = snapshot.getConhecimentos() == null ? List.of() : snapshot.getConhecimentos();
        // A specific source that is expired/unpublished must not silently fall back to general content.
        int nivel = conhecimentos.stream().filter(k -> ativo(k.getStatus()) && Boolean.TRUE.equals(k.getVinculoIntencaoAtivo())
                && escopo(k.getEscopo(), k.getBase(), k.getCodgUnidade(), ctx))
                .mapToInt(k -> especificidade(k.getEscopo())).max().orElse(0);
        List<Conhecimento> especificos = conhecimentos.stream().filter(k -> especificidade(k.getEscopo()) == nivel
                && ativo(k.getStatus()) && Boolean.TRUE.equals(k.getVinculoIntencaoAtivo())
                && escopo(k.getEscopo(), k.getBase(), k.getCodgUnidade(), ctx)).toList();
        boolean complementar = nivel > 0 && !especificos.isEmpty() && especificos.stream().allMatch(k ->
                Boolean.TRUE.equals(k.getPermitirComplementoGeral()) && impedimentos(k, ctx, hoje).isEmpty());
        for (Conhecimento k : conhecimentos) {
            List<String> bloqueios = impedimentos(k, ctx, hoje);
            if (especificidade(k.getEscopo()) < nivel
                    && !("GERAL".equals(k.getEscopo()) && complementar)) bloqueios.add("SUPRIMIDO_POR_ESCOPO_ESPECIFICO");
            if (!pronto) bloqueios.add("CADEIA_OU_CONFIANCA_BLOQUEADA");
            if (bloqueios.isEmpty()) r.getMemoriasPropostas().add(k.getCodgMemoria());
            r.getConhecimentos().add(new MemoriaResultado(k.getId(), k.getVersao(), k.getCodgMemoria(),
                    k.getEditorialVersao(), List.copyOf(bloqueios)));
        }

        List<Etapa> etapas = snapshot.getEtapas() == null ? List.of() : snapshot.getEtapas();
        for (Etapa e : etapas.stream().sorted(Comparator.comparing(Etapa::getOrdem,
                Comparator.nullsLast(Comparator.naturalOrder()))).toList()) {
            ChatV2Capability capability = ChatV2Capability.from(e.getAcaoCodigo());
            boolean confirmar = Boolean.TRUE.equals(e.getExigirConfirmacao())
                    || (capability != null && capability.exigeConfirmacao());
            String status = etapa(e, c, capability, confirmar, pronto, r.getMemoriasPropostas());
            r.getEtapas().add(new EtapaResultado(e.getId(), e.getVersao(), e.getOrdem(), e.getTipo(), e.getCondicao(),
                    e.getAcaoId(), e.getAcaoVersao(), e.getAcaoCodigo(), status, confirmar));
        }
        if (!cadeiaValida) r.setStatus("CADASTRO_BLOQUEADO");
        else if (!pronto) r.setStatus("ESCLARECER");
        else if (r.getEtapas().stream().noneMatch(e -> e.status().equals("PROPOSTA"))) r.setStatus("SEM_ETAPA_PROPONIVEL");
        else r.setStatus("PLANO_PARA_COMPARACAO");
        return r;
    }

    private String etapa(Etapa e, Configuracao c, ChatV2Capability capability, boolean confirmar,
            boolean pronto, Set<Integer> memorias) {
        if (!ativo(e.getStatus())) return "ETAPA_INATIVA";
        if (!Set.of("CONSULTAR_ACAO", "CONSULTAR_CONHECIMENTO", "PERGUNTAR", "SUGERIR_ACAO", "OFERECER_ATENDIMENTO")
                .contains(Objects.toString(e.getTipo(), ""))) return "TIPO_DESCONHECIDO";
        if (e.getOrdem() == null || e.getOrdem() < 1 || e.getOrdem() > 100) return "ORDEM_INVALIDA";
        boolean acao = "CONSULTAR_ACAO".equals(e.getTipo()) || "SUGERIR_ACAO".equals(e.getTipo());
        if (acao) {
            if (!ativo(e.getAcaoStatus())) return "ACAO_INATIVA_OU_AUSENTE";
            if (capability == null) return "ACAO_FORA_CATALOGO";
            if (!Objects.equals(c.getTemaId(), e.getAcaoTemaId())) return "ACAO_DE_OUTRO_TEMA";
            if (e.getLimiteResultados() == null || e.getLimiteResultados() < 1 || e.getLimiteResultados() > 10
                    || e.getTimeoutSegundos() == null || e.getTimeoutSegundos() < 1 || e.getTimeoutSegundos() > 120)
                return "PARAMETROS_ACAO_INVALIDOS";
            if (confirmar && "CONSULTAR_ACAO".equals(e.getTipo())) return "EXIGE_CONFIRMACAO_NAO_CONSULTAR";
        } else if (e.getAcaoId() != null) return "ACAO_INDEVIDA_NA_ETAPA";
        if (!pronto) return "CADEIA_OU_CONFIANCA_BLOQUEADA";
        // Results from the live path cannot establish the outcome of a different proposed stage.
        if (Set.of("COM_DADOS", "SEM_DADOS", "DADOS_FALTANTES").contains(Objects.toString(e.getCondicao(), "")))
            return "PENDENTE_RESULTADO_DA_ETAPA";
        if ("A_PEDIDO_USUARIO".equals(e.getCondicao())) return "AGUARDANDO_PEDIDO_USUARIO";
        if (!"SEMPRE".equals(e.getCondicao())) return "CONDICAO_DESCONHECIDA";
        if ("CONSULTAR_CONHECIMENTO".equals(e.getTipo()) && memorias.isEmpty()) return "SEM_CONHECIMENTO_ELEGIVEL";
        return "PROPOSTA";
    }

    private List<String> impedimentos(Conhecimento k, Contexto ctx, LocalDate hoje) {
        List<String> r = new ArrayList<>();
        if (!ativo(k.getStatus())) r.add("VINCULO_INATIVO");
        if (k.getCodgMemoria() == null || !ativo(k.getMemoriaStatus())) r.add("MEMORIA_INATIVA_OU_AUSENTE");
        if (!Boolean.TRUE.equals(k.getVinculoIntencaoAtivo())) r.add("SEM_VINCULO_ATIVO_COM_INTENCAO");
        if (!escopo(k.getEscopo(), k.getBase(), k.getCodgUnidade(), ctx)) r.add("FORA_ESCOPO_CADASTRADO");
        if (!geral(k.getMemoriaBase()) && !mesmaBase(k.getMemoriaBase(), ctx.base())) r.add("BASE_ORIGINAL_INCOMPATIVEL");
        if (k.getEditorialUnidade() != null && !Objects.equals(k.getEditorialUnidade(), ctx.unidade())) r.add("UNIDADE_EDITORIAL_INCOMPATIVEL");
        if (k.getEditorialVersao() == null || !"PUBLICADO".equals(k.getStatusPublicacao())) r.add("MEMORIA_NAO_PUBLICADA");
        if (!Boolean.TRUE.equals(k.getPossuiTexto())) r.add("MEMORIA_SEM_CONTEUDO");
        if (!Boolean.TRUE.equals(k.getPossuiFonte())) r.add("MEMORIA_SEM_FONTE");
        if ((k.getVigenteDe() != null && hoje.isBefore(k.getVigenteDe()))
                || (k.getVigenteAte() != null && hoje.isAfter(k.getVigenteAte()))) r.add("FORA_VIGENCIA");
        if (k.getVigenteDe() != null && k.getVigenteAte() != null && k.getVigenteAte().isBefore(k.getVigenteDe())) r.add("VIGENCIA_INVALIDA");
        return r;
    }

    private static boolean ativo(Integer s) { return Integer.valueOf(1).equals(s); }
    private static boolean geral(String base) { return base == null || base.isBlank() || "geral".equalsIgnoreCase(base.trim()); }
    private static boolean mesmaBase(String a, String b) { return a != null && !a.isBlank() && b != null && a.trim().equalsIgnoreCase(b.trim()); }
    private static int especificidade(String e) { return "UNIDADE".equals(e) ? 2 : "BASE".equals(e) ? 1 : 0; }
    private static boolean escopo(String e, String base, Integer unidade, Contexto ctx) {
        return switch (Objects.toString(e, "")) {
            case "GERAL" -> base == null && unidade == null;
            case "BASE" -> unidade == null && mesmaBase(base, ctx.base());
            case "UNIDADE" -> base == null && unidade != null && unidade > 0 && unidade.equals(ctx.unidade());
            default -> false;
        };
    }
}
