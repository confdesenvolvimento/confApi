package com.confApi.db.confManager.cambio;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.historicoReserva.HistoricoReservaApi;
import com.confApi.db.confManager.markup.MarkupService;
import com.confApi.db.confManager.markup.dto.Markup;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class CambioService {
    private static final Logger LOGGER = Logger.getLogger(MarkupService.class.getName());

    private final RestTemplate restTemplate;
    @Autowired
    private ConfAppService confAppService;

    private final String urlAPI ="/cambio";

    public CambioService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private HttpEntity<Object> authEntity() {
        ConfAppResp token = confAppService.token();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token.getToken());

        return new HttpEntity<>(headers);
    }

    public List<Cambio> findUltimoCambio() {

        try {
            String url = String.format("%s%s/getUltimosRegistroCambio/",
                    UrlConfig.URL_CONFIANCA_MANAGER, urlAPI);

            System.out.println("findUltimoCambio -> " + url);

            ResponseEntity<List<Cambio>> resp = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    authEntity(),
                    new ParameterizedTypeReference<List<Cambio>>() {}
            );

            return resp.getBody();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar últimos registros de câmbio", e);
            return Collections.emptyList(); // Melhor que null 👍
        }
    }


}
