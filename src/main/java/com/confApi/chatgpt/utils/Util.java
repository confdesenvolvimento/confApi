package com.confApi.chatgpt.utils;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Util implements Serializable {

    public static String converterDateParaString(Date data) {
        if (data == null) {
            return null; // Retorna null se a data for nula
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(data);
    }
}
