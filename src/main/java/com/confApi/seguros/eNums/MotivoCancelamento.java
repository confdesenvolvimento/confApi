package com.confApi.seguros.eNums;

public enum MotivoCancelamento {
    DESISTENCIA(1,"Desistência") ,
    DESEJA_CONTRATAR_OUTRO_PLANO(2,"Deseja contratar outro plano") ,
    DIVERGENCIA_DE_INFORMACOES(3,"Divergência de informações"),
    ERRO_DE_SISTEMA_NA_EMISSAO(4,"Erro de sistema na emissão"),
    OUTROS_MOTIVOS(5,"Outros motivos"),
    EMISSAO_DE_TESTE(6,"Emissão de Teste"),
    BOLETO_OU_LINK_VENCIDO(7,"Boleto ou Link vencido"),
    EMISSAO_ABORTADA(8,"Emissão Abortada"),
    PAGAMENTO_DEBITO_ABORTADO(9,"Pagamento Débito abortado");

    private Integer cod;
    private String descricao;

    MotivoCancelamento(Integer cod, String descricao) {
        this.cod = cod;
        this.descricao = descricao;
    }

    public Integer getCod() {
        return cod;
    }

    public void setCod(Integer cod) {
        this.cod = cod;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public static MotivoCancelamento toEnum(Integer cod) {
        if (cod == null) {
            return null;
        }

        for (MotivoCancelamento x : MotivoCancelamento.values()) {
            if (cod.equals(x.getCod())) {
                return x;
            }
        }
        throw new IllegalArgumentException("ID inválido: " + cod);
    }

    public static MotivoCancelamento toDesc(String descricao) {
        if (descricao == null) {
            return null;
        }

        for (MotivoCancelamento x : MotivoCancelamento.values()) {
            if (descricao.equals(x.getDescricao())) {
                return x;
            }
        }
        throw new IllegalArgumentException("ID inválido: " + descricao);
    }
}
