package com.confApi.aereo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
@Data
public class ExceptionDetail implements Serializable {

    @JsonProperty("Message")
    private String Message;
}
