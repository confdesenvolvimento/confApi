package com.confApi.notificacao;

public enum EnumTipoNotificacao {
    condicoes(1, "condições"),
    reembolso(2, "Reembolso"),
    geral(3, "Geral"),
    reservaAereo(4, "Reserva Aereo"),
    cotacaoAereo(5, "Cotação Aereo"),
    cotacaoHotel(6, "Cotação Hotel"),
    plantaoAtendimento(7, "Atendimento Plantão"),
    reservaHotel(8, "Reserva Hotel"),
    reservaPacote(9, "Reserva Pacote"),
    reservaSeguro(10, "Reserva Seguro");

    private int valor;
    private String descricao;

    private EnumTipoNotificacao(int valor, String descricao) {
        this.valor = valor;
        this.descricao = descricao;
    }

    /**
     * @return the valor
     */
    public int getValor() {
        return valor;
    }

    /**
     * @param valor the valor to set
     */
    public void setValor(int valor) {
        this.valor = valor;
    }

    /**
     * @return the descricao
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * @param descricao the descricao to set
     */
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

}
