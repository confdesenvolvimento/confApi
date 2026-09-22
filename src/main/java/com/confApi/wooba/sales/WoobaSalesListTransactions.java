package com.confApi.wooba.sales;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

final class WoobaSalesListTransactions {
    private WoobaSalesListTransactions() {}

    static List<JsonNode> extract(JsonNode node) {
        if (node == null || !node.isContainerNode()) {
            return List.of();
        }
        if (node.isArray()) {
            List<JsonNode> transactions = new ArrayList<>();
            for (JsonNode item : node) {
                if (item.isObject() && (item.has("TransactionType") || item.has("TransactionTypeDescription"))) {
                    transactions.add(item);
                }
            }
            if (!transactions.isEmpty()) {
                return transactions;
            }
        }
        for (String field : List.of("Transactions", "Sales", "Items", "Data", "Result", "Results")) {
            List<JsonNode> transactions = extract(node.path(field));
            if (!transactions.isEmpty()) {
                return transactions;
            }
        }
        for (JsonNode child : node) {
            List<JsonNode> transactions = extract(child);
            if (!transactions.isEmpty()) {
                return transactions;
            }
        }
        return List.of();
    }

    static boolean matches(JsonNode header, int type, int state) {
        return matchesCode(header, "TransactionType", type, type == 1 ? "AirReservation" : "AirTicket")
                && matchesCode(header, "TransactionState", state, state == 4 ? "Issued" : "Reserved");
    }

    private static boolean matchesCode(JsonNode header, String field, int code, String description) {
        return header.hasNonNull(field) ? header.path(field).asInt(-1) == code
                : description.equalsIgnoreCase(header.path(field + "Description").asText());
    }

    static String uniqueId(JsonNode header) {
        return header.path("UniqueId").asText(header.path("TransactionUniqueId").asText("")).trim();
    }
}
