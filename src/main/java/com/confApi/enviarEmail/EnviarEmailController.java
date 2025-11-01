package com.confApi.enviarEmail;

import com.confApi.db.confManager.geradorPdf.ReservaAereoModelPDF;
import com.confApi.geradorPdf.EnvioReservaAereoPDF.EnvioPlanoViagemReservaAereoPDF;
import com.confApi.geradorPdf.geradorAereoPDF.EnvioPdfService;
import com.confApi.geradorPdf.geradorAereoPDF.GeradorAereoPDFModel;
import com.confApi.geradorPdf.geradorAereoPDF.GeradorReservaAereoPDFService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

@Controller
@RequestMapping("/api/email")
public class EnviarEmailController {

    @Autowired
    private EnviarEmailService enviarEmailService;

    @PostMapping("/enviarEmailCodigoLiberacao")
    public void enviarEmailCodigo(@RequestBody EmailNotificacaoApp emailNotificacaoApp) throws IOException {
        new EnviarEmailService().envioEmailLiberacaoSMS(emailNotificacaoApp);
    }
}
