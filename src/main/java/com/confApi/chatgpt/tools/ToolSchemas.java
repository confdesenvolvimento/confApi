package com.confApi.chatgpt.tools;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class ToolSchemas {
    public static ToolDefinition searchFlights() {
        Map<String,Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "origem", Map.of("type","string"),
                        "destino", Map.of("type","string"),
                        "dataIda", Map.of("type","string","format","date"),
                        "dataVolta", Map.of("type","string","format","date"),
                        "adt", Map.of("type","integer","minimum",1),
                        "cabine", Map.of(
                                "type", "string",
                                "enum", List.of("Y", "W", "C", "F"),
                                "description", "Cabine Y, W, C ou F quando informada")
                ),
                "required", List.of("origem","destino","dataIda","adt")
        );
        return new ToolDefinition("search_flights",
                "Busca voos no motor OTA da Confiança", schema);
    }

    public static ToolDefinition searchCheapestAirfares() {
        Map<String, Object> properties = Map.of(
                "origem", Map.of(
                        "type", "string",
                        "description", "IATA de origem. Converta a cidade informada para o codigo IATA."),
                "destino", Map.of(
                        "type", "string",
                        "description", "IATA de destino. Converta a cidade informada para o codigo IATA."),
                "cabine", Map.of(
                        "type", "string",
                        "description", "Filtro opcional: Economica (Y), Economica Premium (W), Executiva (C) ou Primeira Classe (F). Se o usuario nao informar cabine, omita este campo para consultar todas as cabines."),
                "mes", Map.of(
                        "type", "string",
                        "pattern", "^[0-9]{4}-[0-9]{2}$",
                        "description", "Mes opcional no formato YYYY-MM."),
                "dataInicio", Map.of("type", "string", "format", "date"),
                "dataFim", Map.of("type", "string", "format", "date"),
                "modoResposta", Map.of(
                        "type", "string",
                        "enum", List.of("resumo", "alternativas", "cabines", "mensal"),
                        "description", "Formato solicitado: resumo geral, melhores datas alternativas, comparacao de cabines ou melhor dia de cada mes."),
                "limiteAlternativas", Map.of(
                        "type", "integer",
                        "minimum", 1,
                        "maximum", 10,
                        "description", "Quantidade de dias a exibir no modo alternativas. Use a quantidade pedida pelo usuario; padrao 5.")
        );
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", properties,
                "required", List.of("origem", "destino")
        );
        return new ToolDefinition(
                "search_cheapest_airfares",
                "Consulta as menores tarifas aereas validas em BRL por dia, mes e cabine. "
                        + "Use quando o usuario perguntar o dia, mes, preco ou cabine mais barata; "
                        + "nao exija uma data exata nem pergunte a preferencia de cabine. "
                        + "Com origem e destino informados, execute imediatamente; sem cabine, consulte todas.",
                schema
        );
    }

    public static ToolDefinition searchCheapestRoundtripAirfares() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("origem", Map.of(
                "type", "string",
                "description", "IATA de origem. Converta a cidade informada para o codigo IATA."));
        properties.put("destino", Map.of(
                "type", "string",
                "description", "IATA de destino. Converta a cidade informada para o codigo IATA."));
        properties.put("cabine", Map.of(
                "type", "string",
                "enum", List.of("Y", "W", "C", "F"),
                "description", "Cabine opcional aplicada aos dois trechos. Sem preferencia, omita."));
        properties.put("mesIda", Map.of(
                "type", "string",
                "pattern", "^[0-9]{4}-[0-9]{2}$"));
        properties.put("mesVolta", Map.of(
                "type", "string",
                "pattern", "^[0-9]{4}-[0-9]{2}$"));
        properties.put("dataIda", Map.of("type", "string", "format", "date"));
        properties.put("dataVolta", Map.of("type", "string", "format", "date"));
        properties.put("dataIdaInicio", Map.of("type", "string", "format", "date"));
        properties.put("dataIdaFim", Map.of("type", "string", "format", "date"));
        properties.put("dataVoltaInicio", Map.of("type", "string", "format", "date"));
        properties.put("dataVoltaFim", Map.of("type", "string", "format", "date"));
        properties.put("duracaoMinimaDias", Map.of(
                "type", "integer",
                "minimum", 1,
                "maximum", 365,
                "description", "Duracao minima opcional. Sem valor, o cache usa 3 dias."));
        properties.put("duracaoMaximaDias", Map.of(
                "type", "integer",
                "minimum", 1,
                "maximum", 365,
                "description", "Duracao maxima opcional. Sem valor, o cache usa 30 dias."));
        properties.put("politicaCompanhia", Map.of(
                "type", "string",
                "enum", List.of("comparar", "mesma", "diferentes"),
                "description", "Compara as categorias, exige a mesma companhia ou exige companhias diferentes. Padrao comparar."));
        properties.put("modoResposta", Map.of(
                "type", "string",
                "enum", List.of("resumo", "alternativas", "companhias"),
                "description", "Formato da resposta. Padrao resumo."));
        properties.put("limiteAlternativas", Map.of(
                "type", "integer",
                "minimum", 1,
                "maximum", 10,
                "description", "Quantidade de combinacoes alternativas. Padrao 5."));

        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", properties,
                "required", List.of("origem", "destino"));
        return new ToolDefinition(
                "search_cheapest_roundtrip_airfares",
                "Consulta no cache os menores totais combinados de ida e volta em BRL. "
                        + "Execute imediatamente quando houver origem e destino, mesmo sem datas. "
                        + "Nao pergunte cabine nem periodo antes da consulta. Diferencie a menor "
                        + "combinacao com a mesma companhia da menor com companhias diferentes.",
                schema);
    }

    public static ToolDefinition searchHotels() {
        Map<String,Object> quartoSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "adultos", Map.of("type", "integer", "minimum", 1),
                        "criancas", Map.of("type", "integer", "minimum", 0),
                        "idadesCriancas", Map.of(
                                "type", "array",
                                "items", Map.of("type", "integer", "minimum", 0)
                        )
                ),
                "required", List.of("adultos", "criancas", "idadesCriancas")
        );

        Map<String,Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "destino", Map.of("type","string"),
                        "destinoId", Map.of("type", List.of("integer", "null")),
                        "checkin", Map.of("type","string","format","date"),
                        "checkout", Map.of("type","string","format","date"),
                        "diarias", Map.of("type","integer","minimum",1),
                        "quartos", Map.of(
                                "type","array",
                                "items", quartoSchema,
                                "minItems", 1
                        ),
                        "totalHospedes", Map.of("type","integer","minimum",1)
                ),
                "required", List.of("destino", "checkin", "checkout", "quartos", "totalHospedes")
        );

        return new ToolDefinition(
                "search_hotels",
                "Monta os parâmetros para pesquisa de hotéis no motor OTA da Confiança",
                schema
        );
    }
}
