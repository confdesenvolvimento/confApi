package com.confApi.carros;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class LocadoraLogoResolver {

    private static final Map<String, String> CODIGOS_POR_NOME;
    private static final Map<String, String> DOMINIOS_POR_CODIGO;

    static {
        Map<String, String> codigosPorNome = new LinkedHashMap<>();

        codigosPorNome.put("alamo", "AL");
        codigosPorNome.put("avis", "ZI");
        codigosPorNome.put("budget", "ZD");
        codigosPorNome.put("dollar", "ZR");
        codigosPorNome.put("enterprise", "ET");
        codigosPorNome.put("europcar", "EP");
        codigosPorNome.put("hertz", "ZE");
        codigosPorNome.put("keddy", "XX");
        codigosPorNome.put("national", "ZL");
        codigosPorNome.put("sixt", "SX");
        codigosPorNome.put("thrifty", "ZT");
        codigosPorNome.put("foco", "FO");
        codigosPorNome.put("localiza", "LL");
        codigosPorNome.put("movida", "MO");
        codigosPorNome.put("unidas", "UN");

        CODIGOS_POR_NOME =
                Collections.unmodifiableMap(codigosPorNome);

        Map<String, String> dominiosPorCodigo = new LinkedHashMap<>();

        dominiosPorCodigo.put("AL", "alamo.com");
        dominiosPorCodigo.put("ZI", "avis.com");
        dominiosPorCodigo.put("ZD", "budget.com");
        dominiosPorCodigo.put("ZR", "dollar.com");
        dominiosPorCodigo.put("ET", "enterprise.com");
        dominiosPorCodigo.put("EP", "europcar.com");
        dominiosPorCodigo.put("ZE", "hertz.com");
        dominiosPorCodigo.put("XX", "keddy.com");
        dominiosPorCodigo.put("ZL", "nationalcar.com");
        dominiosPorCodigo.put("SX", "sixt.com");
        dominiosPorCodigo.put("ZT", "thrifty.com");
        dominiosPorCodigo.put("FO", "focoalugueldecarros.com.br");
        dominiosPorCodigo.put("LL", "localiza.com");
        dominiosPorCodigo.put("MO", "movida.com.br");
        dominiosPorCodigo.put("UN", "unidas.com.br");

        DOMINIOS_POR_CODIGO =
                Collections.unmodifiableMap(dominiosPorCodigo);
    }

    private LocadoraLogoResolver() {
    }

    public static String resolver(
            String codigoIata,
            String nomeLocadora
    ) {
        String codigo =
                resolverCodigo(
                        codigoIata,
                        nomeLocadora
                );

        if (codigo == null) {
            return null;
        }

        String dominio =
                DOMINIOS_POR_CODIGO.get(codigo);

        if (dominio == null || dominio.trim().isEmpty()) {
            return null;
        }

        String dominioCodificado =
                URLEncoder.encode(
                        dominio,
                        StandardCharsets.UTF_8
                );

        return "https://www.google.com/s2/favicons"
                + "?domain="
                + dominioCodificado
                + "&sz=128";
    }

    private static String resolverCodigo(
            String codigoIata,
            String nomeLocadora
    ) {
        if (codigoIata != null
                && !codigoIata.trim().isEmpty()) {

            String codigoTratado =
                    codigoIata
                            .trim()
                            .toUpperCase(Locale.ROOT);

            if (DOMINIOS_POR_CODIGO.containsKey(
                    codigoTratado
            )) {
                return codigoTratado;
            }
        }

        String nomeNormalizado =
                normalizar(nomeLocadora);

        if (nomeNormalizado.isEmpty()) {
            return null;
        }

        for (Map.Entry<String, String> entry
                : CODIGOS_POR_NOME.entrySet()) {

            if (nomeNormalizado.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private static String normalizar(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "";
        }

        return Normalizer
                .normalize(
                        valor,
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }
}
