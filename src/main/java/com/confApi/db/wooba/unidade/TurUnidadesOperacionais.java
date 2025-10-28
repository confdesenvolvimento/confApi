package com.confApi.db.wooba.unidade;

import lombok.Data;

import java.io.Serializable;

@Data
public class TurUnidadesOperacionais implements Serializable {

    private Integer id;
    private String nomeUnidade;
    private String iderp;
    private Integer situacao;
}
