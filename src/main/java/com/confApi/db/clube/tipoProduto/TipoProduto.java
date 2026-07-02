package com.confApi.db.clube.tipoProduto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TipoProduto implements Serializable {
    private Integer codgTipoProduto;
    private String nome;
}
