package com.confApi.geradorPdf;

import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.AbstractTransactionServiceApi;
import com.confApi.geradorPdf.aereo.EnvioReservaAereoPDF;
import com.confApi.geradorPdf.carro.EnvioReservaCarroPDF;
import com.confApi.geradorPdf.hotel.EnvioReservaHotelPDF;
import com.confApi.geradorPdf.rodoviario.EnvioReservaRodoviarioPDF;
import com.confApi.geradorPdf.rodoviario.PlanoViagemReservaRodoviarioPDF;
import com.confApi.geradorPdf.seguro.EnvioReservaSeguroPDF;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class GeradorPDFApi extends AbstractTransactionServiceApi implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(GeradorPDFApi.class);
    private final RestTemplate aereoHttp;
    private final ConfAppService aereoAuth;

    public GeradorPDFApi() {
        this(new RestTemplate(), new ConfAppService());
    }

    GeradorPDFApi(RestTemplate http, ConfAppService auth) {
        this.aereoHttp = http;
        this.aereoAuth = auth;
    }

    private final String urlAPI = "mail";

    public byte[] gerarAereoPDF(EnvioReservaAereoPDF envioReservaPDF) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + new ConfAppService().token());

        HttpEntity<EnvioReservaAereoPDF> requestEntity = new HttpEntity<>(envioReservaPDF, headers);

        try {
            ResponseEntity<byte[]> responseEntity = new RestTemplate().exchange(
                    UrlConfig.URL_CONFIANCA_EMAIL + urlAPI + "/gerar-pdf-reserva-aereo",
                    HttpMethod.POST, requestEntity, byte[].class);

            if (responseEntity.getStatusCode().is2xxSuccessful()
                && responseEntity.getBody() != null) {
                return responseEntity.getBody();
            }
        } catch (HttpClientErrorException ex) {
            System.err.println("Erro ao gerar PDF: " + ex.getMessage());
        }

        return new byte[0];
    }

    public void envioAereoPDF(EnvioReservaAereoPDF envioReservaPDF) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aereoAuth.token().getToken());

        HttpEntity<EnvioReservaAereoPDF> requestEntity = new HttpEntity<>(envioReservaPDF, headers);
        ResponseEntity<String> responseEntity = aereoHttp.exchange(
                UrlConfig.URL_CONFIANCA_EMAIL + urlAPI + "/sendEmailPdf",
                HttpMethod.POST, requestEntity, String.class);

        try {
            JsonNode result = new ObjectMapper().readTree(responseEntity.getBody());
            if (!responseEntity.getStatusCode().is2xxSuccessful()
                || result == null || !result.isArray() || result.isEmpty()) {
                throw new IllegalStateException("O serviço não confirmou o envio do e-mail da reserva.");
            }
            for (JsonNode recipient : result) {
                String status = recipient.path("statusEnvio").asText();
                if (!"Email enviado".equalsIgnoreCase(status)
                    && !"Email enviado com sucesso".equalsIgnoreCase(status)) {
                    throw new IllegalStateException("O serviço informou falha no envio do e-mail da reserva.");
                }
            }
        } catch (JsonProcessingException invalid) {
            throw new IllegalStateException("Resposta inválida do serviço de e-mail.", invalid);
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
                    HttpMethod.POST, requestEntity, String.class);
        } catch (HttpClientErrorException ex) {
            // Comportamento preexistente dos demais produtos.
        }

        if (responseEntity != null && responseEntity.getStatusCode().is2xxSuccessful()) {
            // Envio concluído.
        }
    }

    public void envioCarroPDF(EnvioReservaCarroPDF envioReservaCarroPDF) {
        System.out.println("envioCarroPDF:: geradorPDFAPI");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + new ConfAppService().token());

        HttpEntity<EnvioReservaCarroPDF> requestEntity = new HttpEntity<>(envioReservaCarroPDF, headers);

        ResponseEntity<String> responseEntity = null;
        try {
            responseEntity = new RestTemplate().exchange(
                    UrlConfig.URL_CONFIANCA_EMAIL + urlAPI + "/sendEmailCarroPdf",
                    HttpMethod.POST, requestEntity, String.class);
        } catch (HttpClientErrorException ex) {
            // Comportamento preexistente dos demais produtos.
        }

        if (responseEntity != null && responseEntity.getStatusCode().is2xxSuccessful()) {
            // Envio concluído.
        }
    }

    public void envioSeguroPDF(EnvioReservaSeguroPDF envioReservaSeguroPDF) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + new ConfAppService().token());

        HttpEntity<EnvioReservaSeguroPDF> requestEntity = new HttpEntity<>(envioReservaSeguroPDF, headers);

        ResponseEntity<String> responseEntity = null;
        try {
            responseEntity = new RestTemplate().exchange(
                    UrlConfig.URL_CONFIANCA_EMAIL + urlAPI + "/sendEmailSeguroPdf",
                    HttpMethod.POST, requestEntity, String.class);
        } catch (HttpClientErrorException ex) {
            // Comportamento preexistente dos demais produtos.
        }

        if (responseEntity != null && responseEntity.getStatusCode().is2xxSuccessful()) {
            // Envio concluído.
        }
    }

    public void envioRodoviarioPDF(EnvioReservaRodoviarioPDF envioReservaPDF) {
        if (envioReservaPDF == null
            || envioReservaPDF.getReservaRodoviarioModelPDF() == null
            || envioReservaPDF.getPlanoViagemReservaRodoviarioPDF() == null) {
            throw new IllegalArgumentException("Informe a reserva e o plano de viagem rodoviário.");
        }

        String envioId = UUID.randomUUID().toString();
        PlanoViagemReservaRodoviarioPDF plano = envioReservaPDF.getPlanoViagemReservaRodoviarioPDF();
        JsonNode emailsUsuario = new ObjectMapper()
                .valueToTree(envioReservaPDF.getUsuarioConfDto()).path("emails");
        long totalEmailsUsuario = emailsUsuario.isArray()
                ? emailsUsuario.size() : contarEmails(emailsUsuario.asText(""));
        log.info("Rodoviario email inicio envioId={} para={} copia={} lista={} copiaUsuario={} emailsUsuario={}",
                envioId, contarEmails(plano.getEmailPara()), contarEmails(plano.getEmailCopia()),
                contarEmails(plano.getEmailsList()), plano.isReceberCopiaEmail(), totalEmailsUsuario);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(aereoAuth.token().getToken());

        ResponseEntity<String> response;
        try {
            response = aereoHttp.exchange(
                    UrlConfig.URL_CONFIANCA_EMAIL + urlAPI + "/sendEmailRodoviarioPdf",
                    HttpMethod.POST,
                    new HttpEntity<>(envioReservaPDF, headers),
                    String.class);
        } catch (RestClientException ex) {
            log.warn("Rodoviario email falhou envioId={} tipo={}",
                    envioId, ex.getClass().getSimpleName());
            throw ex;
        }

        try {
            String corpo = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || corpo == null || corpo.isBlank()) {
                throw new IllegalStateException("O serviço não confirmou o envio do e-mail rodoviário.");
            }
            JsonNode destinatarios = new ObjectMapper().readTree(corpo);
            if (!destinatarios.isArray() || destinatarios.isEmpty()) {
                throw new IllegalStateException("O serviço não confirmou o envio do e-mail rodoviário.");
            }
            Set<String> emailsUnicos = new HashSet<>();
            for (JsonNode destinatario : destinatarios) {
                String email = destinatario.path("emailDestinatario")
                        .asText("").trim().toLowerCase(Locale.ROOT);
                if (!email.isEmpty()) {
                    emailsUnicos.add(email);
                }
                String status = destinatario.path("statusEnvio").asText();
                if (!"Email enviado".equalsIgnoreCase(status)
                    && !"Email enviado com sucesso".equalsIgnoreCase(status)) {
                    throw new IllegalStateException("O serviço informou falha no envio do e-mail rodoviário.");
                }
            }
            log.info("Rodoviario email resposta envioId={} registros={} destinatariosUnicos={}",
                    envioId, destinatarios.size(), emailsUnicos.size());
            if (!emailsUnicos.isEmpty() && destinatarios.size() > emailsUnicos.size()) {
                log.warn("Rodoviario email duplicidade na resposta envioId={} registros={} destinatariosUnicos={}",
                        envioId, destinatarios.size(), emailsUnicos.size());
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Resposta inválida do serviço de e-mail rodoviário.", ex);
        }
    }

    private static long contarEmails(String lista) {
        if (lista == null || lista.isBlank()) {
            return 0;
        }
        return java.util.Arrays.stream(lista.split("[;,]"))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .count();
    }

    public byte[] gerarRodoviarioPDF(EnvioReservaRodoviarioPDF envioReservaPDF) {
        if (envioReservaPDF == null || envioReservaPDF.getReservaRodoviarioModelPDF() == null) {
            throw new IllegalArgumentException("Informe a reserva rodoviária para gerar o PDF.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_PDF));
        headers.setBearerAuth(aereoAuth.token().getToken());

        ResponseEntity<byte[]> response = aereoHttp.exchange(
                UrlConfig.URL_CONFIANCA_EMAIL + urlAPI + "/gerar-pdf-reserva-rodoviario",
                HttpMethod.POST,
                new HttpEntity<>(envioReservaPDF, headers),
                byte[].class);

        byte[] pdf = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || pdf == null || pdf.length == 0) {
            throw new IllegalStateException("O serviço não retornou o PDF da reserva rodoviária.");
        }
        return pdf;
    }
}
