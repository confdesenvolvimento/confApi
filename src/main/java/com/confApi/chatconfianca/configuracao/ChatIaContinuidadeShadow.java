package com.confApi.chatconfianca.configuracao;

import com.confApi.chatconfianca.configuracao.ChatIaConfiguracaoResolver.Contexto;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/** Bounded diagnostic topic state. Never stores messages, dates, replies or customer results. */
final class ChatIaContinuidadeShadow {
    private static final String BSP = "financeiro.calendario_bsp";
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private final Map<Long, Turno> recentes = new LinkedHashMap<>();
    private final LongSupplier relogio;
    private final int limite;
    private final long validade;
    private record Escopo(Integer usuario, Integer agencia, String base, Integer unidade) {}
    private record Turno(Escopo escopo, Long mensagemId, Long origemId, long inicio, boolean bsp) {}

    ChatIaContinuidadeShadow() { this(System::nanoTime, 1000, TimeUnit.MINUTES.toNanos(15)); }
    ChatIaContinuidadeShadow(LongSupplier relogio, int limite, long validade) {
        this.relogio = relogio; this.limite = Math.max(1, limite); this.validade = Math.max(1, validade);
    }

    synchronized Contexto observar(Contexto atual, Integer usuario, Integer agencia, String mensagem,
            boolean conversaAssistida, boolean semEvidenciaAtual) {
        long agora = relogio.getAsLong();
        recentes.values().removeIf(t -> agora - t.inicio() < 0 || agora - t.inicio() >= validade);
        if (atual.conversaId() == null || atual.mensagemId() == null) return atual;
        if (usuario == null || usuario <= 0 || agencia == null || agencia <= 0
                || atual.unidade() == null || atual.unidade() <= 0
                || atual.base() == null || atual.base().isBlank()) {
            recentes.remove(atual.conversaId()); return atual;
        }
        Escopo escopo = new Escopo(usuario, agencia, atual.base(), atual.unidade());
        Turno anterior = recentes.get(atual.conversaId());
        // An older request completing late cannot resurrect a topic or consume newer context.
        if (anterior != null && atual.mensagemId() <= anterior.mensagemId()) return atual;
        boolean continuar = conversaAssistida && !atual.humanoSolicitado() && semEvidenciaAtual
                && geral(atual.intencaoAtual()) && geral(atual.intencaoConsulta())
                && anterior != null && anterior.bsp() && escopo.equals(anterior.escopo())
                && dataIsoladaValida(mensagem);
        boolean bsp = conversaAssistida && !atual.humanoSolicitado()
                && BSP.equals(atual.intencaoConsulta()) && atual.confianca() != null
                && atual.confianca() > 0 && atual.confianca() <= 100;
        Contexto resultado = atual;
        if (continuar) {
            // Preserve the actual decision and do not fabricate a new classification score.
            resultado = new Contexto(atual.conversaId(), atual.mensagemId(), atual.intencaoAtual(), BSP,
                    atual.base(), atual.unidade(), null, false, atual.departamentosPermitidos(),
                    atual.memoriasAtuais(), atual.resultadoAtual(), atual.fonteDecisao(),
                    "CONTEXTO_SEM_NOVO_SCORE", "CONTINUIDADE_BSP_DATA", anterior.origemId());
        }
        recentes.remove(atual.conversaId());
        recentes.put(atual.conversaId(), new Turno(escopo, atual.mensagemId(),
                continuar ? anterior.origemId() : atual.mensagemId(),
                continuar ? anterior.inicio() : agora, bsp || continuar));
        while (recentes.size() > limite) recentes.remove(recentes.keySet().iterator().next());
        return resultado;
    }

    private static boolean geral(String codigo) { return codigo == null || "orientacao_geral".equals(codigo); }
    private static boolean dataIsoladaValida(String mensagem) {
        if (mensagem == null || mensagem.length() > 32) return false;
        String texto = mensagem.trim();
        if (!texto.matches("[0-9]{2}([/-])[0-9]{2}\\1[0-9]{4}")) return false;
        try {
            LocalDate data = LocalDate.parse(texto.replace('-', '/'), DATA);
            return data.getYear() >= 1900 && data.getYear() <= 9999;
        } catch (RuntimeException ex) { return false; }
    }
}
