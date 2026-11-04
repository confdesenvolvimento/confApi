package com.confApi.chatconfianca.reservas;

import com.confApi.chatconfianca.client.ChatConfiancaManagerClient;
import com.confApi.chatconfianca.dto.reserva.ReservaAereaRecenteItem;
import com.confApi.exception.ServiceIndisponivelException;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Consulta paginada autorizada pela agência da sessão; nunca interpreta ausência como erro vazio. */
@Service
public class ChatReservaConsultaClient {
    private static final String PATH = "chat-confianca/consultas/reservas-aereas";
    private final ChatConfiancaManagerClient manager;

    public ChatReservaConsultaClient(ChatConfiancaManagerClient manager) {
        this.manager = manager;
    }

    public ReservaConsultaPagina consultar(Integer agencia, ReservaConsultaFiltros filtros) {
        if (agencia == null || agencia <= 0) {
            throw new IllegalArgumentException("Agência autenticada é obrigatória para consultar reservas.");
        }
        ReservaConsultaFiltros request = normalizar(filtros);
        ReservaConsultaPagina pagina = manager.getEncoded(caminho(agencia, request), ReservaConsultaPagina.class);
        validarResposta(agencia, request, pagina);
        return pagina;
    }

    static ReservaConsultaFiltros normalizar(ReservaConsultaFiltros filtros) {
        if (filtros == null) {
            throw new IllegalArgumentException("Informe os filtros da consulta de reservas.");
        }
        ReservaConsultaFiltros f = filtros.copy();
        f.setNomePassageiro(texto(f.getNomePassageiro(), 150));
        if (f.getNomePassageiro() != null && f.getNomePassageiro().split(" ").length > 8) {
            throw new IllegalArgumentException("Informe até oito partes do nome do passageiro.");
        }
        f.setOrigem(texto(f.getOrigem(), 120));
        f.setDestino(texto(f.getDestino(), 120));
        f.setCompanhia(texto(f.getCompanhia(), 120));
        f.setLocalizador(texto(f.getLocalizador(), 30));
        f.setSituacao(opcao(f.getSituacao(), "TODAS", Set.of("TODAS", "EMITIDAS")));
        f.setTipoData(opcao(f.getTipoData(), "CRIACAO", Set.of("CRIACAO", "EMISSAO")));
        if (f.getPage() == null || f.getPage() < 0 || f.getPage() > 10000
                || f.getSize() == null || f.getSize() < 1 || f.getSize() > 50) {
            throw new IllegalArgumentException("Página ou tamanho inválido na consulta de reservas.");
        }
        if (f.getReservaId() != null && f.getReservaId() <= 0) {
            throw new IllegalArgumentException("Código de reserva inválido.");
        }
        if (!dataValida(f.getInicio()) || !dataValida(f.getFim())
                || (f.getInicio() != null && f.getFim() != null && f.getInicio().isAfter(f.getFim()))) {
            throw new IllegalArgumentException("Período inválido para consultar reservas.");
        }
        return f;
    }

    static String caminho(Integer agencia, ReservaConsultaFiltros f) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath(PATH)
                .queryParam("codgAgencia", agencia)
                .queryParam("situacao", f.getSituacao())
                .queryParam("tipoData", f.getTipoData())
                .queryParam("page", f.getPage())
                .queryParam("size", f.getSize());
        adicionar(uri, "nomePassageiro", f.getNomePassageiro());
        adicionar(uri, "origem", f.getOrigem());
        adicionar(uri, "destino", f.getDestino());
        adicionar(uri, "companhia", f.getCompanhia());
        adicionar(uri, "localizador", f.getLocalizador());
        adicionar(uri, "reservaId", f.getReservaId());
        adicionar(uri, "inicio", f.getInicio());
        adicionar(uri, "fim", f.getFim());
        // Valores são variáveis URI: '&', '+', '#', '{' e acentos não viram delimitadores.
        java.util.Map<String, Object> variables = new java.util.LinkedHashMap<>();
        UriComponentsBuilder encoded = UriComponentsBuilder.fromPath(PATH);
        uri.build().getQueryParams().forEach((key, values) -> {
            String variable = "v" + variables.size();
            variables.put(variable, values.get(0));
            encoded.queryParam(key, "{" + variable + "}");
        });
        return encoded.encode().buildAndExpand(variables).toUriString();
    }

    private static void adicionar(UriComponentsBuilder uri, String key, Object value) {
        if (value != null) uri.queryParam(key, value);
    }

    private static String texto(String raw, int max) {
        if (raw == null) return null;
        if (raw.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Filtro textual inválido na consulta de reservas.");
        }
        if (raw.isBlank()) return null;
        String value = raw.strip();
        if (value.length() > max) {
            throw new IllegalArgumentException("Filtro textual inválido na consulta de reservas.");
        }
        return value.replaceAll("\\s+", " ");
    }

    private static String opcao(String value, String fallback, Set<String> permitidos) {
        String result = value == null || value.isBlank() ? fallback : value.strip().toUpperCase(Locale.ROOT);
        if (!permitidos.contains(result)) throw new IllegalArgumentException("Filtro não permitido na consulta de reservas.");
        return result;
    }

    private static boolean dataValida(LocalDate date) {
        return date == null || (date.getYear() >= 1900 && date.getYear() <= 2100);
    }

    private static void validarResposta(Integer agencia, ReservaConsultaFiltros request, ReservaConsultaPagina p) {
        if (p == null || !ReservaConsultaPagina.SCHEMA.equals(p.getSchema())
                || !agencia.equals(p.getCodgAgencia()) || p.getReservas() == null
                || !("OK".equals(p.getStatus()) || "SEM_RESULTADO".equals(p.getStatus()))
                || p.getCriterioContagem() == null || p.getCriterioContagem().isBlank()
                || p.getPage() != request.getPage() || p.getSize() != request.getSize()
                || p.getTotalReservas() < 0 || p.getTotalLocalizadores() < 0
                || p.getTotalLocalizadores() > p.getTotalReservas()
                || p.getReservas().size() > p.getSize() || p.getReservas().size() > p.getTotalReservas()) {
            throw respostaInvalida();
        }
        long paginas = p.getTotalReservas() / p.getSize() + (p.getTotalReservas() % p.getSize() == 0 ? 0 : 1);
        if (paginas > Integer.MAX_VALUE || p.getTotalPages() != paginas
                || p.isHasNext() != (p.getPage() + 1L < paginas)
                || ("OK".equals(p.getStatus()) == p.getReservas().isEmpty())) throw respostaInvalida();
        try {
            ReservaConsultaFiltros applied = normalizar(p.getFiltrosAplicados());
            if (!filtrosEquivalentes(request, applied)) throw respostaInvalida();
        } catch (IllegalArgumentException ex) {
            throw respostaInvalida();
        }
        Set<Integer> ids = new HashSet<>();
        Set<String> localizadores = new HashSet<>();
        for (ReservaAereaRecenteItem item : p.getReservas()) {
            if (item == null || item.getReservaId() == null || item.getReservaId() <= 0
                    || !ids.add(item.getReservaId())
                    || request.getReservaId() != null && !request.getReservaId().equals(item.getReservaId())) {
                throw respostaInvalida();
            }
            if (item.getLocalizador() != null && !item.getLocalizador().isBlank()) {
                localizadores.add(item.getLocalizador().strip().toUpperCase(Locale.ROOT));
            }
        }
        if (localizadores.size() > p.getTotalLocalizadores()
                || p.getPage() == 0 && p.getTotalReservas() <= p.getSize()
                && localizadores.size() != p.getTotalLocalizadores()) throw respostaInvalida();
        long offset = (long) p.getPage() * p.getSize();
        long esperados = offset >= p.getTotalReservas() ? 0 : Math.min(p.getSize(), p.getTotalReservas() - offset);
        if (p.getReservas().size() != esperados) throw respostaInvalida();
    }

    private static boolean filtrosEquivalentes(ReservaConsultaFiltros a, ReservaConsultaFiltros b) {
        return iguais(a.getNomePassageiro(), b.getNomePassageiro()) && iguais(a.getOrigem(), b.getOrigem())
                && iguais(a.getDestino(), b.getDestino()) && iguais(a.getCompanhia(), b.getCompanhia())
                && iguais(a.getLocalizador(), b.getLocalizador()) && Objects.equals(a.getReservaId(), b.getReservaId())
                && Objects.equals(a.getSituacao(), b.getSituacao()) && Objects.equals(a.getTipoData(), b.getTipoData())
                && Objects.equals(a.getInicio(), b.getInicio()) && Objects.equals(a.getFim(), b.getFim())
                && Objects.equals(a.getPage(), b.getPage()) && Objects.equals(a.getSize(), b.getSize());
    }

    private static boolean iguais(String a, String b) {
        return a == null ? b == null : b != null && a.equalsIgnoreCase(b);
    }

    private static ServiceIndisponivelException respostaInvalida() {
        return new ServiceIndisponivelException("A consulta de reservas não retornou dados válidos. Tente novamente; não foi possível concluir a busca.");
    }
}
