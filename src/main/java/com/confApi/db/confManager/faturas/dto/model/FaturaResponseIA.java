package com.confApi.db.confManager.faturas.dto.model;

import com.confApi.db.confManager.faturas.dto.FaturaIA;

import java.util.List;

public class FaturaResponseIA {
    private String descricao = "Faturas desta agencia";
    private List<FaturaIA> faturas;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Descrição: ").append(descricao).append("\n");

        if (faturas != null && !faturas.isEmpty()) {
            sb.append("Faturas:\n");
            for (FaturaIA fatura : faturas) {
                sb.append("  - Número da Fatura: ").append(fatura.getNumfat()).append("\n")
                        .append("    Data da Fatura: ").append(fatura.getDataFatura() != null ? fatura.getDataFatura() : "N/A").append("\n")
                        // .append("    Agencia: ").append(fatura.getEmpfat()).append("\n")
                        .append("    Valor: ").append(fatura.getValor()).append("\n")
                        //.append("    Base: ").append(fatura.getCodest() != null ? fatura.getCodest() : "N/A").append("\n")
                        .append("    Pagamento: ").append(fatura.getPago()).append("\n")
                        .append("    Data de Vencimento: ").append(fatura.getDataVen() != null ? fatura.getDataVen() : "N/A").append("\n")
                        // .append("    Conta Banco: ").append(fatura.getContbanco()).append("\n")
                        .append("    Cancelada: ").append(fatura.getCanceladoDesc()).append("\n")
                        //  .append("    Condição de Pagamento: ").append(fatura.getCondicaoPagamento()).append("\n")
                        .append("    Produto: ").append(fatura.getInvoiceType()).append("\n")
                        .append("    Tipo de Fatura: ").append(fatura.getSituacao()).append("\n")
                        .append("    Fatura Vencida: ").append(fatura.getFatVenc()).append("\n")
                        .append("\n");
            }
        } else {
            sb.append("Nenhuma fatura encontrada.\n");
        }
        return sb.toString();
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public List<FaturaIA> getFaturas() {
        return faturas;
    }

    public void setFaturas(List<FaturaIA> faturas) {
        this.faturas = faturas;
    }
}
