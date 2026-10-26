package com.confApi.chatgpt.service;

import com.confApi.db.wooba.checkin.dto.Checkin72Horas;
import com.confApi.db.wooba.checkin.dto.CheckinRQ;
import com.confApi.db.wooba.checkin.dto.TrechoCheckin;
import java.net.URI;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;

/** Filters individual flight segments and renders facts without a second model call. */
final class ChatCheckinResumo {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private final Clock clock;
    private final ZoneId zona;
    private final ZoneId zonaDados;

    ChatCheckinResumo() { this(Clock.systemDefaultZone()); }
    ChatCheckinResumo(ZoneId zona) { this(Clock.system(zona)); }
    ChatCheckinResumo(Clock clock) { this(clock, clock.getZone()); }
    ChatCheckinResumo(Clock clock, ZoneId zonaDados) {
        this.clock = Objects.requireNonNull(clock);
        this.zona = clock.getZone();
        this.zonaDados = Objects.requireNonNull(zonaDados);
    }

    static ChatCheckinResumo configurado(String referencia, String fonte) {
        ZoneId zona = referencia == null || referencia.isBlank() ? ZoneId.systemDefault() : ZoneId.of(referencia.trim());
        ZoneId zonaFonte = fonte == null || fonte.isBlank() ? zona : ZoneId.of(fonte.trim());
        return new ChatCheckinResumo(Clock.system(zona), zonaFonte);
    }

    static boolean pedidoCheckins(String normalizado) {
        if (normalizado == null || normalizado.isBlank()) return false;
        // Guidance about the feature is not a request to query agency bookings.
        if (normalizado.matches("(?s).*\\b(como|onde)\\s+(faco|fazer|realizar|acesso|acessar|encontro|encontrar|fica|funciona|funcionam|consultar|vejo|posso)\\b.*")) return false;
        if (normalizado.matches("(?s).*\\bcheck[ -]*ins?\\b.*")) return true;
        boolean embarques = normalizado.matches("(?s).*\\b(embarques?|voos?)\\b.*");
        return embarques && normalizado.matches("(?s).*\\b(proximos?|proximas?|hoje)\\b.*");
    }

    CheckinRQ consultaProximos(String idErp) {
        LocalDate hoje = LocalDate.now(clock);
        CheckinRQ consulta = new CheckinRQ(idErp, 2);
        consulta.setDataForm(DATA.format(hoje));
        consulta.setDataTo(DATA.format(hoje.plusDays(3)));
        return consulta;
    }

    Map<String, Object> montar(List<Checkin72Horas> reservas, CheckinRQ consulta) {
        Objects.requireNonNull(consulta, "Consulta de embarques obrigatória.");
        LocalDate inicio = LocalDate.parse(consulta.getDataForm(), DATA);
        LocalDate fim = LocalDate.parse(consulta.getDataTo(), DATA);
        LocalDateTime agora = LocalDateTime.now(clock);
        if (fim.isBefore(inicio) || inicio.isBefore(agora.toLocalDate())
                || fim.isAfter(agora.toLocalDate().plusDays(3))) {
            throw new IllegalArgumentException("Período de próximos embarques inválido.");
        }
        List<Map<String, Object>> itens = new ArrayList<>();
        int datasAusentes = 0;
        for (Checkin72Horas reserva : reservas == null ? Collections.<Checkin72Horas>emptyList() : reservas) {
            if (reserva == null || !Objects.equals(2, reserva.getStatusReserva())) continue;
            List<TrechoCheckin> originais = new ArrayList<>();
            adicionar(originais, reserva.getTrechosIda());
            adicionar(originais, reserva.getTrechosVolta());
            adicionar(originais, reserva.getTrechosMultiplaConexao());
            Map<String, Map<String, Object>> trechos = new LinkedHashMap<>();
            for (TrechoCheckin trecho : originais) {
                if (trecho == null) continue;
                if (encerrado(trecho.getStatusEmbarque())) continue;
                LocalDate data = data(trecho.getData());
                LocalTime hora = hora(trecho.getHora());
                if (data == null || hora == null) { datasAusentes++; continue; }
                if (data.isBefore(inicio) || data.isAfter(fim)) continue;
                if (LocalDateTime.of(data, hora).isBefore(agora)) continue;
                Map<String, Object> voo = new LinkedHashMap<>();
                voo.put("data", DATA.format(data));
                voo.put("hora", DateTimeFormatter.ofPattern("HH:mm").format(hora));
                voo.put("de", texto(trecho.getDe()));
                voo.put("para", texto(trecho.getPara()));
                voo.put("companhia", texto(trecho.getCompanhia()));
                voo.put("voo", texto(trecho.getVoo()));
                String chave = String.join("|", data.toString(), (String) voo.get("hora"),
                        texto(trecho.getDe()), texto(trecho.getPara()), texto(trecho.getCompanhia()), texto(trecho.getVoo()));
                trechos.putIfAbsent(chave, voo);
            }
            // The reservation's date is not a substitute for missing segment dates.
            if (originais.isEmpty()) datasAusentes++;
            if (trechos.isEmpty()) continue;
            List<Map<String, Object>> ordenados = new ArrayList<>(trechos.values());
            ordenados.sort(Comparator.comparing((Map<String, Object> voo) -> LocalDate.parse((String) voo.get("data"), DATA))
                    .thenComparing(voo -> (String) voo.get("hora")));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("localizadorCompanhia", texto(reserva.getLocalizadorCompanhia()));
            item.put("passageiro", texto(reserva.getPassageiro()));
            item.put("companhia", texto(reserva.getCompanhia()));
            item.put("statusReserva", 2);
            item.put("data", ordenados.get(0).get("data"));
            item.put("trechosMultiplaConexao", ordenados);
            item.put("linkCheckin", link(reserva.getLinkCheckin()));
            itens.add(item);
        }
        itens.sort(Comparator.comparing((Map<String, Object> item) -> LocalDate.parse((String) item.get("data"), DATA))
                .thenComparing(item -> (String) item.get("localizadorCompanhia")));
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("statusConsulta", itens.isEmpty() ? "SEM_RESULTADO" : "DADOS_CONSULTADOS");
        resposta.put("dataInicio", DATA.format(inicio));
        resposta.put("dataFim", DATA.format(fim));
        resposta.put("fusoReferencia", zona.getId());
        resposta.put("reservaCheckInIA", itens);
        resposta.put("textoResposta", render(itens, inicio, fim, datasAusentes));
        return resposta;
    }

    private String render(List<Map<String, Object>> itens, LocalDate inicio, LocalDate fim, int datasAusentes) {
        String periodo = DATA.format(inicio) + " a " + DATA.format(fim) + " (inclusive)";
        StringBuilder out = new StringBuilder(itens.isEmpty()
                ? "Não há reservas emitidas com embarques entre " + periodo + " retornadas para a agência deste atendimento."
                : "Embarques da agência entre " + periodo + ":");
        for (Map<String, Object> item : itens) {
            out.append("\n\nLocalizador: ").append(ou((String) item.get("localizadorCompanhia"), "não informado"));
            if (!((String) item.get("passageiro")).isBlank()) out.append(" — Passageiro: ").append(item.get("passageiro"));
            @SuppressWarnings("unchecked") List<Map<String, Object>> trechos = (List<Map<String, Object>>) item.get("trechosMultiplaConexao");
            for (Map<String, Object> voo : trechos) {
                out.append("\n- ").append(voo.get("data"));
                if (!((String) voo.get("hora")).isBlank()) out.append(" às ").append(voo.get("hora"));
                out.append(" — ").append(ou((String) voo.get("de"), "origem não informada"))
                        .append(" → ").append(ou((String) voo.get("para"), "destino não informado"));
                if (!((String) voo.get("companhia")).isBlank()) out.append(" — ").append(voo.get("companhia"));
                if (!((String) voo.get("voo")).isBlank()) out.append(" voo ").append(voo.get("voo"));
            }
            if (!((String) item.get("linkCheckin")).isBlank()) out.append("\nCheck-in: ").append(item.get("linkCheckin"));
        }
        if (datasAusentes > 0) out.append("\n\nAlguns registros vieram sem data ou horário de trecho válido e não foram incluídos. Confirme-os na reserva ou com o atendimento.");
        if (!itens.isEmpty()) out.append("\n\nDatas e horários conforme o cadastro dos voos. Referência da consulta: ")
                .append(zona.getId()).append(". A abertura do check-in depende da companhia; nenhum check-in foi realizado por esta consulta.");
        return out.toString();
    }

    private LocalDate data(Date value) {
        if (value == null) return null;
        LocalDate data = java.time.Instant.ofEpochMilli(value.getTime()).atZone(zonaDados).toLocalDate();
        return data.getYear() < 1900 || data.getYear() > 9999 ? null : data;
    }
    private static LocalTime hora(String value) {
        if (value == null || value.isBlank()) return null;
        String texto = value.trim();
        try {
            if (texto.matches("\\d{3,4}")) {
                int horario = Integer.parseInt(texto);
                return LocalTime.of(horario / 100, horario % 100);
            }
            if (texto.matches("\\d{1,2}:\\d{2}(:\\d{2})?")) {
                String[] partes = texto.split(":");
                return LocalTime.of(Integer.parseInt(partes[0]), Integer.parseInt(partes[1]),
                        partes.length == 3 ? Integer.parseInt(partes[2]) : 0);
            }
        } catch (java.time.DateTimeException | NumberFormatException ignored) { /* invalid provider field */ }
        return null;
    }
    private static boolean encerrado(String value) {
        if (value == null) return false;
        String status = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .trim().toUpperCase(Locale.ROOT);
        return Set.of("CANCELADO", "CANCELADA", "CANCELLED", "CANCELED", "EMBARCADO", "EMBARCADA",
                "DECOLADO", "DECOLADA", "CONCLUIDO", "CONCLUIDA", "REALIZADO", "REALIZADA").contains(status);
    }
    private static void adicionar(List<TrechoCheckin> target, List<TrechoCheckin> source) { if (source != null) target.addAll(source); }
    private static String texto(String value) {
        return value == null ? "" : value.replaceAll("[\\p{Cntrl}]", " ").replace("<", "&lt;").replace(">", "&gt;").trim();
    }
    private static String ou(String value, String fallback) { return value.isBlank() ? fallback : value; }
    private static String link(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            URI uri = URI.create(value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) return "";
            return uri.toASCIIString();
        } catch (IllegalArgumentException ignored) { return ""; }
    }
}
