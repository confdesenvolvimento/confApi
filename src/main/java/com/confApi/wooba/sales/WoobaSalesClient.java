package com.confApi.wooba.sales;

import com.confApi.wooba.sales.dto.WoobaSalesAccessCredentials;
import com.confApi.wooba.sales.dto.WoobaSalesCompanyCredentials;
import com.confApi.wooba.sales.dto.WoobaSalesDetailsRequest;
import com.confApi.wooba.sales.dto.WoobaSalesDetailsResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;

@Component
public class WoobaSalesClient {

    private final RestTemplate restTemplate;
    private final WoobaSalesProperties properties;
    private final WoobaDeveloperAccessCodeGenerator developerAccessCodeGenerator;

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

        ResponseEntity<WoobaSalesDetailsResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(request, headers()),
                WoobaSalesDetailsResponse.class
        );

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
}
