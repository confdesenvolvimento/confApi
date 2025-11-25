package com.confApi.geradorPdf;

import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.AbstractTransactionServiceApi;
import com.confApi.geradorPdf.aereo.EnvioReservaAereoPDF;
import com.confApi.geradorPdf.hotel.EnvioReservaHotelPDF;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;

public class GeradorPDFApi extends AbstractTransactionServiceApi implements Serializable {

    private final String urlAPI = "mail";

    public void envioAereoPDF(EnvioReservaAereoPDF envioReservaPDF) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + new ConfAppService().token());

        HttpEntity<EnvioReservaAereoPDF> requestEntity = new HttpEntity<>(envioReservaPDF, headers);

        ResponseEntity<String> responseEntity = null;
        try {
            responseEntity = new RestTemplate().exchange(
                    UrlConfig.URL_CONFIANCA_EMAIL + urlAPI + "/sendEmailPdf2",
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

    public void envioHotelPDF(EnvioReservaHotelPDF envioReservaPDF) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + new ConfAppService().token());

        HttpEntity<EnvioReservaHotelPDF> requestEntity = new HttpEntity<>(envioReservaPDF, headers);

        ResponseEntity<String> responseEntity = null;
        try {
            responseEntity = new RestTemplate().exchange(
                    UrlConfig.URL_CONFIANCA_EMAIL + urlAPI + "/sendEmailReservaHotelPDF",
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
