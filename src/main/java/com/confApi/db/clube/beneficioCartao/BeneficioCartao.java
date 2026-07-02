package com.confApi.db.clube.beneficioCartao;

import com.confApi.db.clube.cartaoCia.CartaoCia;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Data
public class BeneficioCartao implements Serializable {

    private Integer codgBeneficioCartao;
    private String nomeBeneficio;
    private Integer statusBeneficio;
    private CartaoCia cartaoCia;
}
