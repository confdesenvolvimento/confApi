package com.confApi.geradorPdf.seguro;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlanoViagemReservaSeguroPDF {

    private String assunto;
    private String mensagem;
    private String emailPara;
    private String emailCopia;
    private boolean receberCopiaEmail = true;
    private boolean semValores ;
    private String emailsList;
}
