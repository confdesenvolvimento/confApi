package com.confApi.db.wooba.vendas.dto;

import java.util.List;

public class VendaAereaExibicaoResponse {

    private String descricao = "Listagem de vendas aereas";
    private List<VendasAereasExibicaoIA> vendas;

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("Descrição: ").append(descricao).append("\n");

        if (vendas != null && !vendas.isEmpty()) {

            sb.append("Informações da Venda Aérea:\n");
            for (VendasAereasExibicaoIA venda : vendas) {

                sb.append("  - Companhia Aérea (Sigla): ").append(venda.getSiglaCia() != null ? venda.getSiglaCia() : "N/A").append("\n");
                sb.append("  - Companhia Aérea (Nome): ").append(venda.getNomeCia() != null ? venda.getNomeCia() : "N/A").append("\n");
                sb.append("  - Número do Bilhete: ").append(venda.getNumeroBilhete() != null ? venda.getNumeroBilhete() : "N/A").append("\n");
                sb.append("  - Localizador: ").append(venda.getLocalizador() != null ? venda.getLocalizador() : "N/A").append("\n");
                sb.append("  - Passageiro: ").append(venda.getPassageiro() != null ? venda.getPassageiro() : "N/A").append("\n");
                sb.append("  - Rota: ").append(venda.getRota() != null ? venda.getRota() : "N/A").append("\n");
                sb.append("  - Tipo de Rota: ").append(venda.getTipoRota() != null ? venda.getTipoRota() : "N/A").append("\n");
                sb.append("  - Valor: R$ ").append(venda.getValor() != null ? String.format("%.2f", venda.getValor()) : "0.00").append("\n");
                String status;
                if (venda.getStatus() == null) {
                    status = "N/A";
                } else if (Integer.parseInt(venda.getStatus()) == 1) {
                    status = "Emitida";
                } else if (Integer.parseInt(venda.getStatus()) == 2) {
                    status = "Cancelada";
                } else {
                    status = "Desconhecido";
                }
                sb.append("  - Status: ").append(status).append("\n");
                sb.append("  - Fonte: ").append(venda.getFonte()).append("\n");
                sb.append("  - Data de Emissão: ").append(venda.getDataEmissao() != null ? venda.getDataEmissao() : "N/A").append("\n")
                        .append("\n");

            }
        } else {
            sb.append("Nenhuma venda encontrada.\n");
        }
        return sb.toString();
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public List<VendasAereasExibicaoIA> getVendas() {
        return vendas;
    }

    public void setVendas(List<VendasAereasExibicaoIA> vendas) {
        this.vendas = vendas;
    }
}
