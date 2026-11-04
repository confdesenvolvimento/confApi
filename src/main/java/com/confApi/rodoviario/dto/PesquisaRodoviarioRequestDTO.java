package com.confApi.rodoviario.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.validation.constraints.*;
import java.time.LocalDate;

/**
 * Contrato de pesquisa rodoviaria enviado ao Hub.
 */
@Data
public class PesquisaRodoviarioRequestDTO {
    @NotBlank
    private String sistema = "QueroPassagem";

    @NotNull @Positive
    private Integer origem;

    @NotNull @Positive
    private Integer destino;

    @NotNull
    @FutureOrPresent
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDate dataViagem;

    @AssertTrue(message = "Origem e destino devem ser diferentes.")
    @JsonIgnore
    public boolean isRotaValida() {
        return origem == null || !origem.equals(destino);
    }
}
