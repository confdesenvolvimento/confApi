package com.confApi.db.confManager.rodoviario.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class RodoviarioTrechoDTO {
    private Integer codgRodoviarioTrecho;
    private Integer codgReservaRodoviario;
    private String nomeEmpresa;
    private String urlLogo;
    private String categoria;
    private Integer codgCidadeOrigem;
    private Integer codgCidadeDestino;
    private LocalDate dataPartida;
    private LocalDate dataChegada;
    private String horaPartida;
    private String horaChegada;
}
