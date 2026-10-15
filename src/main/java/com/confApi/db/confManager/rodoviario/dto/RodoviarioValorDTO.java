package com.confApi.db.confManager.rodoviario.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class RodoviarioValorDTO {
    private Integer codgRodoviarioValor;
    private BigDecimal valorTarifa;
    private BigDecimal valorTarifaNet;
    private BigDecimal valorTaxaEmbarque;
    private BigDecimal valorMkp;
    private BigDecimal percMkp;
    private BigDecimal demaisTaxas;
    private BigDecimal valorSeguro;
    private BigDecimal taxaConveniencia;
    private BigDecimal valorFee;
    private BigDecimal valorTotalNet;
    private BigDecimal valorTotalMkp;
    private Integer flagSeguroIncluido;
    private Integer codgReservaRodoviario;
}
