package com.confApi.db.clube.quemParticipa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuemParticipa {

    private Integer codgQuemParticipa;
    private String descricao;
    private Integer flagStatus;
}
