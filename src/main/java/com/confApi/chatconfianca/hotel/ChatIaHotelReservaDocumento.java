package com.confApi.chatconfianca.hotel;

import java.util.*;
import lombok.Data;

/** Minimal private read contract. Never serialize booking entities or supplier payloads. */
@Data
public class ChatIaHotelReservaDocumento {
    private String contrato="hotel-reserva-v1";
    private String consultadoEm;
    private ChatIaHotelDocumento.Configuracao configuracao;
    private boolean maisResultados;
    private List<Reserva> reservas=new ArrayList<>();
    @Data public static class Consulta {
        private Integer agencia;
        private String localizador, hospede, hotel, cidade;
        private Integer selecionada;
    }
    @Data public static class Reserva {
        private Integer id, agencia;
        private String localizador, hotel, cidade, entrada, saida;
        private boolean detalhesCompletos, detalhesLimitados;
        private List<Quarto> quartos=new ArrayList<>();
        private List<String> hospedes=new ArrayList<>();
    }
    @Data public static class Quarto {
        private String nome, tipo, regime;
    }
}
