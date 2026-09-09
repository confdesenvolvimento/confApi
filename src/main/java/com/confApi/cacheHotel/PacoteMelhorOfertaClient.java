package com.confApi.cacheHotel;

import com.confApi.chatconfianca.client.ChatConfiancaTokenProvider;
import com.confApi.config.UrlConfig;
import com.confApi.exception.RegraDeNegocioException;
import com.confApi.exception.ServiceIndisponivelException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class PacoteMelhorOfertaClient {
    private static final String ENDPOINT = "cacheHotel/oportunidades/pacotes/buscar";
    private static final String MENSAGEM_CRITERIOS_INVALIDOS =
            "Os criterios da consulta de pacotes sao invalidos.";
    private static final int TAMANHO_MAXIMO_MENSAGEM_ERRO = 200;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestTemplate restTemplate;
    private final ChatConfiancaTokenProvider tokenProvider;

    public PacoteMelhorOfertaClient(
            @Qualifier("chatConfiancaRestTemplate") RestTemplate restTemplate,
            ChatConfiancaTokenProvider tokenProvider) {
        this.restTemplate = restTemplate;
        this.tokenProvider = tokenProvider;
    }

    public List<PacoteMelhorOfertaDTO> consultar(PacoteMelhorOfertaRequest request) {
        String token = tokenProvider.bearerToken();
        try {
            return corpo(exchange(request, token));
        } catch (HttpClientErrorException.Unauthorized ex) {
            tokenProvider.invalidateIfCurrent(token);
            String novoToken = tokenProvider.bearerToken();
            try {
                return corpo(exchange(request, novoToken));
            } catch (HttpClientErrorException.Unauthorized secondUnauthorized) {
                tokenProvider.invalidateIfCurrent(novoToken);
                throw new ServiceIndisponivelException(
                        "Nao foi possivel autenticar na consulta de oportunidades de pacote.");
            } catch (HttpStatusCodeException secondStatus) {
                throw mapStatus(secondStatus);
            } catch (RestClientException secondIoError) {
                throw indisponivel();
            }
        } catch (HttpStatusCodeException ex) {
            throw mapStatus(ex);
        } catch (RestClientException ex) {
            throw indisponivel();
        }
    }

    private List<PacoteMelhorOfertaDTO> corpo(
            ResponseEntity<PacoteMelhorOfertaDTO[]> response) {
        PacoteMelhorOfertaDTO[] body = response.getBody();
        return body == null ? List.of() : Arrays.asList(body);
    }

    private RuntimeException mapStatus(HttpStatusCodeException ex) {
        if (ex.getStatusCode().is4xxClientError()) {
            return new RegraDeNegocioException(ex.getRawStatusCode(), mensagemSegura(ex));
        }
        return indisponivel();
    }

    private String mensagemSegura(HttpStatusCodeException ex) {
        String corpo = ex.getResponseBodyAsString();
        if (corpo == null || corpo.isBlank()) {
            return MENSAGEM_CRITERIOS_INVALIDOS;
        }
        try {
            JsonNode raiz = JSON.readTree(corpo);
            JsonNode mensagem = raiz.isObject() ? raiz.get("mensagem") : null;
            if (mensagem == null || !mensagem.isTextual()) {
                return MENSAGEM_CRITERIOS_INVALIDOS;
            }
            String valor = mensagem.asText().trim();
            return valor.isEmpty()
                    || valor.length() > TAMANHO_MAXIMO_MENSAGEM_ERRO
                    || valor.chars().anyMatch(Character::isISOControl)
                    || valor.indexOf('<') >= 0
                    || valor.indexOf('>') >= 0
                    ? MENSAGEM_CRITERIOS_INVALIDOS
                    : valor;
        } catch (Exception ignored) {
            return MENSAGEM_CRITERIOS_INVALIDOS;
        }
    }

    private ServiceIndisponivelException indisponivel() {
        return new ServiceIndisponivelException(
                "A consulta de oportunidades de pacote esta indisponivel.");
    }

    private ResponseEntity<PacoteMelhorOfertaDTO[]> exchange(
            PacoteMelhorOfertaRequest request,
            String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);
        return restTemplate.exchange(
                url(),
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                PacoteMelhorOfertaDTO[].class);
    }

    private String url() {
        String base = UrlConfig.URL_CONFIANCA_CACHEHOTEL;
        if (base == null || base.isBlank()) {
            throw new ServiceIndisponivelException("URL do cache de pacotes nao configurada.");
        }
        return (base.endsWith("/") ? base : base + "/") + ENDPOINT;
    }
}
