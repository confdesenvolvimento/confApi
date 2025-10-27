package com.confApi.db.confManager.faturas.dto.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Cabecalho {

    @JsonProperty("numero")
    private Integer numero;
    @JsonProperty("nome")
    private String nome;
    @JsonProperty("bairro")
    private String bairro;
    @JsonProperty("cep")
    private String cep;
    @JsonProperty("cidade")
    private String cidade;
    @JsonProperty("cliente")
    private String cliente;
    @JsonProperty("cnpj")
    private String cnpj;
    @JsonProperty("email")
    private String email;
    @JsonProperty("emissao")
    private String emissao;
    @JsonProperty("endereco")
    private String endereco;
    @JsonProperty("estado")
    private String estado;
    @JsonProperty("fax")
    private String fax;
    @JsonProperty("fone1")
    private String fone1;
    @JsonProperty("fone2")
    private String fone2;
    @JsonProperty("ie")
    private String ie;
    @JsonProperty("pago")
    private Boolean pago;
    @JsonProperty("valor")
    private Double valor;
    @JsonProperty("vencimento")
    private String vencimento;
    @JsonProperty("codest")
    private String codest;
}
