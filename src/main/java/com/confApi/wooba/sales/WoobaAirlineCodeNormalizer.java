package com.confApi.wooba.sales;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WoobaAirlineCodeNormalizer {

    private WoobaAirlineCodeNormalizer() {
    }

    public static String canonicalIata(String iata) {
        if (iata == null || iata.trim().isEmpty()) {
            return null;
        }

        String normalized = iata.trim().toUpperCase(Locale.ROOT);
        if ("JJ".equals(normalized)) {
            return "LA";
        }
        return normalized;
    }

    public static boolean sameIata(String left, String right) {
        String canonicalLeft = canonicalIata(left);
        String canonicalRight = canonicalIata(right);
        return canonicalLeft != null && canonicalLeft.equals(canonicalRight);
    }

    public static List<String> lookupIatas(String iata) {
        String canonical = canonicalIata(iata);
        if (canonical == null) {
            return List.of();
        }

        List<String> iatas = new ArrayList<>();
        iatas.add(canonical);
        if ("LA".equals(canonical)) {
            iatas.add("JJ");
        }
        return iatas;
    }
}
