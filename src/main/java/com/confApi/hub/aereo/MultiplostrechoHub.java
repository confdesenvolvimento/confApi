package com.confApi.hub.aereo;

import lombok.Data;

import java.util.Date;

@Data
public class MultiplostrechoHub {
    private Date data;
    private String destino;
    private String origem;
}
