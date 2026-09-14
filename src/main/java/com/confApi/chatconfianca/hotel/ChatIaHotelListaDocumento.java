package com.confApi.chatconfianca.hotel;

import java.util.*;
import lombok.Data;

/** Bounded private list. Cursor and IDs are integration/server state, never model parameters. */
@Data
public class ChatIaHotelListaDocumento {
    private String contrato="hotel-lista-v1", consultadoEm;
    private ChatIaHotelDocumento.Configuracao configuracao;
    private List<Item> reservas=new ArrayList<>();
    private boolean maisResultados;
    private String proximaData;
    private Integer proximoId;
    @Data public static class Consulta {
        private Integer agencia;
        private String hospede, hotel, cidade, tipoData, inicio, fim;
        private String aposData;
        private Integer aposId;
    }
    @Data public static class Item {
        private Integer id, agencia;
        private String localizador, hotel, cidade, criacao, entrada, saida, dataOrdenacao;
    }
}
