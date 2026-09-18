package com.confApi.cacheHotel;

import com.confApi.chatgpt.dto.ChatActionDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PacoteMelhorOfertaService {
    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PacoteMelhorOfertaClient client;

    public PacoteMelhorOfertaService(PacoteMelhorOfertaClient client) {
        this.client = client;
    }

    public Map<String, Object> consultar(Map<String, Object> argumentos) {
        Map<String, Object> args = argumentos == null ? Map.of() : argumentos;
        PacoteMelhorOfertaRequest request = montarRequest(args);
        List<PacoteMelhorOfertaDTO> retorno = client.consultar(request);
        List<PacoteMelhorOfertaDTO> ofertas = new ArrayList<>(
                retorno == null ? List.of() : retorno);
        ofertas.removeIf(item -> item == null || item.getValorTotal() == null
                || item.getValorTotal() <= 0);
        ofertas.sort(Comparator.comparingDouble(PacoteMelhorOfertaDTO::getValorTotal));
        int limite = request.getLimite();
        if (ofertas.size() > limite) {
            ofertas = new ArrayList<>(ofertas.subList(0, limite));
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("schema", "chat.melhor-oferta-pacote.v1");
        resultado.put("tipo", "melhor_oferta_pacote");
        resultado.put("status", ofertas.isEmpty() ? "SEM_DADOS" : "OK");
        resultado.put("origem", request.getOrigem());
        resultado.put("destinosConsultados", request.getDestinos());
        resultado.put("dataIdaInicio", request.getDataIdaInicio());
        resultado.put("dataIdaFim", request.getDataIdaFim());
        resultado.put("duracaoMinimaNoites", request.getDuracaoMinimaNoites());
        resultado.put("duracaoMaximaNoites", request.getDuracaoMaximaNoites());
        resultado.put("adultos", request.getAdultos());
        resultado.put("quartos", request.getQuartos());
        resultado.put("ofertas", ofertas);
        resultado.put("mensagem", montarMensagem(request, ofertas));
        resultado.put("actions", criarAcoes(ofertas));
        return resultado;
    }

    private PacoteMelhorOfertaRequest montarRequest(Map<String, Object> args) {
        PacoteMelhorOfertaRequest request = new PacoteMelhorOfertaRequest();
        request.setOrigem(iata(texto(args.get("origem")), "origem"));
        request.setDestinos(expandirDestino(iata(texto(args.get("destino")), "destino")));
        aplicarPeriodo(args, request);

        Integer duracaoNoites = inteiroOpcional(args.get("duracaoNoites"), 1, 30);
        Integer duracaoDias = inteiroOpcional(args.get("duracaoDias"), 2, 31);
        int noites = duracaoNoites != null
                ? duracaoNoites
                : duracaoDias != null ? duracaoDias - 1 : 5;
        request.setDuracaoMinimaNoites(noites);
        request.setDuracaoMaximaNoites(noites);
        request.setAdultos(inteiro(args.get("adultos"), 2, 1, 9));
        request.setQuartos(inteiro(args.get("quartos"), 1, 1, 4));
        request.setLimite(inteiro(args.get("limite"), 3, 1, 10));
        return request;
    }

    private void aplicarPeriodo(Map<String, Object> args, PacoteMelhorOfertaRequest request) {
        String mes = texto(args.get("mesIda"));
        if (mes != null) {
            try {
                YearMonth periodo = YearMonth.parse(mes);
                request.setDataIdaInicio(periodo.atDay(1));
                request.setDataIdaFim(periodo.atEndOfMonth());
                return;
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("mesIda deve estar no formato YYYY-MM.");
            }
        }

        LocalDate inicio = data(args.get("dataIdaInicio"), "dataIdaInicio");
        LocalDate fim = data(args.get("dataIdaFim"), "dataIdaFim");
        if (inicio == null) {
            inicio = LocalDate.now();
        }
        if (fim == null) {
            fim = inicio.plusMonths(12);
        }
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException(
                    "dataIdaFim nao pode ser anterior a dataIdaInicio.");
        }
        request.setDataIdaInicio(inicio);
        request.setDataIdaFim(fim);
    }

    private List<String> expandirDestino(String destino) {
        return switch (destino) {
            case "RIO" -> List.of("GIG", "SDU");
            case "SAO" -> List.of("GRU", "CGH", "VCP");
            default -> List.of(destino);
        };
    }

    private String montarMensagem(
            PacoteMelhorOfertaRequest request,
            List<PacoteMelhorOfertaDTO> ofertas
    ) {
        if (ofertas.isEmpty()) {
            return "Nao encontrei pacote no cache para " + request.getOrigem()
                    + " e " + String.join("/", request.getDestinos())
                    + " no periodo informado.";
        }
        PacoteMelhorOfertaDTO melhor = ofertas.get(0);
        int noites = melhor.getQuantidadeNoites() == null
                ? request.getDuracaoMinimaNoites()
                : melhor.getQuantidadeNoites();
        String hotel = melhor.getHotelEconomico() == null
                || melhor.getHotelEconomico().getNomeHotel() == null
                ? "hotel economico"
                : melhor.getHotelEconomico().getNomeHotel();
        return "Menor pacote encontrado no cache: " + melhor.getOrigem() + " -> "
                + melhor.getDestino() + ", ida em " + formatarData(melhor.getDataIda())
                + " e volta em " + formatarData(melhor.getDataVolta()) + ", "
                + noites + " noites (" + (noites + 1) + " dias), "
                + request.getAdultos() + " adultos em " + request.getQuartos()
                + " quarto, " + hotel + ", total "
                + formatarBrl(melhor.getValorTotal())
                + ". Tarifa e disponibilidade devem ser revalidadas na pesquisa.";
    }

    private List<ChatActionDTO> criarAcoes(List<PacoteMelhorOfertaDTO> ofertas) {
        List<ChatActionDTO> actions = new ArrayList<>();
        for (int i = 0; i < ofertas.size(); i++) {
            PacoteMelhorOfertaDTO oferta = ofertas.get(i);
            if (oferta.getDataIda() == null || oferta.getDataVolta() == null) {
                continue;
            }
            String localizador = "?origem=" + encode(oferta.getOrigem())
                    + "&destino=" + encode(oferta.getDestino())
                    + "&dataIda=" + dataIso(oferta.getDataIda())
                    + "&dataVolta=" + dataIso(oferta.getDataVolta())
                    + "&qtdADT=" + valorOuPadrao(oferta.getQuantidadeAdultos(), 2)
                    + "&qtdCHD=0&qtdINF=0"
                    + "&quantidadeNoites=" + valorOuPadrao(oferta.getQuantidadeNoites(), 1)
                    + "&nomeCidade=" + encode(oferta.getNomeCidade())
                    + "&nomeEstado=" + encode(oferta.getNomeEstado())
                    + "&nomePais=" + encode(oferta.getNomePais())
                    + "&hotelOutraCidade=" + Boolean.TRUE.equals(oferta.getHotelOutraCidade());
            actions.add(new ChatActionDTO(
                    "pesquisar_pacotes",
                    i == 0 ? "Pesquisar este pacote" : "Pesquisar pacote " + (i + 1),
                    formatarBrl(oferta.getValorTotal()) + " encontrado no cache",
                    localizador,
                    false,
                    false,
                    false,
                    "Pesquise e revalide o pacote de " + oferta.getOrigem() + " para "
                            + oferta.getDestino() + " com ida em " + dataIso(oferta.getDataIda())
                            + " e volta em " + dataIso(oferta.getDataVolta()) + "."));
        }
        return actions;
    }

    private String iata(String valor, String campo) {
        String normalizado = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
        if (!normalizado.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Informe o codigo IATA de 3 letras para " + campo + ".");
        }
        return normalizado;
    }

    private LocalDate data(Object valor, String campo) {
        String texto = texto(valor);
        if (texto == null) {
            return null;
        }
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(campo + " deve estar no formato YYYY-MM-DD.");
        }
    }

    private int inteiro(Object valor, int padrao, int minimo, int maximo) {
        Integer numero = inteiroOpcional(valor, minimo, maximo);
        return numero == null ? padrao : numero;
    }

    private Integer inteiroOpcional(Object valor, int minimo, int maximo) {
        if (valor == null || valor.toString().isBlank()) {
            return null;
        }
        try {
            int numero = valor instanceof Number n
                    ? n.intValue()
                    : Integer.parseInt(valor.toString());
            if (numero < minimo || numero > maximo) {
                throw new IllegalArgumentException(
                        "O valor deve estar entre " + minimo + " e " + maximo + ".");
            }
            return numero;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("O valor deve ser um numero inteiro.");
        }
    }

    private String texto(Object valor) {
        return valor == null || valor.toString().isBlank()
                ? null
                : valor.toString().trim();
    }

    private String formatarBrl(Double valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(
                valor == null ? BigDecimal.ZERO : BigDecimal.valueOf(valor));
    }

    private String formatarData(java.util.Date valor) {
        return valor == null ? "data nao informada" : dataLocal(valor).format(DATA_BR);
    }

    private String dataIso(java.util.Date valor) {
        return dataLocal(valor).toString();
    }

    private LocalDate dataLocal(java.util.Date valor) {
        return Instant.ofEpochMilli(valor.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    private int valorOuPadrao(Integer valor, int padrao) {
        return valor == null || valor <= 0 ? padrao : valor;
    }

    private String encode(String valor) {
        return URLEncoder.encode(valor == null ? "" : valor, StandardCharsets.UTF_8);
    }
}
