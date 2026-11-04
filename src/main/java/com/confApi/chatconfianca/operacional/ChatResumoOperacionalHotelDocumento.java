package com.confApi.chatconfianca.operacional;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** Mirror of the bounded private Manager projection; never supplier entities or payment data. */
@Data
public class ChatResumoOperacionalHotelDocumento {
    public static final String CONTRATO = "chat.operacional-hotel.v1";
    private String contrato = CONTRATO;
    private String consultadoEm;
    private Integer agencia, unidade;
    private String tipoData, inicio, fim;
    private String cobertura = "MANAGER";
    private List<Item> reservas = new ArrayList<>();
    private boolean maisResultados;

    @Data public static class Consulta {
        private Integer agencia, unidade;
        private String tipoData, inicio, fim;
    }
    @Data public static class Item {
        private Integer id, agencia, unidade, status;
        private String localizador, hotel, cidade, entrada, saida, dataCancelamento;
    }
}
