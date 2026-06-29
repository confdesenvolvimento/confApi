package com.confApi.db.confManager.familia.dto;

import com.confApi.db.confManager.companhiaAerea.CompanhiaAerea;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
@Data
public class FamiliaCompanhia implements Serializable {
    private Integer codgFamiliaCompanhia;
    private CompanhiaAerea companhiaAerea;
    private String nomeFamiliaCompanhia;
    private String nomeFamiliaCompanhiaDescricao;
    private String codSigla;
    private String corFamilia;
    private Integer posicao;
    private Integer tipoRota;
    private List<FamiliaInformacoes> familiaInformacoes;
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("✈️ Companhia: ").append(companhiaAerea != null ? companhiaAerea.getNomeCia() : "N/I");
        if (companhiaAerea != null && companhiaAerea.getIataCia() != null) {
            sb.append(" (").append(companhiaAerea.getIataCia()).append(")");
        }
        sb.append("\n");

        sb.append("💺 Família: ").append(nomeFamiliaCompanhia != null ? nomeFamiliaCompanhia : "N/I");
        if (nomeFamiliaCompanhiaDescricao != null && !nomeFamiliaCompanhiaDescricao.isEmpty()) {
            sb.append(" – ").append(nomeFamiliaCompanhiaDescricao);
        }
        if (codSigla != null) {
            sb.append(" (").append(codSigla).append(")");
        }
        sb.append("\n");

        sb.append(" | Rota: ").append(rotuloTipoRota());
        sb.append("\n\n");

        if (familiaInformacoes != null && !familiaInformacoes.isEmpty()) {
            sb.append("🧾 Benefícios e Condições:\n");
            for (FamiliaInformacoes info : familiaInformacoes) {
                String marcador;
                if (info.getFlagContempla() == null) {
                    marcador = "•";
                } else {
                    switch (info.getFlagContempla()) {
                        case 1 -> marcador = "✅"; // incluso
                        case 2 -> marcador = "💲"; // disponível com taxa
                        default -> marcador = "❌"; // não incluso
                    }
                }
                sb.append("   ").append(marcador).append(" ").append(info.getDescricao()).append("\n");
            }
        } else {
            sb.append("Sem informações de benefícios cadastradas.\n");
        }

        return sb.toString();
    }

    private String rotuloTipoRota() {
        if (tipoRota == null) {
            return "N/I";
        }
        if (tipoRota == 0) {
            return "Ambos";
        }
        if (tipoRota == 1) {
            return "Nacional";
        }
        if (tipoRota == 2) {
            return "Internacional";
        }
        return "N/I";
    }
}
