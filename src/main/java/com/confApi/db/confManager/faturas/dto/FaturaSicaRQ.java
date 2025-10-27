package com.confApi.db.confManager.faturas.dto;

import lombok.Data;

@Data
public class FaturaSicaRQ {
    private Integer invoiceNumber;
    private String invoiceType;
    private String empfat;
    private String tipoData;
    private String dataInicio;
    private String dataFim;
    private String pagamento;
    private Boolean disabledAFaturar ;

}
