package com.confApi.geradorPdf.geradorAereoPDF;

import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.AbstractTransactionServiceApi;
import com.confApi.geradorPdf.EnvioReservaAereoPDF.EnvioPlanoViagemReservaAereoPDF;
import com.confApi.geradorPdf.aereo.EnvioReservaAereoPDF;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class EnvioPdfService extends AbstractTransactionServiceApi {

    public void envioPDF(EnvioPlanoViagemReservaAereoPDF envioReservaAereoPDF) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + new ConfAppService().token());

        HttpEntity<EnvioPlanoViagemReservaAereoPDF> requestEntity = new HttpEntity<>(envioReservaAereoPDF, headers);

        ResponseEntity<String> responseEntity = null;
        try {
            responseEntity = new RestTemplate().exchange(
                    UrlConfig.URL_CONFIANCA_EMAIL + "/mail/sendEmailPdf",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
        } catch (HttpClientErrorException ex) {
          //  Util.mensageAlert(EnumIconesMensagem.Erro.getValor(), "Ops: " + ex.getMessage());
        }

        if (responseEntity != null && responseEntity.getStatusCode().is2xxSuccessful()) {
           // Util.mensageSucessPDF("PDF enviado com sucesso!");
        }
    }
    public void envioPDF2(EnvioReservaAereoPDF envioReservaPDF) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + new ConfAppService().token());

        HttpEntity<EnvioReservaAereoPDF> requestEntity = new HttpEntity<>(envioReservaPDF, headers);

        ResponseEntity<String> responseEntity = null;
        try {
            responseEntity = new RestTemplate().exchange(
                    UrlConfig.URL_CONFIANCA_EMAIL + "/mail/sendEmailPdf2",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
        } catch (HttpClientErrorException ex) {
            //  Util.mensageAlert(EnumIconesMensagem.Erro.getValor(), "Ops: " + ex.getMessage());
        }

        if (responseEntity != null && responseEntity.getStatusCode().is2xxSuccessful()) {
            // Util.mensageSucessPDF("PDF enviado com sucesso!");
        }
    }

}
