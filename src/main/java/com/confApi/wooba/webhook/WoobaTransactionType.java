package com.confApi.wooba.webhook;

import java.util.Arrays;
import java.util.Optional;

public enum WoobaTransactionType {
    UNDEFINED(0, "Undefined"),
    AIR_RESERVATION(1, "AirReservation"),
    HOTEL(2, "Hotel"),
    CAR(3, "Car"),
    INSURANCE(4, "Insurance"),
    SERVICES(5, "Services"),
    BUS(6, "Bus"),
    CRUISE(7, "Cruise"),
    FILE(8, "File"),
    OTA(9, "OTA"),
    SERVICE_TAX(11, "ServiceTax"),
    BASKET(12, "Basket"),
    CORPORATE(13, "Corporate"),
    CHIP(16, "Chip"),
    AIR_TICKET(100, "AirTicket"),
    EMD(1000, "Emd");

    private final int code;
    private final String description;

    WoobaTransactionType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static Optional<WoobaTransactionType> fromCode(Integer code) {
        if (code == null) {
            return Optional.empty();
        }

        return Arrays.stream(values())
                .filter(type -> type.code == code)
                .findFirst();
    }

    public boolean isAirReservation() {
        return this == AIR_RESERVATION;
    }

    public boolean isAir() {
        return this == AIR_RESERVATION || this == AIR_TICKET;
    }
}
