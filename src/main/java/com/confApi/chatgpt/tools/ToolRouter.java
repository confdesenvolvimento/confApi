package com.confApi.chatgpt.tools;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ToolRouter {
    public Map<String,Object> execute(String name, Map<String,Object> args) {
        return switch (name) {
            case "search_flights" -> Map.of(
                    "status","OK",
                    "itinerarios", List.of(
                            Map.of("cia","LA","preco", 1234.56, "saida","GRU","chegada","MIA","data","2025-11-05")
                    )
            );
            default -> Map.of("status","ERROR","message","tool not found");
        };
    }
}