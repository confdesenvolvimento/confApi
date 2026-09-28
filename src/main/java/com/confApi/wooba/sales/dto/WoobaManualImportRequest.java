package com.confApi.wooba.sales.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public record WoobaManualImportRequest(String localizador,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dataCriacao, String companhia) {
}
