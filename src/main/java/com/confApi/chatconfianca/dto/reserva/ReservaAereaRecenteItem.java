package com.confApi.chatconfianca.dto.reserva;

import com.confApi.chatgpt.dto.ChatActionDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ReservaAereaRecenteItem {
    private Integer reservaId;
    private String localizador;
    private Integer status;
    private String statusDescricao;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataEmissao;
    private LocalDateTime dataLimiteEmissao;
    private LocalDateTime dataCancelamento;
    private String companhiaIata;
    private String companhiaNome;
    private String sistema;
    private List<String> passageiros = new ArrayList<>();
    private Integer quantidadePassageiros;
    private List<Voo> voos = new ArrayList<>();
    private List<Bilhete> bilhetes = new ArrayList<>();
    private Integer quantidadeBilhetesAtivos;
    private LocalDateTime proximaPartida;
    private boolean disponivelSimulacao;
    private String motivoIndisponibilidade;
    private boolean cancelamentoRequerValidacao;
    private boolean emissaoCandidata;
    @JsonIgnore
    private List<ChatActionDTO> actions = new ArrayList<>();

    @Data
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
    public static class Bilhete {
        private Integer bilheteId;
        private String numero;
        private String passageiroNome;
        private Integer status;
        private LocalDateTime dataCancelamento;
    }
}
