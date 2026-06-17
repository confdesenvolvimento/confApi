package com.confApi.wooba.sales;

import com.confApi.util.TelegramErrorAlert;
import com.confApi.wooba.sales.dto.WoobaSalesAccessCredentials;
import com.confApi.wooba.sales.dto.WoobaSalesCompanyCredentials;
import com.confApi.wooba.sales.dto.WoobaSalesDetailsRequest;
import com.confApi.wooba.sales.dto.WoobaSalesDetailsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;

@Component
public class WoobaSalesClient {

    private static final DateTimeFormatter ACCESS_CODE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RestTemplate restTemplate;
    private final WoobaSalesProperties properties;
    private final WoobaDeveloperAccessCodeGenerator developerAccessCodeGenerator;

    @Autowired(required = false)
    private TelegramErrorAlert telegramErrorAlert;

    @Value("${wooba.webhook.trace.enabled:false}")
    private boolean traceEnabled;

    public WoobaSalesClient(RestTemplate restTemplate,
                            WoobaSalesProperties properties,
                            WoobaDeveloperAccessCodeGenerator developerAccessCodeGenerator) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.developerAccessCodeGenerator = developerAccessCodeGenerator;
    }

    public WoobaSalesDetailsResponse details(String transactionUniqueId) {
        validarConfiguracao();

        String url = UriComponentsBuilder
                .fromHttpUrl(normalizedBaseUrl())
                .path("details")
                .toUriString();

        WoobaSalesDetailsRequest request = new WoobaSalesDetailsRequest(
                transactionUniqueId,
                properties.getOffset(),
                new WoobaSalesAccessCredentials(
                        new WoobaSalesCompanyCredentials(
                                properties.getIdentifier(),
                                properties.getPassword()
                        )
                )
        );

        HttpHeaders headers = headers();
        rastrearRequestDetails(url, transactionUniqueId, headers);

        ResponseEntity<WoobaSalesDetailsResponse> response;
        try {
            response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    WoobaSalesDetailsResponse.class
            );
        } catch (HttpStatusCodeException ex) {
            rastrear("Wooba Sales details retornou erro HTTP. UniqueId=" + safe(transactionUniqueId)
                    + ", Status=" + ex.getRawStatusCode()
                    + ", Body=" + limitar(ex.getResponseBodyAsString()));
            throw ex;
        } catch (Exception ex) {
            rastrear("Wooba Sales details falhou antes de retornar resposta. UniqueId=" + safe(transactionUniqueId)
                    + ", Erro=" + ex.getClass().getSimpleName()
                    + " - " + safe(ex.getMessage()));
            throw ex;
        }

        WoobaSalesDetailsResponse body = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || body == null) {
            throw new IllegalStateException("Wooba details retornou resposta invalida para " + transactionUniqueId);
        }

        if (!body.isSuccess() || body.getTransaction() == null || body.getTransaction().isNull()) {
            throw new IllegalStateException("Wooba details nao retornou transacao valida para " + transactionUniqueId);
        }

        return body;
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        if (!isBlank(properties.getDeveloperToken())) {
            headers.set(properties.getDeveloperTokenHeaderName(), properties.getDeveloperToken());
        }

        headers.set(properties.getDeveloperAccessCodeHeaderName(), developerAccessCode());

        return headers;
    }

    private String developerAccessCode() {
        if (!isBlank(properties.getDeveloperAccessCode())) {
            return properties.getDeveloperAccessCode();
        }
        return developerAccessCodeGenerator.gerar(properties);
    }

    private String normalizedBaseUrl() {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalStateException("wooba.sales.base-url nao configurado.");
        }

        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    private void validarConfiguracao() {
        if (isBlank(properties.getIdentifier())) {
            throw new IllegalStateException("wooba.sales.identifier nao configurado.");
        }

        if (isBlank(properties.getPassword())) {
            throw new IllegalStateException("wooba.sales.password nao configurado.");
        }

        if (isBlank(properties.getDeveloperToken())) {
            throw new IllegalStateException("wooba.sales.developer-token nao configurado.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void rastrearRequestDetails(String url, String transactionUniqueId, HttpHeaders headers) {
        String developerAccessCode = headers.getFirst(properties.getDeveloperAccessCodeHeaderName());
        rastrear("Chamando Wooba Sales details. Url=" + safe(url)
                + ", UniqueId=" + safe(transactionUniqueId)
                + ", Identifier=" + mascarar(properties.getIdentifier())
                + ", PasswordConfigurado=" + booleano(!isBlank(properties.getPassword()))
                + ", Offset=" + safe(properties.getOffset())
                + ", DeveloperTokenHeader=" + safe(properties.getDeveloperTokenHeaderName())
                + ", DeveloperTokenConfigurado=" + booleano(!isBlank(headers.getFirst(properties.getDeveloperTokenHeaderName())))
                + ", DeveloperToken=" + mascarar(properties.getDeveloperToken())
                + ", DeveloperAccessCodeHeader=" + safe(properties.getDeveloperAccessCodeHeaderName())
                + ", DeveloperAccessCodeOrigem=" + (isBlank(properties.getDeveloperAccessCode()) ? "GERADO" : "CONFIGURADO")
                + ", DeveloperAccessCodeData=" + LocalDate.now().format(ACCESS_CODE_DATE_FORMAT)
                + ", DeveloperAccessCodeTamanho=" + tamanho(developerAccessCode)
                + ", DeveloperAccessCodeBytes=" + tamanhoBase64(developerAccessCode));
    }

    private void rastrear(String mensagem) {
        if (traceEnabled && telegramErrorAlert != null) {
            telegramErrorAlert.enviar(this, "[TRACE] " + mensagem);
        }
    }

    private String mascarar(String value) {
        if (isBlank(value)) {
            return "nao-configurado";
        }

        String trimmed = value.trim();
        int length = trimmed.length();
        if (length <= 4) {
            return "****";
        }

        return "****" + trimmed.substring(length - 4);
    }

    private int tamanho(String value) {
        return value == null ? 0 : value.length();
    }

    private String tamanhoBase64(String value) {
        if (isBlank(value)) {
            return "0";
        }

        try {
            return String.valueOf(Base64.getDecoder().decode(value).length);
        } catch (IllegalArgumentException ex) {
            return "base64-invalido";
        }
    }

    private String limitar(String value) {
        if (value == null) {
            return "null";
        }

        String normalized = value.replace("\r", " ").replace("\n", " ").trim();
        if (normalized.length() <= 500) {
            return normalized;
        }
        return normalized.substring(0, 500) + "...";
    }

    private String booleano(boolean value) {
        return value ? "sim" : "nao";
    }

    private String safe(String value) {
        return value == null ? "null" : value;
    }
}
