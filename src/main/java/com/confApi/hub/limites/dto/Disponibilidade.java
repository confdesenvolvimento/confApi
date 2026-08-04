package com.confApi.hub.limites.dto;

import java.io.Serializable;
import java.util.List;

public class Disponibilidade implements Serializable {

    private List<LimiteCredito> limiteCredito;
    private Boolean consultaConfirmada;
    private String mensagemConsulta;

    public Disponibilidade() {
    }
    public String gerarResumoLimites() {
        if (limiteCredito == null || limiteCredito.isEmpty()) {
            return "❌ Nenhum limite de crédito disponível para exibir.";
        }

        StringBuilder sb = new StringBuilder("📊 *Resumo de Limites de Crédito*\n\n");
        for (LimiteCredito limite : limiteCredito) {
            sb.append(limite.toTextoResumo()).append("\n\n");
        }
        return sb.toString().trim(); // remove o último \n
    }
    public Disponibilidade(List<LimiteCredito> limiteCredito) {
        this.limiteCredito = limiteCredito;
    }

    /**
     * @return the limiteCredito
     */
    public List<LimiteCredito> getLimiteCredito() {
        return limiteCredito;
    }

    /**
     * @param limiteCredito the limiteCredito to set
     */
    public void setLimiteCredito(List<LimiteCredito> limiteCredito) {
        this.limiteCredito = limiteCredito;
    }

    public Boolean getConsultaConfirmada() {
        return consultaConfirmada;
    }

    public void setConsultaConfirmada(Boolean consultaConfirmada) {
        this.consultaConfirmada = consultaConfirmada;
    }

    public String getMensagemConsulta() {
        return mensagemConsulta;
    }

    public void setMensagemConsulta(String mensagemConsulta) {
        this.mensagemConsulta = mensagemConsulta;
    }
}
