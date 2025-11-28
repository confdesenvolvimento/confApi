package com.confApi.hub.aereo;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ConsultarLocalizadorResponseHub {
    private Object exception;
    private List<ReservaHub> reservas = new ArrayList<>();
}
