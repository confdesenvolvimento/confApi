package com.confApi.chatgpt.tools;

import java.util.List;
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
                        "adt", Map.of("type","integer","minimum",1)
                ),
                "required", List.of("origem","destino","dataIda","adt")
        );
        return new ToolDefinition("search_flights",
                "Busca voos no motor OTA da Confiança", schema);
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