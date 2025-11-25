package com.confApi.db.confManager.hotel;


public class HotelService {


    public static String formatarDistancia(double distanciaKm) {
        if (distanciaKm >= 1) {
            return String.format("%.1f KM", distanciaKm);
        } else {
            int distanciaMetros = (int) (distanciaKm * 1000);
            return distanciaMetros + " Metros";
        }
    }
}
