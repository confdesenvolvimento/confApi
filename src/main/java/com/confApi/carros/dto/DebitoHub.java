package com.confApi.carros.dto;

import lombok.Data;

@Data
public class DebitoHub {
    private String url;
    private String html;
    private String urlPassageiroDebitoNote;
    private String htmlPassageiroDebitoNote;
    private String tipoNotaDebito;
}
