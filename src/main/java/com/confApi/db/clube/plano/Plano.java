package com.confApi.db.clube.plano;

import com.confApi.db.clube.beneficio.Beneficio;
import lombok.Data;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Data
public class Plano {
    private Integer codgPlano;
    private String nomePlano;
    private Double valorPlano;
    private String descricaoPlano;
    private String tituloBotao;
    private Integer tipoPlano;
    private Integer quantMes;
    private Integer status;
    private Set<Beneficio> beneficios = new HashSet<>();
}
