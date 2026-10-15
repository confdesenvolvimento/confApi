package com.confApi.db.confManager.rodoviario.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
@Data
public class RegistrarRecebimentoRodoviarioRequest {
    private String chaveIdempotencia;
    private Integer codgFormaPagto;
    private BigDecimal valor;
    private LocalDate dataRecebimento;
    private String codgTransacao;
}
