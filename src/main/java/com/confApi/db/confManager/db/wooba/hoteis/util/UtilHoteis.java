package com.confApi.db.confManager.db.wooba.hoteis.util;

import java.io.Serializable;

public class UtilHoteis implements Serializable {
    public static String getNomeSistemaExibicao(Integer sistemaId) {
        if (sistemaId == null) return "Desconhecido";

        switch (sistemaId) {
            case 11: return "Diretorio";
            case 37: return "B2BReservas";
            case 39: return "OmniBees";
            case 56: return "EHTL";
            case 65: return "Trend_Sophia";
            case 76: return "EzLink";
            case 79: return "Cangoroo";
            case 80: return "TBO";
            case 703: return "BEDSONLINE";
            case 807: return "HotelDO";
            case 812: return "BestBuy";
            case 813: return "Restel";
            case 816: return "PriceTravel";
            default: return "Sistema " + sistemaId;
        }
    }
}


