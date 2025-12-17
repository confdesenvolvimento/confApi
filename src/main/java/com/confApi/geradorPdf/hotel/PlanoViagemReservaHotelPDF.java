package com.confApi.geradorPdf.hotel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlanoViagemReservaHotelPDF {
    private String assunto;
    private String mensagem;
    private String emailPara;
    private String emailCopia;
    private boolean receberCopiaEmail;
    private boolean semValores ;
    private String emailsList;
}
