package com.confApi.chatgpt.tools;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ToolRouter {
    public Map<String,Object> execute(String name, Map<String,Object> args) {
        return switch (name) {
            case "search_flights" -> buildFlightResponse(args);
            case "search_hotels" -> buildHotelResponse(args);
            default -> Map.of("status","ERROR","message","tool not found");
        };
    }

    private Map<String, Object> buildFlightResponse(Map<String, Object> args) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("tipo", "aereo");
        resp.put("status", "OK");
        resp.put("origem", args.get("origem"));
        resp.put("destino", args.get("destino"));
        resp.put("dataIda", args.get("dataIda"));
        if (args.get("dataVolta") != null) {
            resp.put("dataVolta", args.get("dataVolta"));
        }
        resp.put("qtdADT", args.get("adt") == null ? 1 : args.get("adt"));
        resp.put("qtdCHD", 0);
        resp.put("qtdINF", 0);
        if (args.get("tipoConsulta") != null) {
            resp.put("tipoConsulta", args.get("tipoConsulta"));
        }
        return resp;
    }

    private Map<String, Object> buildHotelResponse(Map<String, Object> args) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("tipo", "hotel");
        resp.put("status", "OK");
        resp.put("statusHotel", "OK");
        resp.put("destino", args.get("destino"));
        resp.put("destinoId", args.get("destinoId"));
        resp.put("checkin", args.get("checkin"));
        resp.put("checkout", args.get("checkout"));
        resp.put("diarias", args.get("diarias"));
        resp.put("quartos", args.get("quartos"));
        resp.put("totalHospedes", args.get("totalHospedes"));
        resp.put("dataEntrada", args.get("checkin"));
        resp.put("dataSaida", args.get("checkout"));
        resp.put("quantidadeQuartos", quantidadeQuartos(args.get("quartos")));
        resp.put("nomeCidade", args.get("destino"));
        resp.put("nomePais", "Brasil");
        resp.put("quartoPesquisa", quartoPesquisaLegado(args.get("quartos")));
        return resp;
    }

    private int quantidadeQuartos(Object quartos) {
        if (quartos instanceof List<?> list && !list.isEmpty()) {
            return list.size();
        }
        return 1;
    }

    private List<Map<String, Object>> quartoPesquisaLegado(Object quartos) {
        if (!(quartos instanceof List<?> list) || list.isEmpty()) {
            return List.of(Map.of(
                    "id", 1,
                    "nomeQuartoPesquisa", "Quarto 1",
                    "qtdQuartos", 1,
                    "qtdAdultos", 1,
                    "qtdCriancas", 0,
                    "idadeCriancas", List.of()
            ));
        }

        java.util.ArrayList<Map<String, Object>> result = new java.util.ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            Map<?, ?> quarto = item instanceof Map<?, ?> map ? map : Map.of();
            result.add(Map.of(
                    "id", i + 1,
                    "nomeQuartoPesquisa", "Quarto " + (i + 1),
                    "qtdQuartos", 1,
                    "qtdAdultos", inteiroOuPadrao(quarto.get("adultos"), 1),
                    "qtdCriancas", inteiroOuPadrao(quarto.get("criancas"), 0),
                    "idadeCriancas", quarto.get("idadesCriancas") == null ? List.of() : quarto.get("idadesCriancas")
            ));
        }
        return result;
    }

    private int inteiroOuPadrao(Object value, int padrao) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return padrao;
            }
        }
        return padrao;
    }
}
