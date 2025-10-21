package com.confApi.db.confManager.chatMemoria.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMemoria {

    private Integer codgMemoria;
    private String intencao;
    private String text;
    private Integer status;
    private String base;


}
