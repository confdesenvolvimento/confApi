package com.confApi.chatconfianca.indicadores;

import com.confApi.chatconfianca.dto.reserva.ReservaAereaRecenteItem;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** Totais calculados pelo banco no universo completo, nunca pela pagina de detalhes nem pelo modelo. */
@Data
public class VendaAereaIndicadoresResultado {
    public static final String SCHEMA = "chat.vendas-aereas.v1";
    public static final String COMPOSICAO = "DASHBOARD_TARIFA_EMBARQUE_DU_RAV_RC_COMBUSTIVEL_V1";
    private String schema;
    private String status;
    private Integer codgAgencia;
    private VendaAereaIndicadoresFiltros filtrosAplicados;
    private String criterioContagem;
    private String criterioValor;
    private String statusValor;
    private String motivoValor;
    private String composicaoValor;
    @JsonSetter(nulls = Nulls.FAIL)
    private long passageirosValorIncompleto = -1;
    private boolean reembolsosDisponiveis;
    private boolean gruposTruncados;
    private List<String> limitacoes = new ArrayList<>();
    private Medida totais;
    private List<MedidaValor> totaisPorMoeda = new ArrayList<>();
    private List<Grupo> grupos = new ArrayList<>();
    private List<ReservaAereaRecenteItem> reservas = new ArrayList<>();
    @JsonSetter(nulls = Nulls.FAIL)
    private long totalDetalhes = -1;
    private int page;
    private int size;
    private int totalPages;
    private boolean hasNext;

    @Data
    public static class Medida {
        // Ausente nao pode ser confundido com zero; null explicito falha na desserializacao.
        @JsonSetter(nulls = Nulls.FAIL)
        private long reservasEmitidas = -1;
        @JsonSetter(nulls = Nulls.FAIL)
        private long localizadoresEmitidos = -1;
        @JsonSetter(nulls = Nulls.FAIL)
        private long passageirosEmitidos = -1;
        @JsonSetter(nulls = Nulls.FAIL)
        private long bilhetesAtivos = -1;
        @JsonSetter(nulls = Nulls.FAIL)
        private long bilhetesCancelados = -1;
        @JsonSetter(nulls = Nulls.FAIL)
        private long reservasCanceladas = -1;
        @JsonSetter(nulls = Nulls.FAIL)
        private long reservasOutrasSituacoes = -1;
        @JsonSetter(nulls = Nulls.FAIL)
        private long bilhetesOutrasSituacoes = -1;
    }

    @Data
    public static class MedidaValor {
        private String moeda;
        @JsonSetter(nulls = Nulls.FAIL)
        private long passageirosComValor = -1;
        private BigDecimal valorBrutoEmitido;
        private boolean completo;
    }

    @Data
    public static class Grupo {
        private String chave;
        private String descricao;
        private Medida totais;
        private String statusValor;
        private String motivoValor;
        private String composicaoValor;
        @JsonSetter(nulls = Nulls.FAIL)
        private long passageirosValorIncompleto = -1;
        private List<MedidaValor> totaisPorMoeda = new ArrayList<>();
    }
}
