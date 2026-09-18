package com.confApi.chatconfianca.intencao;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ChatIntencaoRuntimeDto {
    private Long id;
    private String codigo;
    private String nome;
    private String tipo;
    private Integer prioridade;
    private List<Termo> termos = new ArrayList<>();
    private List<Memoria> memorias = new ArrayList<>();

    @Data
    public static class Termo {
        private Long id;
        private String termo;
        private String termoNormalizado;
        private BigDecimal peso;
        private String polaridade;
    }

    @Data
    public static class Memoria {
        private Integer codgMemoria;
        private String base;
        private Integer codgUnidade;
        private String texto;
        private Integer prioridade;
    }
}
