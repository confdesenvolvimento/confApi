package com.confApi.chatconfianca.indicadores;

import lombok.Data;

/** Somente criterios de leitura; nunca totais, precos ou autorizacao reaproveitados do historico. */
@Data
public class VendaAereaIndicadoresEstado {
    private Long conversa;
    private Long agencia;
    private Long usuario;
    private Long atualizadoEm;
    private VendaAereaIndicadoresFiltros filtros;
    private String foco = "RESUMO";
}
