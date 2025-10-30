package com.confApi.db.confManager.alertaTarifa.dto.ia;

import com.confApi.db.confManager.alertaTarifa.dto.AlertaTarifaDTO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
@Data
public class AlertaTarifaIAResponse {
    private String descricao ="Alerta de Tarifa";
    private List<AlertaTarifaDTO> tarifas =new ArrayList<AlertaTarifaDTO>();


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("📊 ").append(descricao != null ? descricao : "Alertas de Tarifa").append("\n");
        sb.append("=====================================================\n");

        if (tarifas == null || tarifas.isEmpty()) {
            sb.append("Nenhum alerta de tarifa encontrado.\n");
            return sb.toString();
        }

        int contador = 1;
        for (AlertaTarifaDTO alerta : tarifas) {
            sb.append("🧾 [Alerta #").append(contador).append("]\n");
            sb.append(alerta.toString()).append("\n");
            sb.append("-----------------------------------------------------\n");
            contador++;
        }

        sb.append("Total de alertas: ").append(tarifas.size()).append("\n");
        return sb.toString();
    }

}
