package com.confApi.aereo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.io.Serializable;
@Data
public class ExceptionDetail implements Serializable {

    @JsonProperty("Message")
    @JsonAlias("message")
    private String Message;
}
