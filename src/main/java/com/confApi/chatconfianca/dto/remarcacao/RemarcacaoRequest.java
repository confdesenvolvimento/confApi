package com.confApi.chatconfianca.dto.remarcacao;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

public final class RemarcacaoRequest {
    private RemarcacaoRequest() {
    }

    @Data
    public static class Iniciar {
        private Long conversaId;
        private Integer codgUsuario;
        private Integer reservaId;
        private String localizador;
        private Integer codgAgenciaSessao;
    }

    @Data
    public static class SelecionarTrecho {
        private Integer codgUsuario;
        private Integer trechoIndice;
        private List<Integer> trechosIndices;
    }

    @Data
    public static class SelecionarPassageiros {
        private Integer codgUsuario;
        private String escopo;
        private Integer passageiroIndice;
    }

    @Data
    public static class Pesquisar {
        private Integer codgUsuario;
        private LocalDate data;
        private String periodo;
        private Boolean somenteDireto;
    }

    @Data
    public static class Simular {
        private Integer codgUsuario;
        private Integer opcaoIndice;
        private Integer familiaIndice;
    }

    @Data
    public static class SelecionarFormaPagamento {
        private Integer codgUsuario;
        private Integer codigo;
    }

    @Data
    public static class Encaminhar {
        private Integer codgUsuario;
    }

    @Data
    public static class Voltar {
        private Integer codgUsuario;
        private Integer versaoEsperada;
    }
}
