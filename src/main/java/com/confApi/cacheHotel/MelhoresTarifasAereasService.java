package com.confApi.cacheHotel;

import com.confApi.chatgpt.dto.ChatActionDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MelhoresTarifasAereasService {
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final Locale PT_BR = new Locale("pt", "BR");

    private final MelhoresTarifasAereasClient client;

    public MelhoresTarifasAereasService(MelhoresTarifasAereasClient client) {
        this.client = client;
    }

    public Map<String, Object> consultar(Map<String, Object> argumentos) {
        Map<String, Object> args = argumentos == null ? Map.of() : argumentos;
        MelhoresTarifasAereasRequest request = new MelhoresTarifasAereasRequest();
        request.setOrigem(normalizarIata(texto(args.get("origem")), "origem"));
        request.setDestino(normalizarIata(texto(args.get("destino")), "destino"));
        request.setCabine(normalizarCabine(texto(args.get("cabine"))));
        int quantidade = inteiro(args.get("limiteAlternativas"), 5, 1, 10);
        String modoResposta = normalizarModoResposta(texto(args.get("modoResposta")));
        request.setLimiteAlternativas(quantidade);
        aplicarPeriodo(args, request);

        MelhoresTarifasAereasResponse response = client.consultar(request);
        if (response == null) {
            throw new IllegalStateException("O cache nao retornou uma resposta.");
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("schema", "chat.melhores-tarifas-aereas.v1");
        resultado.put("tipo", "melhores_tarifas_aereas");
        resultado.put("status", response.getStatus());
        resultado.put("moeda", "BRL");
        resultado.put("origem", request.getOrigem());
        resultado.put("destino", request.getDestino());
        resultado.put("cabine", request.getCabine());
        resultado.put("modoResposta", modoResposta);
        resultado.put("quantidadeAplicada", quantidade);
        resultado.put("periodoInicio", response.getPeriodoInicio());
        resultado.put("periodoFim", response.getPeriodoFim());
        resultado.put("melhorGeral", response.getMelhorGeral());
        resultado.put("melhoresPorDia", lista(response.getMelhoresPorDia()));
        resultado.put("melhoresPorCabine", lista(response.getMelhoresPorCabine()));
        resultado.put("melhoresPorMesECabine", lista(response.getMelhoresPorMesECabine()));
        resultado.put("alternativas", lista(response.getAlternativas()));

        List<MelhorTarifaAereaDTO> tarifasParaAcao = tarifasParaAcao(
                modoResposta, response, quantidade);
        List<ChatActionDTO> actions = criarAcoesPesquisa(
                request.getOrigem(),
                request.getDestino(),
                tarifasParaAcao);
        actions.addAll(criarAcoesRefinamento(
                request, response, modoResposta, quantidade));
        resultado.put("actions", actions);
        resultado.put("mensagem", montarMensagem(
                request, response, modoResposta, quantidade));
        return resultado;
    }

    private void aplicarPeriodo(Map<String, Object> args, MelhoresTarifasAereasRequest request) {
        String mesTexto = primeiroTexto(args, "mes", "mesIda");
        if (mesTexto != null) {
            try {
                YearMonth mes = YearMonth.parse(mesTexto);
                request.setDataInicio(mes.atDay(1));
                request.setDataFim(mes.atEndOfMonth());
                return;
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("O mes deve estar no formato YYYY-MM.");
            }
        }
        request.setDataInicio(data(args.get("dataInicio"), "dataInicio"));
        request.setDataFim(data(args.get("dataFim"), "dataFim"));
        if (request.getDataInicio() != null && request.getDataFim() == null) {
            request.setDataFim(request.getDataInicio().plusMonths(12).minusDays(1));
        }
        if (request.getDataInicio() == null && request.getDataFim() != null) {
            throw new IllegalArgumentException("Informe dataInicio quando usar dataFim.");
        }
    }

    private String montarMensagem(MelhoresTarifasAereasRequest request,
                                  MelhoresTarifasAereasResponse response,
                                  String modoResposta,
                                  int quantidade) {
        if (!respostaValida(response)) {
            return montarMensagemResumo(request, response);
        }
        return switch (modoResposta) {
            case "alternativas" -> montarMensagemAlternativas(
                    request, response, quantidade);
            case "cabines" -> montarMensagemCabines(request, response);
            case "mensal" -> montarMensagemMensal(request, response);
            default -> montarMensagemResumo(request, response);
        };
    }

    private String montarMensagemResumo(MelhoresTarifasAereasRequest request,
                                        MelhoresTarifasAereasResponse response) {
        MelhorTarifaAereaDTO melhor = response.getMelhorGeral();
        if (!"OK".equalsIgnoreCase(response.getStatus())
                || melhor == null
                || melhor.getData() == null
                || melhor.getTotal() == null) {
            return "Não encontrei tarifas de " + request.getOrigem() + " para "
                    + request.getDestino() + " nos critérios informados.";
        }

        StringBuilder texto = new StringBuilder();
        texto.append("A menor tarifa de ")
                .append(request.getOrigem()).append(" para ").append(request.getDestino())
                .append(" é em ").append(melhor.getData().format(DATA_BR))
                .append(": ").append(formatarBrl(melhor.getTotal()))
                .append(" para 1 adulto, somente ida")
                .append(", na cabine ").append(nomeCabine(melhor)).append(companhia(melhor))
                .append('.');

        List<MelhorTarifaAereaDTO> porCabine = lista(response.getMelhoresPorCabine());
        if (request.getCabine() == null && porCabine.size() > 1) {
            texto.append("\n\nMelhor opção por cabine:");
            porCabine.forEach(item -> texto.append("\n- ")
                    .append(nomeCabine(item)).append(": ")
                    .append(item.getData().format(DATA_BR)).append(" por ")
                    .append(formatarBrl(item.getTotal())).append(companhia(item)));
        }

        String cabineReferencia = request.getCabine() == null
                ? melhor.getCabine()
                : request.getCabine();
        List<MelhorTarifaAereaDTO> mensais = lista(response.getMelhoresPorMesECabine()).stream()
                .filter(item -> item != null
                        && cabineReferencia != null
                        && cabineReferencia.equalsIgnoreCase(item.getCabine()))
                .toList();
        if (mensais.size() > 1) {
            texto.append("\n\nMelhores dias por mês em ")
                    .append(nomeCabine(melhor)).append(':');
            mensais.forEach(item -> texto.append("\n- ")
                    .append(formatarMes(item)).append(": ")
                    .append(item.getData().format(DATA_BR)).append(" por ")
                    .append(formatarBrl(item.getTotal())));
        }
        return texto.toString();
    }

    private String montarMensagemAlternativas(MelhoresTarifasAereasRequest request,
                                               MelhoresTarifasAereasResponse response,
                                               int quantidade) {
        List<MelhorTarifaAereaDTO> dias = melhoresDias(response).stream()
                .limit(quantidade)
                .toList();
        if (dias.isEmpty()) {
            return montarMensagemResumo(request, response);
        }
        StringBuilder texto = new StringBuilder("As ")
                .append(dias.size())
                .append(dias.size() == 1 ? " data mais barata de " : " datas mais baratas de ")
                .append(request.getOrigem()).append(" para ").append(request.getDestino());
        if (request.getCabine() != null) {
            texto.append(" na cabine ").append(nomeCabine(request.getCabine()));
        }
        texto.append(" são:");
        dias.forEach(item -> texto.append("\n- ")
                .append(item.getData().format(DATA_BR)).append(": ")
                .append(formatarBrl(item.getTotal())).append(" — ")
                .append(nomeCabine(item)).append(companhia(item)));
        texto.append("\n\nValores para 1 adulto, somente ida.");
        return texto.toString();
    }

    private String montarMensagemCabines(MelhoresTarifasAereasRequest request,
                                         MelhoresTarifasAereasResponse response) {
        List<MelhorTarifaAereaDTO> cabines = lista(response.getMelhoresPorCabine()).stream()
                .filter(this::tarifaValida)
                .sorted(comparadorTarifa())
                .toList();
        if (cabines.isEmpty()) {
            return montarMensagemResumo(request, response);
        }
        BigDecimal menor = cabines.get(0).getTotal();
        StringBuilder texto = new StringBuilder("Comparação das menores tarifas por cabine de ")
                .append(request.getOrigem()).append(" para ").append(request.getDestino())
                .append(":");
        cabines.forEach(item -> {
            BigDecimal diferenca = item.getTotal().subtract(menor);
            texto.append("\n- ").append(nomeCabine(item)).append(": ")
                    .append(formatarBrl(item.getTotal())).append(" em ")
                    .append(item.getData().format(DATA_BR)).append(companhia(item));
            if (diferenca.signum() == 0) {
                texto.append(" — menor opção");
            } else {
                texto.append(" — ").append(formatarBrl(diferenca)).append(" a mais (")
                        .append(percentualAcima(diferenca, menor)).append("%)");
            }
        });
        texto.append("\n\nValores para 1 adulto, somente ida.");
        return texto.toString();
    }

    private String montarMensagemMensal(MelhoresTarifasAereasRequest request,
                                        MelhoresTarifasAereasResponse response) {
        List<MelhorTarifaAereaDTO> mensais = melhoresMensais(request, response);
        if (mensais.isEmpty()) {
            return montarMensagemResumo(request, response);
        }
        StringBuilder texto = new StringBuilder("Melhor dia de cada mês de ")
                .append(request.getOrigem()).append(" para ").append(request.getDestino());
        if (request.getCabine() != null) {
            texto.append(" na cabine ").append(nomeCabine(request.getCabine()));
        }
        texto.append(":");
        mensais.forEach(item -> texto.append("\n- ")
                .append(formatarMes(item)).append(": ")
                .append(item.getData().format(DATA_BR)).append(" por ")
                .append(formatarBrl(item.getTotal())).append(" — ")
                .append(nomeCabine(item)).append(companhia(item)));
        texto.append("\n\nValores para 1 adulto, somente ida.");
        return texto.toString();
    }

    private String formatarMes(MelhorTarifaAereaDTO tarifa) {
        try {
            YearMonth mes = tarifa.getMes() == null || tarifa.getMes().isBlank()
                    ? YearMonth.from(tarifa.getData())
                    : YearMonth.parse(tarifa.getMes());
            return mes.getMonth().getDisplayName(TextStyle.FULL, PT_BR)
                    + "/" + mes.getYear();
        } catch (RuntimeException ex) {
            return tarifa.getMes() == null ? "" : tarifa.getMes();
        }
    }

    private List<MelhorTarifaAereaDTO> tarifasParaAcao(
            String modoResposta,
            MelhoresTarifasAereasResponse response,
            int quantidade) {
        List<MelhorTarifaAereaDTO> tarifas = switch (modoResposta) {
            case "alternativas" -> melhoresDias(response).stream()
                    .limit(Math.min(quantidade, 5))
                    .toList();
            case "mensal" -> response.getMelhorGeral() == null
                    ? List.of()
                    : List.of(response.getMelhorGeral());
            default -> lista(response.getMelhoresPorCabine());
        };
        if (tarifas.isEmpty() && response.getMelhorGeral() != null) {
            return List.of(response.getMelhorGeral());
        }
        return tarifas;
    }

    private List<MelhorTarifaAereaDTO> melhoresDias(
            MelhoresTarifasAereasResponse response) {
        List<MelhorTarifaAereaDTO> origem = lista(response.getMelhoresPorDia());
        if (origem.isEmpty()) {
            origem = new ArrayList<>();
            if (response.getMelhorGeral() != null) {
                origem.add(response.getMelhorGeral());
            }
            origem.addAll(lista(response.getAlternativas()));
        }
        Map<LocalDate, MelhorTarifaAereaDTO> porData = new LinkedHashMap<>();
        origem.stream()
                .filter(this::tarifaValida)
                .sorted(comparadorTarifa())
                .forEach(item -> porData.putIfAbsent(item.getData(), item));
        return new ArrayList<>(porData.values());
    }

    private List<MelhorTarifaAereaDTO> melhoresMensais(
            MelhoresTarifasAereasRequest request,
            MelhoresTarifasAereasResponse response) {
        Map<YearMonth, MelhorTarifaAereaDTO> porMes = new LinkedHashMap<>();
        lista(response.getMelhoresPorMesECabine()).stream()
                .filter(this::tarifaValida)
                .filter(item -> request.getCabine() == null
                        || request.getCabine().equalsIgnoreCase(item.getCabine()))
                .sorted(Comparator.comparing((MelhorTarifaAereaDTO item) ->
                                YearMonth.from(item.getData()))
                        .thenComparing(comparadorTarifa()))
                .forEach(item -> porMes.merge(
                        YearMonth.from(item.getData()),
                        item,
                        (atual, candidata) -> comparadorTarifa().compare(atual, candidata) <= 0
                                ? atual
                                : candidata));
        return new ArrayList<>(porMes.values());
    }

    private boolean respostaValida(MelhoresTarifasAereasResponse response) {
        return response != null
                && "OK".equalsIgnoreCase(response.getStatus())
                && tarifaValida(response.getMelhorGeral());
    }

    private boolean tarifaValida(MelhorTarifaAereaDTO tarifa) {
        return tarifa != null
                && tarifa.getData() != null
                && tarifa.getTotal() != null
                && tarifa.getTotal().signum() > 0;
    }

    private Comparator<MelhorTarifaAereaDTO> comparadorTarifa() {
        return Comparator.comparing(MelhorTarifaAereaDTO::getTotal)
                .thenComparing(MelhorTarifaAereaDTO::getData)
                .thenComparing(item -> item.getCabine() == null ? "" : item.getCabine());
    }

    private String percentualAcima(BigDecimal diferenca, BigDecimal base) {
        if (base == null || base.signum() <= 0) {
            return "0";
        }
        return diferenca.multiply(BigDecimal.valueOf(100))
                .divide(base, 1, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private List<ChatActionDTO> criarAcoesPesquisa(String origem,
                                                   String destino,
                                                   List<MelhorTarifaAereaDTO> tarifas) {
        List<ChatActionDTO> actions = new ArrayList<>();
        for (MelhorTarifaAereaDTO tarifa : tarifas) {
            if (tarifa == null || tarifa.getData() == null || tarifa.getCabine() == null) {
                continue;
            }
            String nomeCabine = nomeCabine(tarifa);
            String localizador = "?origem=" + origem
                    + "&destino=" + destino
                    + "&dataIda=" + tarifa.getData()
                    + "&qtdADT=1&qtdCHD=0&qtdINF=0"
                    + "&cabine=" + tarifa.getCabine();
            actions.add(new ChatActionDTO(
                    "pesquisar_voos",
                    "Pesquisar " + nomeCabine + " - " + tarifa.getData().format(DIA_MES),
                    formatarBrl(tarifa.getTotal()) + " em " + tarifa.getData().format(DATA_BR),
                    localizador,
                    false,
                    false,
                    false,
                    "Pesquisar voo de " + origem + " para " + destino
                            + " em " + tarifa.getData() + " na cabine " + nomeCabine));
        }
        return actions;
    }

    private List<ChatActionDTO> criarAcoesRefinamento(
            MelhoresTarifasAereasRequest request,
            MelhoresTarifasAereasResponse response,
            String modoResposta,
            int quantidade) {
        List<ChatActionDTO> actions = new ArrayList<>();
        String filtros = filtrosPrompt(request);
        if (!"alternativas".equals(modoResposta)
                && melhoresDias(response).size() > 1) {
            actions.add(acaoRefinamento(
                    "ver_alternativas_tarifas",
                    "Ver " + quantidade + " melhores datas",
                    "Lista as datas distintas com menor tarifa",
                    "Mostre as " + quantidade + " datas mais baratas de "
                            + request.getOrigem() + " para " + request.getDestino() + filtros + "."));
        }
        if (!"cabines".equals(modoResposta)
                && request.getCabine() == null
                && lista(response.getMelhoresPorCabine()).size() > 1) {
            actions.add(acaoRefinamento(
                    "comparar_cabines_tarifas",
                    "Comparar cabines",
                    "Compara preço e melhor data de cada cabine",
                    "Compare as cabines e as menores tarifas de "
                            + request.getOrigem() + " para " + request.getDestino()
                            + filtrosSemCabine(request) + "."));
        }
        if (!"mensal".equals(modoResposta)
                && melhoresMensais(request, response).size() > 1) {
            actions.add(acaoRefinamento(
                    "ver_tarifas_mensais",
                    "Ver mês a mês",
                    "Mostra o melhor dia disponível em cada mês",
                    "Mostre o dia mais barato de cada mês de "
                            + request.getOrigem() + " para " + request.getDestino() + filtros + "."));
        }
        if (!"resumo".equals(modoResposta)) {
            actions.add(acaoRefinamento(
                    "ver_resumo_tarifas",
                    "Voltar ao resumo",
                    "Mostra novamente a menor tarifa geral",
                    "Mostre o resumo da menor tarifa de "
                            + request.getOrigem() + " para " + request.getDestino() + filtros + "."));
        }
        return actions;
    }

    private ChatActionDTO acaoRefinamento(String codigo,
                                          String label,
                                          String descricao,
                                          String prompt) {
        return new ChatActionDTO(
                codigo,
                label,
                descricao,
                null,
                false,
                false,
                false,
                prompt);
    }

    private String filtrosPrompt(MelhoresTarifasAereasRequest request) {
        StringBuilder filtros = new StringBuilder();
        if (request.getCabine() != null) {
            filtros.append(" na cabine ").append(nomeCabine(request.getCabine()));
        }
        if (request.getDataInicio() != null && request.getDataFim() != null) {
            filtros.append(" entre ").append(request.getDataInicio())
                    .append(" e ").append(request.getDataFim());
        }
        return filtros.append(" para 1 adulto, somente ida, em reais").toString();
    }

    private String filtrosSemCabine(MelhoresTarifasAereasRequest request) {
        if (request.getDataInicio() == null || request.getDataFim() == null) {
            return " para 1 adulto, somente ida, em reais";
        }
        return " entre " + request.getDataInicio() + " e " + request.getDataFim()
                + " para 1 adulto, somente ida, em reais";
    }

    private String companhia(MelhorTarifaAereaDTO tarifa) {
        return tarifa.getIataCia() == null || tarifa.getIataCia().isBlank()
                ? ""
                : " (companhia " + tarifa.getIataCia() + ")";
    }

    private String nomeCabine(MelhorTarifaAereaDTO tarifa) {
        if (tarifa.getNomeCabine() != null && !tarifa.getNomeCabine().isBlank()) {
            return tarifa.getNomeCabine();
        }
        return nomeCabine(tarifa.getCabine());
    }

    private String nomeCabine(String cabine) {
        return switch (cabine == null ? "Y" : cabine) {
            case "W" -> "Econômica Premium";
            case "C" -> "Executiva";
            case "F" -> "Primeira Classe";
            default -> "Econômica";
        };
    }

    private String formatarBrl(BigDecimal valor) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(PT_BR);
        return formato.format(valor == null ? BigDecimal.ZERO : valor);
    }

    private String normalizarIata(String valor, String campo) {
        String iata = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
        if (!iata.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Informe o codigo IATA de 3 letras para " + campo + ".");
        }
        return iata;
    }

    String normalizarCabine(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String normalizada = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z]", "")
                .toUpperCase(Locale.ROOT);
        return switch (normalizada) {
            case "Y", "ECONOMICA", "ECONOMY" -> "Y";
            case "W", "P", "ECONOMICAPREMIUM", "PREMIUMECONOMY",
                    "ECONOMICAPLUSPREMIUM", "ECONOMICAPREMIUMPLUS" -> "W";
            case "C", "EXECUTIVA", "BUSINESS" -> "C";
            case "F", "PRIMEIRACLASSE", "FIRSTCLASS" -> "F";
            default -> throw new IllegalArgumentException(
                    "Cabine invalida. Use Economica, Economica Premium, Executiva ou Primeira Classe.");
        };
    }

    private String normalizarModoResposta(String valor) {
        if (valor == null || valor.isBlank()) {
            return "resumo";
        }
        String normalizado = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
        return switch (normalizado) {
            case "alternativas", "alternativa", "datas", "opcoes" -> "alternativas";
            case "cabines", "cabine", "comparacao" -> "cabines";
            case "mensal", "mes", "meses" -> "mensal";
            default -> "resumo";
        };
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
        if (valor == null) {
            return padrao;
        }
        try {
            int numero = valor instanceof Number
                    ? ((Number) valor).intValue()
                    : Integer.parseInt(valor.toString());
            return Math.max(minimo, Math.min(numero, maximo));
        } catch (NumberFormatException ex) {
            return padrao;
        }
    }

    private String primeiroTexto(Map<String, Object> args, String... campos) {
        for (String campo : campos) {
            String valor = texto(args.get(campo));
            if (valor != null) {
                return valor;
            }
        }
        return null;
    }

    private String texto(Object valor) {
        if (valor == null || valor.toString().isBlank()) {
            return null;
        }
        return valor.toString().trim();
    }

    private List<MelhorTarifaAereaDTO> lista(List<MelhorTarifaAereaDTO> valores) {
        return valores == null ? List.of() : valores;
    }
}
