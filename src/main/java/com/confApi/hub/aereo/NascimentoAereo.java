package com.confApi.hub.aereo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/** Mesma data civil na pré-reserva, no envio ao HUB e na gravação no Manager. */
public final class NascimentoAereo {
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private NascimentoAereo() {}

    public static LocalDate parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String data = value.trim();
        return LocalDate.parse(data, data.contains("/") ? BR : DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
