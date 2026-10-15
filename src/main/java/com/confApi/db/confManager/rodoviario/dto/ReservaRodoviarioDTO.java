package com.confApi.db.confManager.rodoviario.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class ReservaRodoviarioDTO {
    private Integer codgReservaRodoviario;
    private Integer codgUsuarioCriacao;
    private LocalDateTime dataCriacao;
    private String localizador;
    private Integer status;
    private Integer codgUsuarioCancelamento;
    private Integer codgSistema;
    private String descMotivoCancelamento;
    private Integer codgAgencia;
    private LocalDateTime dataEmissao;
    private LocalDateTime dataCancelamento;
    private Boolean excluido;
}
