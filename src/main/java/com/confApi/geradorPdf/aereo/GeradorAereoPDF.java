package com.confApi.geradorPdf.aereo;

import lombok.Data;

@Data
public class GeradorAereoPDF {
    private Integer usuarioCodg;
    private String usuarioLogin;
    private Integer agenciaCodg;
    private String reservaLocalizaodr;
    private String emails;
    private Boolean semValores;
}
