package com.confApi.db.clube.cartaoCia;

import com.confApi.db.clube.arquivoAnexo.ArquivoAnexo;
import com.confApi.db.clube.beneficioCartao.BeneficioCartao;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Data
public class CartaoCia implements Serializable {

    private Integer codgCartaoCia;
    private String nomeCartaoCia;
    private String codgIataCia;
    private String nomeCompanhia;
    private String descricaoCartao;
    private Integer tipoBeneficio;
    private Double valorFundo;
    private ArquivoAnexo arquivoAnexo;
    private List<BeneficioCartao> beneficios;
}
