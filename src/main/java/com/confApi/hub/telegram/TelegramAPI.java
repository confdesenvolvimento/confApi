package com.confApi.hub.telegram;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.hub.telegram.dto.MensagemRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;

@Component
public class TelegramAPI {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ConfAppService confAppService;

    private static final String API_ACTION = "/telegram";

    public String enviarLogDeErros(MensagemRequest mensagem) {

        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_HUB)
                    .path(API_ACTION + "/enviarLogErros")
                    .toUriString();

            HttpHeaders headers = defaultHeaders(token.getToken());

            HttpEntity<MensagemRequest> entity =
                    new HttpEntity<>(mensagem, headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            String.class
                    );

            return response.getBody();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar log para Telegram", e);
        }
    }

    private HttpHeaders defaultHeaders(String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(bearerToken);
        return headers;
    }
}
