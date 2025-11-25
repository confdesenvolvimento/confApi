package com.confApi.geradorPdf.aereo;

import lombok.Data;

import java.io.Serializable;

@Data
public class PlanoViagemReservaAereoPDF implements Serializable {
    private String assunto;
    private String mensagem;
    private String emailPara;
    private String emailCopia;
    private boolean receberCopiaEmail;
    private boolean semValores;
}
