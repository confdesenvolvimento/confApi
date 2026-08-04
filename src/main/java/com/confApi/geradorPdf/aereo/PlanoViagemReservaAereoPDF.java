package com.confApi.geradorPdf.aereo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlanoViagemReservaAereoPDF implements Serializable {
    private String assunto;
    private String mensagem;
    private String emailPara;
    private String emailCopia;
    private boolean receberCopiaEmail;
    private boolean semValores;
    private String emailsList;

    public PlanoViagemReservaAereoPDF(GeradorAereoPDF geradorAereoPDF) {
        this.assunto = textoOuPadrao(
                geradorAereoPDF.getAssunto(),
                "Plano de viagem - " + geradorAereoPDF.getReservaLocalizaodr()
        );
        this.mensagem = textoOuPadrao(
                geradorAereoPDF.getMensagem(),
                "Segue o plano de viagem para o Localizador" + geradorAereoPDF.getReservaLocalizaodr()
        );
        this.emailPara = geradorAereoPDF.getEmails();
        this.emailCopia = null;
        this.receberCopiaEmail = false;
        this.semValores = geradorAereoPDF.getSemValores();
    }

    private String textoOuPadrao(String texto, String padrao) {
        return texto != null && !texto.trim().isEmpty() ? texto.trim() : padrao;
    }
}
