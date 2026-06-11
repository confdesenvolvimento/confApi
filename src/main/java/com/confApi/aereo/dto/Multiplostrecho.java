package com.confApi.aereo.dto;

import lombok.Data;

import java.util.Date;

@Data
public class Multiplostrecho {
    private Date data;
    private String destino;
    private String origem;
}
