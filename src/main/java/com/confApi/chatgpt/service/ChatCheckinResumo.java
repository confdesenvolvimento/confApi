package com.confApi.chatgpt.service;

import com.confApi.db.wooba.checkin.dto.Checkin72Horas;
import com.confApi.db.wooba.checkin.dto.CheckinRQ;
import com.confApi.db.wooba.checkin.dto.TrechoCheckin;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;

/** Filters individual flight segments and renders facts without a second model call. */
final class ChatCheckinResumo {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private final ZoneId zona;

    ChatCheckinResumo() { this(ZoneId.systemDefault()); }
    ChatCheckinResumo(ZoneId zona) { this.zona = Objects.requireNonNull(zona); }

    Map<String, Object> montar(List<Checkin72Horas> reservas, CheckinRQ consulta) {
        LocalDate inicio = LocalDate.parse(consulta.getDataForm(), DATA);
        LocalDate fim = LocalDate.parse(consulta.getDataTo(), DATA);
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
                LocalDate data = data(trecho.getData());
                if (data == null) { datasAusentes++; continue; }
                if (data.isBefore(inicio) || data.isAfter(fim)) continue;
                Map<String, Object> voo = new LinkedHashMap<>();
                voo.put("data", DATA.format(data));
                voo.put("hora", texto(trecho.getHora()));
                voo.put("de", texto(trecho.getDe()));
                voo.put("para", texto(trecho.getPara()));
                voo.put("companhia", texto(trecho.getCompanhia()));
                voo.put("voo", texto(trecho.getVoo()));
                String chave = String.join("|", data.toString(), texto(trecho.getHora()),
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
        if (datasAusentes > 0) out.append("\n\nAlguns registros vieram sem data de trecho válida e não foram incluídos. Confirme-os na reserva ou com o atendimento.");
        if (!itens.isEmpty()) out.append("\n\nDatas e horários conforme o cadastro dos voos. A abertura do check-in depende da companhia; nenhum check-in foi realizado por esta consulta.");
        return out.toString();
    }

    private LocalDate data(Date value) {
        return value == null ? null : java.time.Instant.ofEpochMilli(value.getTime()).atZone(zona).toLocalDate();
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
