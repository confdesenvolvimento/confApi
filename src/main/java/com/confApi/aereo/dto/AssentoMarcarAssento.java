package com.confApi.aereo.dto;

import com.confApi.hub.aereo.dto.Aeroporto;
import lombok.Data;

@Data
public class AssentoMarcarAssento {
    private String assentoCodigo;
    private String cabineDeServico;
    private Aeroporto destino;
    private String moeda;
    private Integer numeroQualificadorAssento;
    private Aeroporto origem;
    private Boolean originalReserva;
    private Object statusAssento;
    private Integer tipoDeAssento;
    private String tipoDeAssentoDescricao;
    private String trechoID;
    private Integer valor;
}
