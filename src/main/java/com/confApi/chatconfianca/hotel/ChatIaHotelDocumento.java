package com.confApi.chatconfianca.hotel;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** Public establishment data only. Never serialize Hotel entities/booking relationships. */
@Data
public class ChatIaHotelDocumento {
    private String contrato = "hotel-dados-v1";
    private String consultadoEm;
    private Configuracao configuracao;
    private boolean maisResultados;
    private List<Hotel> hoteis = new ArrayList<>();
    @Data public static class Configuracao {
        private String revisao;
        private String orientacoes;
        private String restricoes;
        private String objetivo;
        private String pergunta;
        private String orientacoesAcao;
        private String instrucao;
        private Integer limiteResultados;
        private Integer timeoutSegundos;
        private Integer maxSugestoes;
        private Integer qtdInteracoesAtendente;
    }
    @Data public static class Hotel {
        private Integer id;
        private String nome;
        private String cidade;
        private String uf;
        private String pais;
        private String endereco;
        private String telefone;
        private String email;
        private String categoria;
        private String checkin;
        private String checkout;
        private String horarioCafe;
        private String descricao;
        private List<String> detalhes = new ArrayList<>();
    }
}
