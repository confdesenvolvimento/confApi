package com.confApi.chatconfianca.dto.remarcacao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReservasEmitidasRemarcacaoResponse {
    private List<Item> items = new ArrayList<>();
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
    private boolean hasNext;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private Integer reservaId;
        private String localizador;
        private Integer status;
        private LocalDateTime dataEmissao;
        private String companhiaIata;
        private String companhiaNome;
        private String sistema;
        private String origem;
        private String destino;
        private LocalDateTime proximaPartida;
        private String passageiroPrincipal;
        private Integer quantidadePassageiros;
        private Integer quantidadeBilhetesAtivos;
        private boolean disponivelSimulacao;
        private String motivoIndisponibilidade;
        private List<Voo> voos = new ArrayList<>();
        private List<Bilhete> bilhetes = new ArrayList<>();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Voo {
        private Integer trechoId;
        private Integer vooId;
        private String companhiaIata;
        private String companhiaNome;
        private String numeroVoo;
        private String origem;
        private String destino;
        private LocalDateTime dataHoraPartida;
        private LocalDateTime dataHoraChegada;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bilhete {
        private Integer bilheteId;
        private String numero;
        private String passageiroNome;
    }
}
