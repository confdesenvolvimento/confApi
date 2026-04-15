package com.confApi.chatgpt.tools;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
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
            case "search_hotels" -> buildHotelResponse(args);
            default -> Map.of("status","ERROR","message","tool not found");
        };
    }

    private Map<String, Object> buildHotelResponse(Map<String, Object> args) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("tipo", "hotel");
        resp.put("status", "OK");
        resp.put("destino", args.get("destino"));
        resp.put("destinoId", args.get("destinoId"));
        resp.put("checkin", args.get("checkin"));
        resp.put("checkout", args.get("checkout"));
        resp.put("diarias", args.get("diarias"));
        resp.put("quartos", args.get("quartos"));
        resp.put("totalHospedes", args.get("totalHospedes"));
        return resp;
    }
}
