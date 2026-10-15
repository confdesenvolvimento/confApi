package com.confApi.db.confManager.rodoviario.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class RodoviarioBilheteDTO {
    private Integer codgRodoviarioBilhete;
    private Integer codgRodoviarioPassageiro;
    private Integer codgRodoviarioTrecho;
    private String localizadorBilhete;
    private String numerBilhete;
    private Integer status;
}
