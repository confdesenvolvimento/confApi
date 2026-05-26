package com.confApi.carros.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ErrorResponse {
    @JsonProperty("error")
    private String error;

    @JsonProperty("error_description")
    private String errorDescription;
}
