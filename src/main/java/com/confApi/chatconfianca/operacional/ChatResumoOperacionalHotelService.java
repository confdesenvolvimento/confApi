package com.confApi.chatconfianca.operacional;

import com.confApi.chatconfianca.dto.response.SessaoChatResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import static com.confApi.chatconfianca.operacional.ChatResumoOperacionalFonte.Status.*;

/** Uses explicit validated session data, never a request-local SecurityContext or an LLM. */
@Service
public class ChatResumoOperacionalHotelService {
    private static final String FONTE = "CADASTRO_MANAGER";
    private static final String COBERTURA = "Somente registros do Manager com situação cadastral confirmada ou modificada e sem data de cancelamento. "
            + "Não inclui reservas exclusivas do Wooba e não houve revalidação com o fornecedor. Datas civis de entrada/saída, sem horário de chegada confirmado.";
    private final ChatResumoOperacionalHotelClient client;
    private final boolean enabled;
    private final Clock clock;

    @Autowired public ChatResumoOperacionalHotelService(ChatResumoOperacionalHotelClient client,
            @Value("${chat-confianca.resumo-operacional.hotel-enabled:false}") boolean enabled) {
        this(client, enabled, Clock.systemUTC());
    }
    ChatResumoOperacionalHotelService(ChatResumoOperacionalHotelClient client, boolean enabled, Clock clock) {
        this.client = client; this.enabled = enabled; this.clock = clock;
    }

    public ChatResumoOperacionalFonte consultarEntradas(SessaoChatResponse sessao, LocalDate inicio, LocalDate fim) {
        return consultar(sessao, inicio, fim, "ENTRADA", "ENTRADAS_HOTEL", "Entradas de hotel");
    }
    public ChatResumoOperacionalFonte consultarSaidas(SessaoChatResponse sessao, LocalDate inicio, LocalDate fim) {
        return consultar(sessao, inicio, fim, "SAIDA", "SAIDAS_HOTEL", "Saídas de hotel");
    }

    private ChatResumoOperacionalFonte consultar(SessaoChatResponse sessao, LocalDate inicio, LocalDate fim,
            String tipo, String codigo, String titulo) {
        if (!enabled) return new ChatResumoOperacionalFonte(codigo, titulo, INDISPONIVEL, "NAO_CONFIRMADA",
                "Consulta operacional de hotel não habilitada. Isso não significa ausência de entradas ou saídas.", null, List.of(), false);
        if (!sessaoValida(sessao)) return semPermissao(codigo, titulo);
        if (inicio == null || fim == null || inicio.getYear() < 1900 || fim.getYear() > 2100
                || inicio.isAfter(fim) || ChronoUnit.DAYS.between(inicio, fim) > 6)
            return ChatResumoOperacionalFonte.indisponivel(codigo, titulo);
        var q = new ChatResumoOperacionalHotelDocumento.Consulta();
        q.setAgencia(sessao.getAgencia().getCodgAgencia()); q.setUnidade(sessao.getUnidade().getCodgUnidade());
        q.setTipoData(tipo); q.setInicio(inicio.toString()); q.setFim(fim.toString());
        try {
            var doc = client.consultar(q); validar(doc, q);
            var itens = new ArrayList<ChatResumoOperacionalFonte.Item>();
            for (var r : doc.getReservas()) {
                LocalDate data = data("ENTRADA".equals(tipo) ? r.getEntrada() : r.getSaida());
                String hotel = texto(r.getHotel(), 120, "Hotel não informado"), cidade = texto(r.getCidade(), 80, "cidade não informada");
                String localizador = texto(r.getLocalizador(), 85, null);
                String descricao = "Localizador: " + (localizador == null ? "não informado" : localizador)
                        + ". Situação cadastral: " + (r.getStatus() == 1 ? "confirmada" : "modificada")
                        + ". Consulte a reserva de hotel no portal para conferir hóspedes e detalhes.";
                itens.add(new ChatResumoOperacionalFonte.Item("hotel:" + tipo.toLowerCase(java.util.Locale.ROOT) + ":" + r.getId(),
                        hotel + " — " + cidade, descricao, data, null, r.getId().longValue(), localizador, "HOTEL"));
            }
            // A full bounded page proves its count ONLY inside the declared Manager coverage.
            Long total = doc.isMaisResultados() ? null : Long.valueOf(doc.getReservas().size());
            String cobertura = COBERTURA + (doc.isMaisResultados()
                    ? " Exibidos os primeiros dez registros; há mais resultados e o total não foi apurado." : " Consulta concluída dentro dessa cobertura.");
            return new ChatResumoOperacionalFonte(codigo, titulo, INCOMPLETO, FONTE, cobertura, total, itens, doc.isMaisResultados());
        } catch (ChatResumoOperacionalHotelClient.ConsultaException ex) {
            return ex.status() == 401 || ex.status() == 403 ? semPermissao(codigo, titulo)
                    : ChatResumoOperacionalFonte.indisponivel(codigo, titulo);
        } catch (Exception ex) {
            return ChatResumoOperacionalFonte.indisponivel(codigo, titulo);
        }
    }

    private void validar(ChatResumoOperacionalHotelDocumento d, ChatResumoOperacionalHotelDocumento.Consulta q) {
        if (d == null || !ChatResumoOperacionalHotelDocumento.CONTRATO.equals(d.getContrato()) || !"MANAGER".equals(d.getCobertura())
                || !Objects.equals(q.getAgencia(), d.getAgencia()) || !Objects.equals(q.getUnidade(), d.getUnidade())
                || !q.getTipoData().equals(d.getTipoData()) || !q.getInicio().equals(d.getInicio()) || !q.getFim().equals(d.getFim())
                || d.getReservas() == null || d.getReservas().size() > 10 || d.isMaisResultados() && d.getReservas().size() != 10)
            throw new IllegalStateException();
        Instant now = clock.instant(), consultado = Instant.parse(d.getConsultadoEm());
        if (consultado.isAfter(now.plusSeconds(30)) || Duration.between(consultado, now).compareTo(Duration.ofMinutes(2)) > 0)
            throw new IllegalStateException();
        var ids = new HashSet<Integer>(); LocalDate anterior = null;
        LocalDate inicio = data(q.getInicio()), fim = data(q.getFim());
        for (var r : d.getReservas()) {
            if (r == null || r.getId() == null || r.getId() <= 0 || !ids.add(r.getId())
                    || !q.getAgencia().equals(r.getAgencia()) || !q.getUnidade().equals(r.getUnidade())
                    || !Integer.valueOf(1).equals(r.getStatus()) && !Integer.valueOf(4).equals(r.getStatus()) || r.getDataCancelamento() != null)
                throw new IllegalStateException();
            LocalDate atual = data("ENTRADA".equals(q.getTipoData()) ? r.getEntrada() : r.getSaida());
            // Manager sorts original datetimes; different times can share the projected civil date.
            if (atual.isBefore(inicio) || atual.isAfter(fim) || anterior != null && atual.isBefore(anterior)) throw new IllegalStateException();
            anterior = atual;
        }
    }

    private static boolean sessaoValida(SessaoChatResponse s) {
        return s != null && s.getUsuario() != null && positivo(s.getUsuario().getCodgUsuario())
                && Boolean.TRUE.equals(s.getUsuario().getAtivoChat()) && s.getAgencia() != null && positivo(s.getAgencia().getCodgAgencia())
                && Boolean.TRUE.equals(s.getAgencia().getAtivoChat()) && s.getUnidade() != null && positivo(s.getUnidade().getCodgUnidade())
                && Boolean.TRUE.equals(s.getUnidade().getAtivoChat())
                && Objects.equals(s.getAgencia().getCodgUnidade(), s.getUnidade().getCodgUnidade())
                && (s.getUsuario().getCodgAgencia() == null || Objects.equals(s.getUsuario().getCodgAgencia(), s.getAgencia().getCodgAgencia()));
    }
    private static boolean positivo(Integer id) { return id != null && id > 0; }
    private static LocalDate data(String value) {
        if (value == null || !value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new IllegalArgumentException();
        return LocalDate.parse(value);
    }
    private static String texto(String value, int max, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String limpo = value.replaceAll("[\\p{Cntrl}<>\\u202A-\\u202E\\u2066-\\u2069]", " ").strip();
        return limpo.isEmpty() ? fallback : limpo.substring(0, Math.min(max, limpo.length()));
    }
    private static ChatResumoOperacionalFonte semPermissao(String codigo, String titulo) {
        return new ChatResumoOperacionalFonte(codigo, titulo, SEM_PERMISSAO, "NAO_CONFIRMADA",
                "Não foi possível confirmar a permissão para esta consulta de hotel. Isso não significa ausência de entradas ou saídas.", null, List.of(), false);
    }
}
