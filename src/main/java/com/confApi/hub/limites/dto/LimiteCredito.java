package com.confApi.hub.limites.dto;

import com.confApi.hub.enumerador.TipoLimite;

import java.math.BigDecimal;

public class LimiteCredito {

    private TipoLimite tipoLimite;

    private String unidade;

    private String cliente;

    private String limite;

    private String aReceber;

    private String aFaturar;

    private String aDigitar;

    private String totalDisponivel;

    public LimiteCredito() {
    }

    public String toTextoResumo() {
        try {
            BigDecimal bdLimite = new BigDecimal(limite.replace(",", "."));
            BigDecimal bdFaturar = new BigDecimal(aFaturar.replace(",", "."));
            BigDecimal bdDigitar = new BigDecimal(aDigitar.replace(",", "."));
            BigDecimal bdReceber = new BigDecimal(aReceber.replace(",", "."));
            BigDecimal bdUtilizado = bdFaturar.add(bdDigitar).add(bdReceber);
            BigDecimal bdDisponivel = bdLimite.subtract(bdUtilizado);

            return String.format(
                    "💳 Forma de Pagamento: %s\n"
                            + "💰 Limite Total: R$ %.2f\n"
                            + "📥 Limite Utilizado: R$ %.2f\n"
                            + "✅ Total Disponível: R$ %.2f",
                    tipoLimite != null ? tipoLimite.getDescricao() : "N/A",
                    bdLimite,
                    bdUtilizado,
                    bdDisponivel
            );
        } catch (Exception e) {
            return "❌ Erro ao calcular os valores de crédito. Verifique os dados numéricos.";
        }
    }

    public LimiteCredito(TipoLimite tipoLimite, String unidade, String cliente, String limite, String aReceber, String aFaturar, String aDigitar, String totalDisponivel) {
        this.tipoLimite = tipoLimite;
        this.unidade = unidade;
        this.cliente = cliente;
        this.limite = limite;
        this.aReceber = aReceber;
        this.aFaturar = aFaturar;
        this.aDigitar = aDigitar;
        this.totalDisponivel = totalDisponivel;
    }

    /**
     * @return the tipoLimite
     */
    public TipoLimite getTipoLimite() {
        return tipoLimite;
    }

    /**
     * @param tipoLimite the tipoLimite to set
     */
    public void setTipoLimite(TipoLimite tipoLimite) {
        this.tipoLimite = tipoLimite;
    }

    /**
     * @return the unidade
     */
    public String getUnidade() {
        return unidade;
    }

    /**
     * @param unidade the unidade to set
     */
    public void setUnidade(String unidade) {
        this.unidade = unidade;
    }

    /**
     * @return the cliente
     */
    public String getCliente() {
        return cliente;
    }

    /**
     * @param cliente the cliente to set
     */
    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    /**
     * @return the limite
     */
    public String getLimite() {
        return limite;
    }

    /**
     * @param limite the limite to set
     */
    public void setLimite(String limite) {
        this.limite = limite;
    }

    /**
     * @return the aReceber
     */
    public String getaReceber() {
        return aReceber;
    }

    /**
     * @param aReceber the aReceber to set
     */
    public void setaReceber(String aReceber) {
        this.aReceber = aReceber;
    }

    /**
     * @return the aFaturar
     */
    public String getaFaturar() {
        return aFaturar;
    }

    /**
     * @param aFaturar the aFaturar to set
     */
    public void setaFaturar(String aFaturar) {
        this.aFaturar = aFaturar;
    }

    /**
     * @return the aDigitar
     */
    public String getaDigitar() {
        return aDigitar;
    }

    /**
     * @param aDigitar the aDigitar to set
     */
    public void setaDigitar(String aDigitar) {
        this.aDigitar = aDigitar;
    }

    /**
     * @return the totalDisponivel
     */
    public String getTotalDisponivel() {
        return totalDisponivel;
    }

    /**
     * @param totalDisponivel the totalDisponivel to set
     */
    public void setTotalDisponivel(String totalDisponivel) {
        this.totalDisponivel = totalDisponivel;
    }

}