package com.confApi.enviarEmail;

import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.geradorPdf.EnvioReservaAereoPDF.EnvioPlanoViagemReservaAereoPDF;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class EnviarEmailService {

    public void envioEmailLiberacaoSMS(EmailNotificacaoApp emailNotificacaoApp) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + new ConfAppService().token());

        HttpEntity<EmailNotificacaoApp> requestEntity = new HttpEntity<>(emailNotificacaoApp, headers);

        ResponseEntity<String> responseEntity = null;
        try {
            responseEntity = new RestTemplate().exchange(
                    UrlConfig.URL_CONFIANCA_EMAIL +"/mail/sendEmailCodigoLoginApp",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
        } catch (HttpClientErrorException ex) {
            System.out.println("ERRO: " + ex.getMessage());
            //  Util.mensageAlert(EnumIconesMensagem.Erro.getValor(), "Ops: " + ex.getMessage());
        }

        if (responseEntity != null && responseEntity.getStatusCode().is2xxSuccessful()) {
            // Util.mensageSucessPDF("PDF enviado com sucesso!");
        }
    }
}
