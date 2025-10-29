package com.confApi.db.confManager.familia.dto.ia;

import com.confApi.db.confManager.familia.dto.FamiliaCompanhia;
import lombok.Data;

import java.util.List;
@Data
public class FamiliaIAResponse {
    private String descricao = "Lista das familias das tarifas das companhias aereas.";
    List<FamiliaCompanhia> familiaCompanhias;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(descricao).append("\n------------------\n");
        for (FamiliaCompanhia f : familiaCompanhias) {
            sb.append(f.toString()).append("\n------------------\n");
        }
        return sb.toString();
    }
}
