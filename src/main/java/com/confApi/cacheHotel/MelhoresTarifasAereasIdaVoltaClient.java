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

import java.util.Collections;

@Component
public class MelhoresTarifasAereasIdaVoltaClient {
    private static final String ENDPOINT = "CacheAereo/aereo/melhores-datas/ida-volta";
    private static final String MENSAGEM_CRITERIOS_INVALIDOS =
            "Os criterios da consulta de tarifas aereas de ida e volta sao invalidos.";
    private static final int TAMANHO_MAXIMO_MENSAGEM_ERRO = 200;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestTemplate restTemplate;
    private final ChatConfiancaTokenProvider tokenProvider;

    public MelhoresTarifasAereasIdaVoltaClient(
            @Qualifier("chatConfiancaRestTemplate") RestTemplate restTemplate,
            ChatConfiancaTokenProvider tokenProvider) {
        this.restTemplate = restTemplate;
        this.tokenProvider = tokenProvider;
    }

    public MelhoresTarifasAereasIdaVoltaResponse consultar(
            MelhoresTarifasAereasIdaVoltaRequest request) {
        String token = tokenProvider.bearerToken();
        try {
            return exchange(request, token).getBody();
        } catch (HttpClientErrorException.Unauthorized ex) {
            tokenProvider.invalidateIfCurrent(token);
            String novoToken = tokenProvider.bearerToken();
            try {
                return exchange(request, novoToken).getBody();
            } catch (HttpClientErrorException.Unauthorized secondUnauthorized) {
                tokenProvider.invalidateIfCurrent(novoToken);
                throw new ServiceIndisponivelException(
                        "Nao foi possivel autenticar na consulta de tarifas aereas de ida e volta.");
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
            JsonNode campoMensagem = raiz.isObject() ? raiz.get("mensagem") : null;
            if (campoMensagem == null || !campoMensagem.isTextual()) {
                return MENSAGEM_CRITERIOS_INVALIDOS;
            }
            String mensagem = campoMensagem.asText().trim();
            if (mensagem.isEmpty()
                    || mensagem.length() > TAMANHO_MAXIMO_MENSAGEM_ERRO
                    || mensagem.chars().anyMatch(Character::isISOControl)
                    || mensagem.indexOf('<') >= 0
                    || mensagem.indexOf('>') >= 0) {
                return MENSAGEM_CRITERIOS_INVALIDOS;
            }
            return mensagem;
        } catch (Exception ignored) {
            return MENSAGEM_CRITERIOS_INVALIDOS;
        }
    }

    private ServiceIndisponivelException indisponivel() {
        return new ServiceIndisponivelException(
                "A consulta de tarifas aereas de ida e volta esta indisponivel.");
    }

    private ResponseEntity<MelhoresTarifasAereasIdaVoltaResponse> exchange(
            MelhoresTarifasAereasIdaVoltaRequest request,
            String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);
        return restTemplate.exchange(
                url(),
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                MelhoresTarifasAereasIdaVoltaResponse.class);
    }

    private String url() {
        String base = UrlConfig.URL_CONFIANCA_CACHEHOTEL;
        if (base == null || base.isBlank()) {
            throw new ServiceIndisponivelException(
                    "URL do cache de tarifas aereas nao configurada.");
        }
        return (base.endsWith("/") ? base : base + "/") + ENDPOINT;
    }
}
