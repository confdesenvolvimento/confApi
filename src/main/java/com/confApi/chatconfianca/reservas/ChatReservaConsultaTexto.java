package com.confApi.chatconfianca.reservas;

import com.confApi.chatconfianca.dto.reserva.ReservaAereaRecenteItem;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Respostas factuais montadas exclusivamente a partir da página autorizada do Manager. */
public final class ChatReservaConsultaTexto {
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Set<String> FOCOS = Set.of("LISTA", "CONTAGEM", "CIA", "TRECHOS", "PASSAGEIROS", "BILHETES", "DETALHES");

    private ChatReservaConsultaTexto() { }

    public static String renderizar(ReservaConsultaPagina p, String foco) {
        if (p == null || !ReservaConsultaPagina.SCHEMA.equals(p.getSchema()) || p.getFiltrosAplicados() == null
                || p.getReservas() == null || p.getSize() < 1 || p.getTotalReservas() < 0
                || !("OK".equals(p.getStatus()) || "SEM_RESULTADO".equals(p.getStatus()))) {
            throw new IllegalArgumentException("Página de reservas inválida para apresentação.");
        }
        String tipo = foco == null ? "LISTA" : foco.toUpperCase(Locale.ROOT);
        if (!FOCOS.contains(tipo)) tipo = "LISTA";
        StringBuilder text = new StringBuilder();
        if (p.getTotalReservas() == 0) {
            text.append("Não encontrei reservas com esses filtros na sua agência.\n");
            return text.append(escopo(p.getFiltrosAplicados())).toString();
        }
        text.append("Encontrei ").append(p.getTotalReservas()).append(" reserva(s) e ")
                .append(p.getTotalLocalizadores()).append(" localizador(es) distinto(s) na sua agência.\n")
                .append(escopo(p.getFiltrosAplicados()));
        if (p.getTotalLocalizadores() < p.getTotalReservas()) {
            text.append("\nReservas diferentes podem compartilhar um localizador ou ainda não possuir código.");
        }
        if (p.getReservas().isEmpty()) {
            return text.append("\nEsta página não tem registros. Volte para uma página anterior para visualizar os resultados.").toString();
        }
        long primeiro = (long) p.getPage() * p.getSize() + 1;
        text.append("\nExibindo ").append(primeiro).append(" a ")
                .append(primeiro + p.getReservas().size() - 1).append(" de ").append(p.getTotalReservas())
                .append(" reserva(s), página ").append(p.getPage() + 1).append(" de ").append(p.getTotalPages())
                .append(". A contagem acima considera todos os resultados dos filtros, não só esta página.");
        if ("CONTAGEM".equals(tipo)) {
            text.append("\nSe quiser, peça para listar as reservas ou procurar um passageiro nesses resultados.");
            return text.toString();
        }
        boolean unica = p.getTotalReservas() == 1;
        ReservaConsultaFiltros filtros = p.getFiltrosAplicados();
        boolean ultimaSolicitada = "DETALHES".equals(tipo) && p.getPage() == 0 && p.getSize() == 1
                && filtros.getNomePassageiro() == null && filtros.getLocalizador() == null && filtros.getReservaId() == null;
        int opcao = 0;
        for (ReservaAereaRecenteItem r : p.getReservas()) {
            text.append("\n\n").append(++opcao).append(". ").append(cabecalho(r));
            boolean detalhes = "DETALHES".equals(tipo) || unica && "LISTA".equals(tipo);
            if (detalhes || "LISTA".equals(tipo) || "PASSAGEIROS".equals(tipo)) {
                text.append("\nPassageiro(s): ").append(nomes(r.getPassageiros()));
            }
            if (detalhes || "LISTA".equals(tipo) || "CIA".equals(tipo)) {
                text.append("\nCompanhia(s): ").append(companhias(r));
            }
            if (detalhes || "LISTA".equals(tipo) || "TRECHOS".equals(tipo)) {
                text.append("\nTrecho(s): ").append(voos(r.getVoos()));
            }
            if (detalhes || "BILHETES".equals(tipo)) {
                text.append("\nBilhete(s): ").append(bilhetes(r.getBilhetes()));
            }
            if (detalhes) {
                if (valida(r.getDataEmissao())) text.append("\nEmissão: ").append(HORA.format(r.getDataEmissao()));
                if (valida(r.getDataCriacao())) text.append("\nCriação: ").append(HORA.format(r.getDataCriacao()));
                if (valida(r.getDataLimiteEmissao())) text.append("\nPrazo de emissão cadastrado: ").append(HORA.format(r.getDataLimiteEmissao()));
            }
        }
        if (p.isHasNext()) text.append("\n\nHá mais resultados. Peça a próxima página.");
        if (!unica && !ultimaSolicitada) text.append("\nVocê pode pedir os detalhes da primeira ou segunda opção, informar o nome do passageiro ou o destino, ou usar o localizador. Se houver mais de uma reserva para o passageiro, vou apresentar as opções.");
        return text.toString();
    }

    private static String escopo(ReservaConsultaFiltros f) {
        StringBuilder b = new StringBuilder("Escopo: ")
                .append("EMITIDAS".equals(f.getSituacao()) ? "somente reservas com status emitida, emissão válida e sem cancelamento registrado na reserva" : "reservas em todas as situações")
                .append("; data de ").append("EMISSAO".equals(f.getTipoData()) ? "emissão" : "criação").append("; ");
        if (f.getInicio() == null && f.getFim() == null) b.append("histórico disponível, sem recorte de período");
        else if (f.getInicio() != null && f.getFim() != null) b.append("de ").append(DIA.format(f.getInicio())).append(" a ").append(DIA.format(f.getFim()));
        else if (f.getInicio() != null) b.append("a partir de ").append(DIA.format(f.getInicio()));
        else b.append("até ").append(DIA.format(f.getFim()));
        filtro(b, "passageiro", f.getNomePassageiro());
        filtro(b, "origem", f.getOrigem());
        filtro(b, "destino", f.getDestino());
        filtro(b, "companhia", f.getCompanhia());
        filtro(b, "localizador", f.getLocalizador());
        if (f.getReservaId() != null) b.append("; código da reserva: ").append(f.getReservaId());
        return b.append('.').toString();
    }

    private static void filtro(StringBuilder b, String nome, String value) {
        if (value != null && !value.isBlank()) b.append("; ").append(nome).append(": ").append(seguro(value));
    }

    private static String cabecalho(ReservaAereaRecenteItem r) {
        String localizador = r.getLocalizador() == null || r.getLocalizador().isBlank() ? "ainda não informado" : seguro(r.getLocalizador());
        String status = r.getStatusDescricao() == null || r.getStatusDescricao().isBlank() ? "situação não informada" : seguro(r.getStatusDescricao());
        return "Localizador: " + localizador + " | " + status;
    }

    private static String nomes(List<String> nomes) {
        if (nomes == null) return "não informado(s) no cadastro";
        String value = nomes.stream().filter(n -> n != null && !n.isBlank()).map(ChatReservaConsultaTexto::seguro)
                .distinct().collect(Collectors.joining(", "));
        return value.isEmpty() ? "não informado(s) no cadastro" : value;
    }

    private static String companhias(ReservaAereaRecenteItem r) {
        Set<String> valores = new LinkedHashSet<>();
        if (r.getVoos() != null) for (ReservaAereaRecenteItem.Voo voo : r.getVoos()) {
            if (voo != null) {
                String nome = voo.getCompanhiaNome();
                if ((nome == null || nome.isBlank()) && voo.getCompanhiaIata() != null && r.getCompanhiaIata() != null
                        && voo.getCompanhiaIata().strip().equalsIgnoreCase(r.getCompanhiaIata().strip())) {
                    nome = r.getCompanhiaNome();
                }
                adicionarCompanhia(valores, voo.getCompanhiaIata(), nome);
            }
        }
        if (valores.isEmpty()) adicionarCompanhia(valores, r.getCompanhiaIata(), r.getCompanhiaNome());
        return valores.isEmpty() ? "não informada(s) no cadastro" : String.join(", ", valores);
    }

    private static void adicionarCompanhia(Set<String> valores, String iata, String nome) {
        boolean temIata = iata != null && !iata.isBlank();
        boolean temNome = nome != null && !nome.isBlank();
        if (temNome) valores.add(seguro(nome) + (temIata ? " (" + seguro(iata) + ")" : ""));
        else if (temIata) valores.add(seguro(iata));
    }

    private static String voos(List<ReservaAereaRecenteItem.Voo> voos) {
        if (voos == null || voos.isEmpty()) return "não informado(s) no cadastro";
        String value = voos.stream().filter(v -> v != null).map(v -> {
            String origem = v.getOrigem() == null ? "origem não informada" : seguro(v.getOrigem());
            String destino = v.getDestino() == null ? "destino não informado" : seguro(v.getDestino());
            String companhia = v.getCompanhiaIata() == null ? "" : seguro(v.getCompanhiaIata());
            String numero = v.getNumeroVoo() == null ? "" : seguro(v.getNumeroVoo());
            String voo = (companhia + " " + numero).strip();
            return origem + " → " + destino + (voo.isEmpty() ? "" : " | voo " + voo)
                    + (valida(v.getDataHoraPartida()) ? " | saída " + HORA.format(v.getDataHoraPartida()) : " | saída não informada")
                    + (valida(v.getDataHoraChegada()) ? " | chegada " + HORA.format(v.getDataHoraChegada()) : "");
        }).collect(Collectors.joining("; "));
        return value.isEmpty() ? "não informado(s) no cadastro" : value;
    }

    private static String bilhetes(List<ReservaAereaRecenteItem.Bilhete> bilhetes) {
        if (bilhetes == null || bilhetes.isEmpty()) return "não informado(s) no cadastro; ausência de dados não confirma emissão ou cancelamento";
        String value = bilhetes.stream().filter(b -> b != null).map(b -> {
            String numero = b.getNumero() == null || b.getNumero().isBlank() ? "número não informado" : seguro(b.getNumero());
            return numero + (b.getPassageiroNome() == null ? "" : " — " + seguro(b.getPassageiroNome()))
                    + (valida(b.getDataCancelamento()) ? " (cancelamento cadastrado em " + HORA.format(b.getDataCancelamento()) + ")" : "");
        }).collect(Collectors.joining("; "));
        return value.isEmpty() ? "não informado(s) no cadastro" : value;
    }

    private static boolean valida(LocalDateTime data) {
        return data != null && data.getYear() >= 1900 && data.getYear() <= 2100;
    }

    private static String seguro(String value) {
        if (value == null) return "";
        return value.replaceAll("[\\p{Cntrl}]", " ").replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;")
                .replace("[", "\\[").replace("]", "\\]").replace("*", "\\*").replace("`", "\\`");
    }
}
