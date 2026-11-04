package com.confApi.chatconfianca.indicadores;

import com.confApi.chatconfianca.client.ChatConfiancaManagerClient;
import com.confApi.exception.ServiceIndisponivelException;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/** Contrato interno fechado: nunca aceita agencia, SQL ou endpoint escolhido pelo modelo. */
@Service
public class ChatVendaAereaIndicadoresClient {
    private static final String PATH = "chat-confianca/consultas/vendas-aereas/indicadores";
    private final ChatConfiancaManagerClient manager;

    public ChatVendaAereaIndicadoresClient(ChatConfiancaManagerClient manager) { this.manager = manager; }

    public VendaAereaIndicadoresResultado consultar(Integer agencia, VendaAereaIndicadoresFiltros filtros) {
        var somenteContagens = validarFiltros(filtros);
        somenteContagens.setIncluirValores(false);
        return consultar(agencia, null, somenteContagens);
    }

    /** Usuario vem da sessao autenticada/revalidada, nunca de um campo escolhido pelo modelo. */
    public VendaAereaIndicadoresResultado consultar(Integer agencia, Integer codgUsuario, VendaAereaIndicadoresFiltros filtros) {
        if (agencia == null || agencia <= 0) throw new IllegalArgumentException("Agencia autenticada obrigatoria.");
        var normalizados = validarFiltros(filtros);
        if (codgUsuario != null && codgUsuario <= 0) throw new IllegalArgumentException("Usuario autenticado invalido.");
        if (codgUsuario == null) normalizados.setIncluirValores(false);
        var resultado = manager.getEncoded(caminho(agencia, codgUsuario, normalizados), VendaAereaIndicadoresResultado.class);
        validarResposta(agencia, normalizados, resultado);
        return resultado;
    }

    /** Retorna uma copia; replay de contexto passa pela mesma validacao do pedido novo. */
    public static VendaAereaIndicadoresFiltros validarFiltros(VendaAereaIndicadoresFiltros recebidos) {
        if (recebidos == null) throw new IllegalArgumentException("Informe o periodo da consulta.");
        var f = recebidos.copy();
        if (f.getInicio() == null || f.getFim() == null
                || f.getInicio().getYear() < 1900 || f.getFim().getYear() > 2100
                || f.getInicio().isAfter(f.getFim()) || ChronoUnit.DAYS.between(f.getInicio(), f.getFim()) >= 366)
            throw new IllegalArgumentException("Informe um periodo valido de ate 366 dias, pela data de emissao.");
        f.setOrigem(texto(f.getOrigem(), 100)); f.setDestino(texto(f.getDestino(), 100));
        f.setCompanhia(texto(f.getCompanhia(), 100)); f.setNomePassageiro(texto(f.getNomePassageiro(), 100));
        if (f.getNomePassageiro() != null && f.getNomePassageiro().split(" ").length > 8)
            throw new IllegalArgumentException("Informe ate oito partes do nome do passageiro.");
        String agrupamento = f.getAgrupamento() == null || f.getAgrupamento().isBlank()
                ? "NENHUM" : f.getAgrupamento().strip().toUpperCase(Locale.ROOT);
        if (!Set.of("NENHUM", "COMPANHIA").contains(agrupamento))
            throw new IllegalArgumentException("Agrupamento nao disponivel nesta consulta.");
        f.setAgrupamento(agrupamento);
        if (f.getPage() == null || f.getPage() < 0 || f.getPage() > 10000
                || f.getSize() == null || f.getSize() < 1 || f.getSize() > 50)
            throw new IllegalArgumentException("Paginacao invalida.");
        return f;
    }

    static String caminho(Integer agencia, VendaAereaIndicadoresFiltros f) {
        return caminho(agencia, null, f);
    }

    static String caminho(Integer agencia, Integer codgUsuario, VendaAereaIndicadoresFiltros f) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put("codgAgencia", agencia); valores.put("inicio", f.getInicio()); valores.put("fim", f.getFim());
        if (codgUsuario != null) valores.put("codgUsuario", codgUsuario);
        valores.put("agrupamento", f.getAgrupamento()); valores.put("page", f.getPage()); valores.put("size", f.getSize());
        valores.put("detalhar", f.isDetalhar());
        valores.put("incluirValores", f.isIncluirValores());
        if (f.getOrigem() != null) valores.put("origem", f.getOrigem());
        if (f.getDestino() != null) valores.put("destino", f.getDestino());
        if (f.getCompanhia() != null) valores.put("companhia", f.getCompanhia());
        if (f.getNomePassageiro() != null) valores.put("nomePassageiro", f.getNomePassageiro());
        var uri = UriComponentsBuilder.fromPath(PATH);
        valores.forEach((nome, valor) -> uri.queryParam(nome, "{" + nome + "}"));
        return uri.encode().buildAndExpand(valores).toUriString();
    }

    private static String texto(String valor, int tamanho) {
        if (valor == null) return null;
        if (valor.codePoints().anyMatch(Character::isISOControl) || valor.length() > tamanho)
            throw new IllegalArgumentException("Filtro textual invalido.");
        if (valor.isBlank()) return null;
        return valor.strip().replaceAll("\\s+", " ");
    }

    private static void validarResposta(Integer agencia, VendaAereaIndicadoresFiltros f, VendaAereaIndicadoresResultado r) {
        if (r == null || !VendaAereaIndicadoresResultado.SCHEMA.equals(r.getSchema())
                || !Objects.equals(agencia, r.getCodgAgencia()) || r.getTotais() == null
                || !Set.of("OK", "SEM_RESULTADO").contains(r.getStatus() == null ? "" : r.getStatus())
                || vazio(r.getCriterioContagem()) || vazio(r.getCriterioValor())
                || r.getLimitacoes() == null || r.getLimitacoes().size() > 20
                || r.getGrupos() == null || r.getGrupos().size() > 100
                || r.getTotaisPorMoeda() == null || r.getReservas() == null
                || r.isReembolsosDisponiveis() || r.getPage() != f.getPage() || r.getSize() != f.getSize()) throw invalida();
        try { if (!equivalentes(f, validarFiltros(r.getFiltrosAplicados()))) throw invalida(); }
        catch (IllegalArgumentException ex) { throw invalida(); }
        validarMedida(r.getTotais());
        var valoresGlobais = validarValores(f, r.getStatusValor(), r.getMotivoValor(), r.getComposicaoValor(),
                r.getPassageirosValorIncompleto(), r.getTotaisPorMoeda(), r.getTotais(), true);
        long totalBase;
        try { totalBase = Math.addExact(r.getTotais().getReservasEmitidas(), Math.addExact(r.getTotais().getReservasCanceladas(), r.getTotais().getReservasOutrasSituacoes())); }
        catch (ArithmeticException ex) { throw invalida(); }
        if ((totalBase == 0) != "SEM_RESULTADO".equals(r.getStatus())) throw invalida();
        if (totalBase == 0 && (r.getTotais().getBilhetesCancelados() != 0
                || r.getTotais().getBilhetesOutrasSituacoes() != 0)) throw invalida();
        long total = r.getTotais().getReservasEmitidas();
        if (r.getTotalDetalhes() != total) throw invalida();
        long paginas = total / f.getSize() + (total % f.getSize() == 0 ? 0 : 1);
        if (paginas > Integer.MAX_VALUE || r.getTotalPages() != paginas
                || r.isHasNext() != (f.isDetalhar() && f.getPage() + 1L < paginas)) throw invalida();
        long offset = (long) f.getPage() * f.getSize();
        long esperado = !f.isDetalhar() || offset >= total ? 0 : Math.min(f.getSize(), total - offset);
        if (r.getReservas().size() != esperado) throw invalida();
        Set<Integer> ids = new HashSet<>(); Set<String> localizadores = new HashSet<>();
        for (var item : r.getReservas()) {
            if (item == null || item.getReservaId() == null || item.getReservaId() <= 0 || !ids.add(item.getReservaId())
                    || !Integer.valueOf(3).equals(item.getStatus()) || item.getDataCancelamento() != null
                    || item.getDataEmissao() == null || item.getDataEmissao().toLocalDate().isBefore(f.getInicio())
                    || item.getDataEmissao().toLocalDate().isAfter(f.getFim())) throw invalida();
            if (item.getLocalizador() != null && !item.getLocalizador().isBlank())
                localizadores.add(item.getLocalizador().strip().toUpperCase(Locale.ROOT));
        }
        if (localizadores.size() > r.getTotais().getLocalizadoresEmitidos()
                || f.isDetalhar() && f.getPage() == 0 && total <= f.getSize()
                && localizadores.size() != r.getTotais().getLocalizadoresEmitidos()) throw invalida();
        if ("NENHUM".equals(f.getAgrupamento()) && (!r.getGrupos().isEmpty() || r.isGruposTruncados())) throw invalida();
        Set<String> grupos = new HashSet<>();
        for (var g : r.getGrupos()) {
            if (g == null || vazio(g.getChave()) || g.getChave().length() > 200 || vazio(g.getDescricao())
                    || g.getDescricao().length() > 200 || !grupos.add(g.getChave()) || g.getTotais() == null) throw invalida();
            validarMedida(g.getTotais());
            validarValores(f, g.getStatusValor(), g.getMotivoValor(), g.getComposicaoValor(),
                    g.getPassageirosValorIncompleto(), g.getTotaisPorMoeda(), g.getTotais(), false);
            if (!Objects.equals(r.getStatusValor(), g.getStatusValor())
                    || !Objects.equals(r.getMotivoValor(), g.getMotivoValor())) throw invalida();
            if (g.getTotais().getReservasEmitidas() > total || g.getTotais().getPassageirosEmitidos() > r.getTotais().getPassageirosEmitidos()) throw invalida();
        }
        if ("COMPANHIA".equals(f.getAgrupamento())) {
            if (r.isGruposTruncados() && r.getGrupos().size() != 100) throw invalida();
            long[] somas = new long[7];
            try {
                for (var g : r.getGrupos()) {
                    var m = g.getTotais();
                    long[] valores = {m.getReservasEmitidas(), m.getPassageirosEmitidos(), m.getBilhetesAtivos(),
                            m.getReservasCanceladas(), m.getBilhetesCancelados(), m.getReservasOutrasSituacoes(), m.getBilhetesOutrasSituacoes()};
                    for (int i = 0; i < valores.length; i++) somas[i] = Math.addExact(somas[i], valores[i]);
                }
            } catch (ArithmeticException ex) { throw invalida(); }
            var t = r.getTotais();
            long[] globais = {t.getReservasEmitidas(), t.getPassageirosEmitidos(), t.getBilhetesAtivos(),
                    t.getReservasCanceladas(), t.getBilhetesCancelados(), t.getReservasOutrasSituacoes(), t.getBilhetesOutrasSituacoes()};
            for (int i = 0; i < globais.length; i++)
                if (somas[i] > globais[i] || !r.isGruposTruncados() && somas[i] != globais[i]) throw invalida();
            if ("COMPLETO".equals(r.getStatusValor())) {
                Map<String, BigDecimal> somasMoedas = new LinkedHashMap<>();
                for (var g : r.getGrupos()) for (var m : g.getTotaisPorMoeda())
                    somasMoedas.merge(m.getMoeda(), m.getValorBrutoEmitido(), BigDecimal::add);
                for (var entry : somasMoedas.entrySet())
                    if (!valoresGlobais.containsKey(entry.getKey())) throw invalida();
                if (!r.isGruposTruncados() && (somasMoedas.size() != valoresGlobais.size()
                        || valoresGlobais.entrySet().stream().anyMatch(e -> somasMoedas.get(e.getKey()).compareTo(e.getValue()) != 0))) throw invalida();
            }
        }
    }

    private static void validarMedida(VendaAereaIndicadoresResultado.Medida m) {
        if (m == null || m.getReservasEmitidas() < 0 || m.getLocalizadoresEmitidos() < 0
                || m.getPassageirosEmitidos() < 0 || m.getBilhetesAtivos() < 0 || m.getReservasCanceladas() < 0
                || m.getBilhetesCancelados() < 0 || m.getReservasOutrasSituacoes() < 0 || m.getBilhetesOutrasSituacoes() < 0
                || m.getLocalizadoresEmitidos() > m.getReservasEmitidas()
                || m.getReservasEmitidas() == 0 && (m.getPassageirosEmitidos() != 0 || m.getBilhetesAtivos() != 0)) throw invalida();
    }

    private static Map<String, BigDecimal> validarValores(VendaAereaIndicadoresFiltros f, String status, String motivo,
            String composicao, long incompletos, java.util.List<VendaAereaIndicadoresResultado.MedidaValor> moedas,
            VendaAereaIndicadoresResultado.Medida totais, boolean global) {
        long passageiros = totais.getPassageirosEmitidos();
        if (status == null || !Set.of("COMPLETO", "INCOMPLETO", "INDISPONIVEL_SEM_PERMISSAO", "INDISPONIVEL").contains(status)
                || incompletos < 0 || incompletos > passageiros) throw invalida();
        if (moedas == null || moedas.size() > 100) throw invalida();
        boolean completo = "COMPLETO".equals(status);
        boolean incompleto = "INCOMPLETO".equals(status);
        if (completo || incompleto) {
            if (!f.isIncluirValores() || !VendaAereaIndicadoresResultado.COMPOSICAO.equals(composicao)
                    || completo && (incompletos != 0 || motivo != null || passageiros < totais.getReservasEmitidas())
                    || incompleto && !"DADOS_INCOMPLETOS".equals(motivo)
                    || global && incompleto && incompletos == 0 && totais.getReservasEmitidas() == 0) throw invalida();
        } else {
            if (composicao != null || incompletos != 0 || !moedas.isEmpty()
                    || "INDISPONIVEL_SEM_PERMISSAO".equals(status) && (!f.isIncluirValores() || !"SEM_PERMISSAO".equals(motivo))
                    || "INDISPONIVEL".equals(status) && !(f.isIncluirValores()
                    ? "FONTE_INDISPONIVEL".equals(motivo) : "NAO_SOLICITADO".equals(motivo))) throw invalida();
        }
        Set<String> vistas = new HashSet<>(); long passageirosClassificados = 0;
        Map<String, BigDecimal> valores = new LinkedHashMap<>();
        for (var m : moedas) {
            if (m == null || !moedaValida(m.getMoeda()) || !vistas.add(m.getMoeda()) || m.getPassageirosComValor() <= 0
                    || m.getPassageirosComValor() > passageiros || m.isCompleto() != completo
                    || completo && (m.getValorBrutoEmitido() == null
                    || m.getValorBrutoEmitido().precision() > 65 || m.getValorBrutoEmitido().scale() > 12 || m.getValorBrutoEmitido().scale() < -40)
                    || !completo && m.getValorBrutoEmitido() != null) throw invalida();
            try { passageirosClassificados = Math.addExact(passageirosClassificados, m.getPassageirosComValor()); }
            catch (ArithmeticException ex) { throw invalida(); }
            if (completo) valores.put(m.getMoeda(), m.getValorBrutoEmitido());
        }
        if (passageirosClassificados > passageiros || completo && passageirosClassificados != passageiros) throw invalida();
        return valores;
    }

    private static boolean moedaValida(String codigo) {
        if (codigo == null || !codigo.matches("[A-Z]{3}") || Set.of("XXX", "XTS").contains(codigo)) return false;
        try { return java.util.Currency.getInstance(codigo).getDefaultFractionDigits() >= 0; }
        catch (IllegalArgumentException ex) { return false; }
    }

    private static boolean equivalentes(VendaAereaIndicadoresFiltros a, VendaAereaIndicadoresFiltros b) {
        return Objects.equals(a.getInicio(), b.getInicio()) && Objects.equals(a.getFim(), b.getFim())
                && iguais(a.getOrigem(), b.getOrigem()) && iguais(a.getDestino(), b.getDestino())
                && iguais(a.getCompanhia(), b.getCompanhia()) && iguais(a.getNomePassageiro(), b.getNomePassageiro())
                && Objects.equals(a.getAgrupamento(), b.getAgrupamento()) && Objects.equals(a.getPage(), b.getPage())
                && Objects.equals(a.getSize(), b.getSize()) && a.isDetalhar() == b.isDetalhar()
                && a.isIncluirValores() == b.isIncluirValores();
    }
    private static boolean iguais(String a, String b) { return a == null ? b == null : b != null && a.equalsIgnoreCase(b); }
    private static boolean vazio(String texto) { return texto == null || texto.isBlank(); }
    private static ServiceIndisponivelException invalida() {
        return new ServiceIndisponivelException("A consulta de indicadores nao retornou dados validos; nenhum total foi confirmado.");
    }
}
