package com.confApi.aereo.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.util.List;
@Data
public class ReservarResponse {
    @JsonDeserialize(using = ExceptionDetailDeserializer.class)
    private ExceptionDetail exception;
    private List<Reserva> reservas;

}
