package com.confApi.db.confManager.seguro.categoria;

import com.confApi.db.confManager.seguro.cobertura.SeguroCobertura;
import lombok.Data;

@Data
public class SeguroCategoria {

    private Integer codgSeguroCategoria;
    private SeguroCobertura seguroCobertura;
    private Integer idSeguroCategoria;
    private String descSeguroCategoria;
}
