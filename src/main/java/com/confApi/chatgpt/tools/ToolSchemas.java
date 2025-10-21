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
}