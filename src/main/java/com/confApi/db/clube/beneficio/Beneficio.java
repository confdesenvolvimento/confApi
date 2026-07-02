package com.confApi.db.clube.beneficio;

import com.confApi.db.clube.plano.Plano;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Beneficio implements Serializable {

    private Integer codgBeneficio;
    private String nomeBeneficio;
    private Set<Plano> planos = new HashSet<>();
}
