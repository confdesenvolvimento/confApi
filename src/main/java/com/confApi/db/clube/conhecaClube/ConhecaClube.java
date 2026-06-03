package com.confApi.db.clube.conhecaClube;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConhecaClube {

    private Integer codgConnhecaClube;
    private String descricao;
    private Integer flagStatus;
}
