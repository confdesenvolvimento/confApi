package com.confApi.db.confManager.rodoviario.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegistrarRecebimentoRodoviarioRequest {

    private String chaveIdempotencia;

    @JsonAlias("codgFormaPagamento")
    private Integer codgFormaPagto;

    @JsonAlias("valorPagamento")
    private BigDecimal valor;

    private LocalDate dataRecebimento;
    private String codgTransacao;
}