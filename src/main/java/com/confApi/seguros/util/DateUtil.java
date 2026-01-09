package com.confApi.seguros.util;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public final class DateUtil {

    private DateUtil() {}

    public static LocalDate toLocalDate(Date d) {
        if (d == null) return null;
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }


    public static String extractDateToString(Date date) {
        if (date == null) return null;

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(date);
    }

    public static Date extractDataStringToDate(String req) {
        String dataStr = req; // ex: "12/01/2026"

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            sdf.setLenient(false); // evita datas inválidas tipo 32/13/2026
            return sdf.parse(dataStr);
        } catch (ParseException e) {
            throw new IllegalArgumentException(
                    "DataInicioCobertura inválida: " + dataStr + " (esperado dd/MM/yyyy)", e
            );
        }
    }

}