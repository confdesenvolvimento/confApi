package com.confApi.chatconfianca.operacional;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Contrato interno estrito do endpoint Manager; nao serializado para o modelo. */
@Data
public class ChatResumoOperacionalAereoDados {
    private String schema;
    private Integer codgAgencia;
    private String tipo;
    private LocalDate inicio;
    private LocalDate fim;
    private int limite;
    private Long total;
    private String fusoFonte;
    private List<Item> itens;
    @Data
    public static class Item {
        private Integer reservaId;
        private String localizador;
        private Integer vooId;
        private String companhia;
        private String numeroVoo;
        private String origem;
        private String destino;
        private LocalDateTime dataHoraLocal;
        private String statusCadastral;
    }
}
