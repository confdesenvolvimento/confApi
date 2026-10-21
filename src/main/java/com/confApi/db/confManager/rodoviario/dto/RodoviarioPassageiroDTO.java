package com.confApi.db.confManager.rodoviario.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class RodoviarioPassageiroDTO {
    private Integer codgPassageiro;
    private Integer codgRodoviarioTrecho;
    private String nomePassageiro;
    private String sobrenomePassageiro;
    private Integer sexo;
    private Integer tipoPassageiro;
    private String cpf;
    private String celular;
    private String email;
    private LocalDate dataNascimento;
    private Integer codgPassageiroBase;
}
