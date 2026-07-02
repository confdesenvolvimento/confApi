package com.confApi.endPoints.clube.beneficioCartaoUsuario;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.clube.beneficioCartaoUsuario.BeneficioCartaoUsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;
import java.util.Collections;

@Service
public class BeneficioCartaoUsuarioApi implements Serializable {

    @Autowired
    private ConfAppService confAppService;

    private final RestTemplate restTemplate;

    public BeneficioCartaoUsuarioApi(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    public boolean verificaBeneficio(int cartaoUsuarioId, int beneficioId) {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("api/beneficioCartaoUsuario/verificaBeneficio/cartaoUsuario/" + cartaoUsuarioId + "/beneficio/" + beneficioId)
                    .toUriString();

            System.out.println("url: " + url);

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);

            ResponseEntity<Boolean> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Boolean.class
            );

            return Boolean.TRUE.equals(response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            return false;
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
