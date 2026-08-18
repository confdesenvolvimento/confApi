package com.confApi.cacheHotel;

import com.confApi.chatgpt.dto.ChatActionDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class MelhoresTarifasAereasIdaVoltaService {
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final String ICONE_ROTA = "\u2708";
    private static final String ICONE_MELHOR = "\u2605";
    private static final String ICONE_OUTRA = "\u21C4";
    private static final String ICONE_IDA = "\u2197";
    private static final String ICONE_VOLTA = "\u2199";
    private static final String ICONE_ECONOMIA = "\u2713";
    private static final String SETA_ROTA = "\u2192";
    private static final String SEPARADOR = " \u00B7 ";

    private final MelhoresTarifasAereasIdaVoltaClient client;

    public MelhoresTarifasAereasIdaVoltaService(
            MelhoresTarifasAereasIdaVoltaClient client) {
        this.client = client;
    }

    public Map<String, Object> consultar(Map<String, Object> argumentos) {
        Map<String, Object> args = argumentos == null ? Map.of() : argumentos;
        MelhoresTarifasAereasIdaVoltaRequest request = montarRequest(args);
        String politica = normalizarPolitica(texto(args.get("politicaCompanhia")));
        String modo = normalizarModo(texto(args.get("modoResposta")));
        int limite = inteiro(args.get("limiteAlternativas"), 5, 1, 10);

        MelhoresTarifasAereasIdaVoltaResponse response = client.consultar(request);
        if (response == null) {
            throw new IllegalStateException("O cache nao retornou uma resposta.");
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("schema", "chat.melhores-tarifas-aereas-ida-volta.v1");
        resultado.put("tipo", "melhores_tarifas_aereas_ida_volta");
        resultado.put("status", response.getStatus());
        resultado.put("moeda", "BRL");
        resultado.put("origem", request.getOrigem());
        resultado.put("destino", request.getDestino());
        resultado.put("cabine", request.getCabine());
        resultado.put("politicaCompanhia", politica);
        resultado.put("modoResposta", modo);
        resultado.put("quantidadeAplicada", limite);
        copiarPeriodoAplicado(resultado, response);
        copiarRegrasAplicadas(resultado, request, response);
        resultado.put("melhorGeral", response.getMelhorGeral());
        resultado.put("melhorMesmaCompanhia", response.getMelhorMesmaCompanhia());
        resultado.put("melhorCompanhiasDiferentes", response.getMelhorCompanhiasDiferentes());
        resultado.put("alternativasMesmaCompanhia",
                lista(response.getAlternativasMesmaCompanhia()));
        resultado.put("alternativasCompanhiasDiferentes",
                lista(response.getAlternativasCompanhiasDiferentes()));
        List<CombinacaoTarifaAereaIdaVoltaDTO> combinacoesExibidas =
                "OK".equalsIgnoreCase(response.getStatus())
                        ? combinacoesParaExibicao(response, politica, modo, limite)
                        : List.of();
        resultado.put("mensagem", montarMensagem(
                request, response, politica, modo, combinacoesExibidas));

        List<ChatActionDTO> actions = criarAcoesPesquisa(
                request, combinacoesExibidas);
        actions.addAll(criarAcoesRefinamento(request, response, politica, modo, limite));
        resultado.put("actions", actions);
        return resultado;
    }

    private MelhoresTarifasAereasIdaVoltaRequest montarRequest(Map<String, Object> args) {
        MelhoresTarifasAereasIdaVoltaRequest request =
                new MelhoresTarifasAereasIdaVoltaRequest();
        request.setOrigem(normalizarIata(texto(args.get("origem")), "origem"));
        request.setDestino(normalizarIata(texto(args.get("destino")), "destino"));
        request.setCabine(normalizarCabine(texto(args.get("cabine"))));
        request.setLimiteAlternativas(inteiro(args.get("limiteAlternativas"), 5, 1, 10));
        request.setDuracaoMinimaDias(inteiroOpcional(
                args.get("duracaoMinimaDias"), "duracaoMinimaDias", 1, 365));
        request.setDuracaoMaximaDias(inteiroOpcional(
                args.get("duracaoMaximaDias"), "duracaoMaximaDias", 1, 365));
        if (request.getDuracaoMinimaDias() != null
                && request.getDuracaoMaximaDias() != null
                && request.getDuracaoMinimaDias() > request.getDuracaoMaximaDias()) {
            throw new IllegalArgumentException(
                    "A duracao minima nao pode ser maior que a duracao maxima.");
        }
        aplicarPeriodo(args, request, true);
        aplicarPeriodo(args, request, false);
        validarPeriodo(request.getDataIdaInicio(), request.getDataIdaFim(), "ida");
        validarPeriodo(request.getDataVoltaInicio(), request.getDataVoltaFim(), "volta");
        return request;
    }

    private void aplicarPeriodo(Map<String, Object> args,
                                MelhoresTarifasAereasIdaVoltaRequest request,
                                boolean ida) {
        String sufixo = ida ? "Ida" : "Volta";
        String mes = texto(args.get("mes" + sufixo));
        LocalDate inicio;
        LocalDate fim;
        if (mes != null) {
            try {
                YearMonth yearMonth = YearMonth.parse(mes);
                inicio = yearMonth.atDay(1);
                fim = yearMonth.atEndOfMonth();
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException(
                        "O mes de " + sufixo.toLowerCase(Locale.ROOT)
                                + " deve estar no formato YYYY-MM.");
            }
        } else {
            String exata = texto(args.get("data" + sufixo));
            if (exata != null) {
                inicio = data(exata, "data" + sufixo);
                fim = inicio;
            } else {
                Object valorInicio = primeiro(args,
                        ida ? "dataIdaInicio" : "dataVoltaInicio",
                        ida ? "dataInicioIda" : "dataInicioVolta");
                Object valorFim = primeiro(args,
                        ida ? "dataIdaFim" : "dataVoltaFim",
                        ida ? "dataFimIda" : "dataFimVolta");
                inicio = data(valorInicio, ida ? "dataIdaInicio" : "dataVoltaInicio");
                fim = data(valorFim, ida ? "dataIdaFim" : "dataVoltaFim");
            }
        }
        if (ida) {
            request.setDataIdaInicio(inicio);
            request.setDataIdaFim(fim);
        } else {
            request.setDataVoltaInicio(inicio);
            request.setDataVoltaFim(fim);
        }
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim, String trecho) {
        if (inicio == null && fim != null) {
            throw new IllegalArgumentException(
                    "Informe o inicio do periodo de " + trecho + ".");
        }
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new IllegalArgumentException(
                    "O fim do periodo de " + trecho + " nao pode ser anterior ao inicio.");
        }
    }

    private String montarMensagem(MelhoresTarifasAereasIdaVoltaRequest request,
                                  MelhoresTarifasAereasIdaVoltaResponse response,
                                  String politica,
                                  String modo,
                                  List<CombinacaoTarifaAereaIdaVoltaDTO> combinacoes) {
        if (!"OK".equalsIgnoreCase(response.getStatus())) {
            return mensagemSemDados(request, response.getStatus());
        }
        if ("alternativas".equals(modo)) {
            return montarAlternativas(request, response, politica, combinacoes);
        }
        return montarResumo(request, response, politica, combinacoes);
    }

    private String montarResumo(MelhoresTarifasAereasIdaVoltaRequest request,
                                MelhoresTarifasAereasIdaVoltaResponse response,
                                String politica,
                                List<CombinacaoTarifaAereaIdaVoltaDTO> combinacoes) {
        if (combinacoes.isEmpty()) {
            String criterioCompanhia = switch (politica) {
                case "mesma" -> " com a mesma companhia";
                case "diferentes" -> " com companhias diferentes";
                default -> "";
            };
            return "N\u00E3o encontrei uma combina\u00E7\u00E3o de ida e volta de "
                    + request.getOrigem() + " para " + request.getDestino()
                    + criterioCompanhia + " nos crit\u00E9rios informados.";
        }

        String cabineComum = cabineCanonicaComum(combinacoes);
        StringBuilder texto = cabecalho(request, cabineComum);
        adicionarBlocos(texto, combinacoes, cabineComum == null);
        if ("comparar".equals(politica)) {
            if (!combinacaoValida(response.getMelhorCompanhiasDiferentes())) {
                texto.append("\n\n").append(ICONE_OUTRA)
                        .append(" CATEGORIA N\u00C3O DISPON\u00CDVEL\n")
                        .append("N\u00E3o encontrei uma combina\u00E7\u00E3o classificada com companhias diferentes nos registros dispon\u00EDveis.");
            }
            if (!combinacaoValida(response.getMelhorMesmaCompanhia())) {
                texto.append("\n\n").append(ICONE_OUTRA)
                        .append(" CATEGORIA N\u00C3O DISPON\u00CDVEL\n")
                        .append("N\u00E3o encontrei uma combina\u00E7\u00E3o classificada com a mesma companhia nos registros dispon\u00EDveis.");
            }
        }
        adicionarEconomia(texto, combinacoes);
        return texto.toString();
    }

    private String montarAlternativas(MelhoresTarifasAereasIdaVoltaRequest request,
                                      MelhoresTarifasAereasIdaVoltaResponse response,
                                      String politica,
                                      List<CombinacaoTarifaAereaIdaVoltaDTO> combinacoes) {
        if (combinacoes.isEmpty()) {
            return montarResumo(request, response, politica, combinacoes);
        }
        String cabineComum = cabineCanonicaComum(combinacoes);
        StringBuilder texto = cabecalho(request, cabineComum);
        adicionarBlocos(texto, combinacoes, cabineComum == null);
        adicionarEconomia(texto, combinacoes);
        return texto.toString();
    }

    private StringBuilder cabecalho(MelhoresTarifasAereasIdaVoltaRequest request,
                                    String cabineComum) {
        StringBuilder texto = new StringBuilder(ICONE_ROTA).append(' ')
                .append(request.getOrigem()).append(' ')
                .append(SETA_ROTA).append(' ').append(request.getDestino())
                .append("\nIda e volta").append(SEPARADOR).append("1 adulto");
        if (cabineComum != null) {
            texto.append(SEPARADOR).append(nomeCabine(cabineComum));
        }
        return texto;
    }

    private void adicionarBlocos(StringBuilder texto,
                                 List<CombinacaoTarifaAereaIdaVoltaDTO> combinacoes,
                                 boolean mostrarCabineNoBloco) {
        for (int indice = 0; indice < combinacoes.size(); indice++) {
            CombinacaoTarifaAereaIdaVoltaDTO item = combinacoes.get(indice);
            texto.append("\n\n").append(tituloBloco(indice)).append('\n')
                    .append(descrever(item, mostrarCabineNoBloco));
        }
    }

    private String cabineCanonicaComum(
            List<CombinacaoTarifaAereaIdaVoltaDTO> combinacoes) {
        String comum = null;
        for (CombinacaoTarifaAereaIdaVoltaDTO item : combinacoes) {
            String ida = codigoCabine(item.getIda());
            String volta = codigoCabine(item.getVolta());
            if (ida == null || !ida.equals(volta)) {
                return null;
            }
            if (comum == null) {
                comum = ida;
            } else if (!comum.equals(ida)) {
                return null;
            }
        }
        return comum;
    }

    private String codigoCabine(MelhorTarifaAereaDTO trecho) {
        if (trecho == null || trecho.getCabine() == null) {
            return null;
        }
        String cabine = trecho.getCabine().trim().toUpperCase(Locale.ROOT);
        return cabine.matches("[YWCF]") ? cabine : null;
    }

    private String tituloBloco(int indice) {
        if (indice == 0) {
            return ICONE_MELHOR + " MENOR PRE\u00C7O";
        }
        if (indice == 1) {
            return ICONE_OUTRA + " OUTRA OP\u00C7\u00C3O";
        }
        return ICONE_OUTRA + " OP\u00C7\u00C3O " + (indice + 1);
    }

    private String descrever(CombinacaoTarifaAereaIdaVoltaDTO item,
                             boolean mostrarCabine) {
        MelhorTarifaAereaDTO ida = item.getIda();
        MelhorTarifaAereaDTO volta = item.getVolta();
        String categoria = categoriaExibicao(item) + SEPARADOR + iatasExibicao(item)
                + (mostrarCabine ? SEPARADOR + nomeCabine(ida) : "");
        return categoria
                + "\nTotal: " + formatarBrl(item.getTotal())
                + "\n" + ICONE_IDA + " Ida: " + ida.getData().format(DATA_BR)
                + SEPARADOR + companhia(ida) + SEPARADOR + formatarBrl(ida.getTotal())
                + "\n" + ICONE_VOLTA + " Volta: " + volta.getData().format(DATA_BR)
                + SEPARADOR + companhia(volta) + SEPARADOR + formatarBrl(volta.getTotal())
                + "\nPerman\u00EAncia: " + item.getDuracaoDias() + " dias";
    }

    private void adicionarEconomia(StringBuilder texto,
                                   List<CombinacaoTarifaAereaIdaVoltaDTO> combinacoes) {
        if (combinacoes.size() < 2) {
            return;
        }
        CombinacaoTarifaAereaIdaVoltaDTO menor = combinacoes.get(0);
        CombinacaoTarifaAereaIdaVoltaDTO outra = combinacoes.get(1);
        BigDecimal diferenca = outra.getTotal().subtract(menor.getTotal());
        texto.append("\n\n").append(ICONE_ECONOMIA).append(' ');
        if (diferenca.signum() == 0) {
            texto.append("As duas primeiras op\u00E7\u00F5es t\u00EAm o mesmo total.");
            return;
        }
        texto.append(descricaoOpcao(menor, true)).append(" custa ")
                .append(formatarBrl(diferenca)).append(" menos que ")
                .append(descricaoOpcao(outra, false)).append('.');
    }

    private String descricaoOpcao(CombinacaoTarifaAereaIdaVoltaDTO item,
                                  boolean inicioFrase) {
        String sujeito;
        if (!companhiasClassificadas(item)) {
            sujeito = inicioFrase ? "A menor op\u00E7\u00E3o" : "a outra op\u00E7\u00E3o";
        } else {
            sujeito = (inicioFrase ? "A" : "a") + " op\u00E7\u00E3o "
                    + (item.isMesmaCompanhia()
                    ? "com a mesma companhia"
                    : "com companhias diferentes");
        }
        return sujeito + " (" + item.getIda().getData().format(DIA_MES)
                + "\u2013" + item.getVolta().getData().format(DIA_MES) + ")";
    }

    private String categoriaExibicao(CombinacaoTarifaAereaIdaVoltaDTO item) {
        String categoria = rotuloCompanhia(item);
        return Character.toUpperCase(categoria.charAt(0)) + categoria.substring(1);
    }

    private String iatasExibicao(CombinacaoTarifaAereaIdaVoltaDTO item) {
        String ida = companhia(item.getIda());
        String volta = companhia(item.getVolta());
        return item.isMesmaCompanhia() && ida.equals(volta)
                ? ida
                : ida + " / " + volta;
    }

    private String rotuloCompanhia(CombinacaoTarifaAereaIdaVoltaDTO item) {
        if (companhiasClassificadas(item)) {
            return item.isMesmaCompanhia()
                    ? "mesma companhia"
                    : "companhias diferentes";
        }
        return "op\u00E7\u00E3o sem classifica\u00E7\u00E3o completa de companhia";
    }

    private boolean companhiasClassificadas(CombinacaoTarifaAereaIdaVoltaDTO item) {
        return iataValido(item.getIda().getIataCia())
                && iataValido(item.getVolta().getIataCia());
    }

    private List<ChatActionDTO> criarAcoesPesquisa(
            MelhoresTarifasAereasIdaVoltaRequest request,
            List<CombinacaoTarifaAereaIdaVoltaDTO> combinacoes) {
        Map<String, ChatActionDTO> unicas = new LinkedHashMap<>();
        for (int indice = 0; indice < combinacoes.size(); indice++) {
            CombinacaoTarifaAereaIdaVoltaDTO item = combinacoes.get(indice);
            if (!combinacaoValida(item)) {
                continue;
            }
            MelhorTarifaAereaDTO ida = item.getIda();
            MelhorTarifaAereaDTO volta = item.getVolta();
            String cabine = Objects.equals(ida.getCabine(), volta.getCabine())
                    ? ida.getCabine()
                    : null;
            String cabinePrompt = cabine == null
                    ? ""
                    : " na cabine " + nomeCabine(cabine);
            String localizador = "?origem=" + request.getOrigem()
                    + "&destino=" + request.getDestino()
                    + "&dataIda=" + ida.getData()
                    + "&dataVolta=" + volta.getData()
                    + "&qtdADT=1&qtdCHD=0&qtdINF=0"
                    + (cabine == null ? "" : "&cabine=" + cabine);
            if (unicas.containsKey(localizador)) {
                continue;
            }
            unicas.put(localizador, new ChatActionDTO(
                    "pesquisar_voos",
                    labelPesquisa(indice, item),
                    rotuloCompanhia(item) + ": " + formatarBrl(item.getTotal())
                            + " de total combinado",
                    localizador,
                    false,
                    false,
                    false,
                    "Pesquise voos de " + request.getOrigem() + " para "
                            + request.getDestino() + " com ida em " + ida.getData()
                            + " e volta em " + volta.getData() + cabinePrompt
                            + " para 1 adulto."));
        }
        return new ArrayList<>(unicas.values());
    }

    private String labelPesquisa(int indice, CombinacaoTarifaAereaIdaVoltaDTO item) {
        String prefixo = switch (indice) {
            case 0 -> "Menor pre\u00E7o";
            case 1 -> "Outra op\u00E7\u00E3o";
            default -> "Op\u00E7\u00E3o " + (indice + 1);
        };
        return prefixo + SEPARADOR
                + item.getIda().getData().format(DIA_MES)
                + "\u2013" + item.getVolta().getData().format(DIA_MES);
    }

    private List<ChatActionDTO> criarAcoesRefinamento(
            MelhoresTarifasAereasIdaVoltaRequest request,
            MelhoresTarifasAereasIdaVoltaResponse response,
            String politica,
            String modo,
            int limite) {
        List<ChatActionDTO> actions = new ArrayList<>();
        String filtros = filtrosPrompt(request, response, limite);
        boolean alternativas = "alternativas".equals(modo);
        boolean comparar = "comparar".equals(politica);
        if (!alternativas) {
            actions.add(acao("ver_alternativas_tarifas_ida_volta", "Ver outras datas",
                    "Mostre as " + limite + " melhores alternativas de ida e volta de "
                            + request.getOrigem() + " para " + request.getDestino()
                            + filtros + "."));
        }
        if (alternativas && !"mesma".equals(politica)) {
            actions.add(acao("ver_mesma_companhia_tarifas_ida_volta", "Somente mesma companhia",
                    "Mostre as " + limite + " melhores alternativas com a mesma companhia, ida e volta de "
                            + request.getOrigem() + " para " + request.getDestino()
                            + filtros + "."));
        }
        if (alternativas && !"diferentes".equals(politica)) {
            actions.add(acao("ver_companhias_diferentes_tarifas_ida_volta", "Companhias diferentes",
                    "Mostre as " + limite + " melhores alternativas com companhias diferentes, ida e volta de "
                            + request.getOrigem() + " para " + request.getDestino()
                            + filtros + "."));
        }
        if (!comparar) {
            actions.add(acao("comparar_companhias_tarifas_ida_volta", "Comparar companhias",
                    "Compare a menor combinacao com a mesma companhia e com companhias diferentes, ida e volta de "
                            + request.getOrigem() + " para " + request.getDestino()
                            + filtros + "."));
        }
        return actions;
    }

    private ChatActionDTO acao(String codigo, String label, String prompt) {
        return new ChatActionDTO(codigo, label, "Refinar esta consulta", null,
                false, false, false, prompt);
    }

    private String filtrosPrompt(MelhoresTarifasAereasIdaVoltaRequest request,
                                 MelhoresTarifasAereasIdaVoltaResponse response,
                                 int limite) {
        LocalDate idaInicio = periodo(response, true, true, request.getDataIdaInicio());
        LocalDate idaFim = periodo(response, true, false, request.getDataIdaFim());
        LocalDate voltaInicio = periodo(response, false, true, request.getDataVoltaInicio());
        LocalDate voltaFim = periodo(response, false, false, request.getDataVoltaFim());
        Integer minima = response.getRegras() == null
                ? request.getDuracaoMinimaDias()
                : response.getRegras().getDuracaoMinimaDias();
        Integer maxima = response.getRegras() == null
                ? request.getDuracaoMaximaDias()
                : response.getRegras().getDuracaoMaximaDias();
        StringBuilder filtro = new StringBuilder();
        if (request.getCabine() != null) {
            filtro.append(" na cabine ").append(nomeCabine(request.getCabine()));
        }
        if (idaInicio != null && idaFim != null) {
            filtro.append(" com ida entre ").append(idaInicio).append(" e ").append(idaFim);
        }
        if (voltaInicio != null && voltaFim != null) {
            filtro.append(" e volta entre ").append(voltaInicio).append(" e ").append(voltaFim);
        }
        if (minima != null && maxima != null) {
            filtro.append(", duracao entre ").append(minima).append(" e ")
                    .append(maxima).append(" dias");
        }
        return filtro.append(", limite ").append(limite)
                .append(", para 1 adulto, em reais").toString();
    }

    private List<CombinacaoTarifaAereaIdaVoltaDTO> combinacoesParaExibicao(
            MelhoresTarifasAereasIdaVoltaResponse response,
            String politica,
            String modo,
            int limite) {
        int maximo = "alternativas".equals(modo)
                ? limite
                : 2;
        return combinacoesPorPolitica(response, politica,
                "alternativas".equals(modo)).stream()
                .filter(this::combinacaoValida)
                .distinct()
                .sorted(comparadorCombinacao())
                .limit(maximo)
                .toList();
    }

    private Comparator<CombinacaoTarifaAereaIdaVoltaDTO> comparadorCombinacao() {
        return Comparator.comparing(CombinacaoTarifaAereaIdaVoltaDTO::getTotal)
                .thenComparing(item -> item.getIda().getData())
                .thenComparing(item -> item.getVolta().getData())
                .thenComparing(item -> companhia(item.getIda()))
                .thenComparing(item -> companhia(item.getVolta()));
    }

    private List<CombinacaoTarifaAereaIdaVoltaDTO> combinacoesPorPolitica(
            MelhoresTarifasAereasIdaVoltaResponse response,
            String politica,
            boolean incluirAlternativas) {
        List<CombinacaoTarifaAereaIdaVoltaDTO> itens = new ArrayList<>();
        if (!"diferentes".equals(politica)) {
            adicionarValida(itens, response.getMelhorMesmaCompanhia());
            if (incluirAlternativas) {
                itens.addAll(lista(response.getAlternativasMesmaCompanhia()));
            }
        }
        if (!"mesma".equals(politica)) {
            adicionarValida(itens, response.getMelhorCompanhiasDiferentes());
            if (incluirAlternativas) {
                itens.addAll(lista(response.getAlternativasCompanhiasDiferentes()));
            }
        }
        if (itens.isEmpty() && "comparar".equals(politica)) {
            adicionarValida(itens, response.getMelhorGeral());
        }
        return itens;
    }

    private void copiarPeriodoAplicado(Map<String, Object> destino,
                                       MelhoresTarifasAereasIdaVoltaResponse response) {
        if (response.getPeriodos() == null) {
            return;
        }
        if (response.getPeriodos().getIda() != null) {
            destino.put("dataIdaInicio", response.getPeriodos().getIda().getInicio());
            destino.put("dataIdaFim", response.getPeriodos().getIda().getFim());
        }
        if (response.getPeriodos().getVolta() != null) {
            destino.put("dataVoltaInicio", response.getPeriodos().getVolta().getInicio());
            destino.put("dataVoltaFim", response.getPeriodos().getVolta().getFim());
        }
    }

    private void copiarRegrasAplicadas(Map<String, Object> destino,
                                       MelhoresTarifasAereasIdaVoltaRequest request,
                                       MelhoresTarifasAereasIdaVoltaResponse response) {
        destino.put("duracaoMinimaDias", response.getRegras() == null
                ? request.getDuracaoMinimaDias()
                : response.getRegras().getDuracaoMinimaDias());
        destino.put("duracaoMaximaDias", response.getRegras() == null
                ? request.getDuracaoMaximaDias()
                : response.getRegras().getDuracaoMaximaDias());
    }

    private LocalDate periodo(MelhoresTarifasAereasIdaVoltaResponse response,
                              boolean ida,
                              boolean inicio,
                              LocalDate fallback) {
        if (response.getPeriodos() == null) {
            return fallback;
        }
        MelhoresTarifasAereasIdaVoltaResponse.Periodo periodo = ida
                ? response.getPeriodos().getIda()
                : response.getPeriodos().getVolta();
        if (periodo == null) {
            return fallback;
        }
        return inicio ? periodo.getInicio() : periodo.getFim();
    }

    private String mensagemSemDados(MelhoresTarifasAereasIdaVoltaRequest request,
                                    String status) {
        String detalhe = switch (status == null ? "" : status.toUpperCase(Locale.ROOT)) {
            case "SEM_DADOS_IDA" -> "na ida";
            case "SEM_DADOS_VOLTA" -> "na volta";
            case "SEM_COMBINACAO_VALIDA" -> "que respeitem a dura\u00E7\u00E3o informada";
            default -> "nos crit\u00E9rios informados";
        };
        return "N\u00E3o encontrei combina\u00E7\u00F5es de ida e volta de " + request.getOrigem()
                + " para " + request.getDestino() + " " + detalhe + ".";
    }

    private boolean combinacaoValida(CombinacaoTarifaAereaIdaVoltaDTO item) {
        return item != null
                && item.getIda() != null
                && item.getVolta() != null
                && item.getIda().getData() != null
                && item.getVolta().getData() != null
                && item.getIda().getTotal() != null
                && item.getIda().getTotal().signum() > 0
                && item.getVolta().getTotal() != null
                && item.getVolta().getTotal().signum() > 0
                && item.getTotal() != null
                && item.getTotal().signum() > 0
                && item.getDuracaoDias() != null;
    }

    private void adicionarValida(List<CombinacaoTarifaAereaIdaVoltaDTO> destino,
                                 CombinacaoTarifaAereaIdaVoltaDTO item) {
        if (combinacaoValida(item)) {
            destino.add(item);
        }
    }

    private List<CombinacaoTarifaAereaIdaVoltaDTO> lista(
            List<CombinacaoTarifaAereaIdaVoltaDTO> valores) {
        return valores == null ? List.of() : valores;
    }

    private String companhia(MelhorTarifaAereaDTO trecho) {
        return iataValido(trecho.getIataCia()) ? trecho.getIataCia() : "N/I";
    }

    private boolean iataValido(String valor) {
        return valor != null && valor.trim().toUpperCase(Locale.ROOT).matches("[A-Z0-9]{2,3}");
    }

    private String nomeCabine(MelhorTarifaAereaDTO trecho) {
        if (trecho.getCabine() != null && !trecho.getCabine().isBlank()) {
            return nomeCabine(trecho.getCabine());
        }
        return trecho.getNomeCabine() == null || trecho.getNomeCabine().isBlank()
                ? nomeCabine((String) null)
                : trecho.getNomeCabine();
    }

    private String nomeCabine(String cabine) {
        return switch (cabine == null ? "Y" : cabine) {
            case "W" -> "Econ\u00F4mica Premium";
            case "C" -> "Executiva";
            case "F" -> "Primeira Classe";
            default -> "Econ\u00F4mica";
        };
    }

    private String formatarBrl(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(
                valor == null ? BigDecimal.ZERO : valor);
    }

    private String normalizarIata(String valor, String campo) {
        String iata = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
        if (!iata.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Informe o codigo IATA de 3 letras para " + campo + ".");
        }
        return iata;
    }

    private String normalizarCabine(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String normalizada = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z]", "")
                .toUpperCase(Locale.ROOT);
        return switch (normalizada) {
            case "Y", "ECONOMICA", "ECONOMY" -> "Y";
            case "W", "P", "ECONOMICAPREMIUM", "PREMIUMECONOMY" -> "W";
            case "C", "EXECUTIVA", "BUSINESS" -> "C";
            case "F", "PRIMEIRACLASSE", "FIRSTCLASS" -> "F";
            default -> throw new IllegalArgumentException(
                    "Cabine invalida. Use Economica, Economica Premium, Executiva ou Primeira Classe.");
        };
    }

    private String normalizarPolitica(String valor) {
        String normalizado = normalizar(valor);
        return switch (normalizado) {
            case "mesma", "mesma companhia" -> "mesma";
            case "diferentes", "diferente", "companhias diferentes" -> "diferentes";
            default -> "comparar";
        };
    }

    private String normalizarModo(String valor) {
        String normalizado = normalizar(valor);
        return switch (normalizado) {
            case "alternativas", "alternativa", "opcoes" -> "alternativas";
            case "companhias", "companhia", "comparacao" -> "companhias";
            default -> "resumo";
        };
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
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

    private Integer inteiroOpcional(Object valor, String campo, int minimo, int maximo) {
        if (valor == null || valor.toString().isBlank()) {
            return null;
        }
        try {
            int numero = valor instanceof Number
                    ? ((Number) valor).intValue()
                    : Integer.parseInt(valor.toString());
            if (numero < minimo || numero > maximo) {
                throw new IllegalArgumentException(
                        campo + " deve estar entre " + minimo + " e " + maximo + ".");
            }
            return numero;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(campo + " deve ser um numero inteiro.");
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

    private Object primeiro(Map<String, Object> args, String... campos) {
        for (String campo : campos) {
            Object valor = args.get(campo);
            if (valor != null && !valor.toString().isBlank()) {
                return valor;
            }
        }
        return null;
    }

    private String texto(Object valor) {
        return valor == null || valor.toString().isBlank()
                ? null
                : valor.toString().trim();
    }
}
