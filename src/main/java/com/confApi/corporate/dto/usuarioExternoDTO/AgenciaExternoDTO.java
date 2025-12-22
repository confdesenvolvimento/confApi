package com.confApi.corporate.dto.usuarioExternoDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AgenciaExternoDTO {
    @JsonIgnore
    private Integer id;
    private String nome;
    @JsonProperty("codigo_agencia")
    private String codigoErp;
    private String cnpj;
    private Integer status;
    private Integer statusEmissao;
    private String razaoSocial;
    private String logomarca;
}
