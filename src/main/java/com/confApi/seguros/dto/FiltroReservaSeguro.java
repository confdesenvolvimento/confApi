package com.confApi.seguros.dto;

import lombok.Data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

@Data
public class FiltroReservaSeguro {
    private String localizador;
    private Integer codgUsuario;
    private Integer codgAgencia;
    private Integer codgUnidade;
    private String inicio; // String durante o binding inicial
    private String fim;    // String durante o binding inicial
    private Boolean filtroMinhasReservas;
    private Boolean filtroMinhaAgencia;

    private static final SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);

    public Date getInicioAsDate() {
        return parseDate(this.inicio);
    }

    public Date getFimAsDate() {
        return parseDate(this.fim);
    }

    private Date parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }

        try {
            if (dateString.matches("\\d+")) {
                long timestamp = Long.parseLong(dateString);
                return new Date(timestamp);
            }

            SimpleDateFormat isoFormatter = new SimpleDateFormat("yyyy-MM-dd");
            isoFormatter.setLenient(false);
            return isoFormatter.parse(dateString);

        } catch (ParseException e) {
            throw new IllegalArgumentException("Erro ao converter data: " + dateString, e);
        }
    }

    private static Date ajustarHorario(Date data, int hora, int minuto, int segundo, int milissegundo) {
        if (data == null) {
            throw new IllegalArgumentException("A data não pode ser nula.");
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(data);
        cal.set(Calendar.HOUR_OF_DAY, hora);
        cal.set(Calendar.MINUTE, minuto);
        cal.set(Calendar.SECOND, segundo);
        cal.set(Calendar.MILLISECOND, milissegundo);
        return cal.getTime();
    }
}
