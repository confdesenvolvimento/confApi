package com.confApi.hoteis;

import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class StatusReservaHotelFornecedor {
    private StatusReservaHotelFornecedor() { }

    static int codigo(String status) {
        String valor = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        switch (valor) {
            case "reserved": case "confirmed": return 1;
            case "cancelled": case "canceled": return 2;
            case "requestdenied": case "rejected": return 3;
            case "modified": return 4;
            default: throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "O fornecedor retornou um status de hotel não reconhecido; o status salvo foi preservado.");
        }
    }

    static String canonico(int status) {
        switch (status) {
            case 1: return "Reserved";
            case 2: return "Cancelled";
            case 3: return "RequestDenied";
            case 4: return "Modified";
            default: throw new IllegalArgumentException("Status inválido");
        }
    }
}
