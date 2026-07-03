package com.confApi.endPoints.clube.cartaoUsuarioCia;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.AbstractTransactionServiceApi;
import com.confApi.db.clube.campanha.Campanha;
import com.confApi.db.clube.cartaoUsuarioCia.CartaoUsuarioCia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class CartaoUsuarioCiaApi extends AbstractTransactionServiceApi implements Serializable {

    @Autowired
    private ConfAppService confAppService;

    private final RestTemplate restTemplate;

    public CartaoUsuarioCiaApi(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<CartaoUsuarioCia> getAllByUsuario(Integer id) {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/api/cartaoUsuarioCia/AllByCartaoUsuario/"+id).toUriString();
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);
            ResponseEntity<List<CartaoUsuarioCia>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<CartaoUsuarioCia>>() {});
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public CartaoUsuarioCia getAllById(int id) {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("api/cartaoUsuarioCia/AllById/" + id)
                    .toUriString();

          //  System.out.println("url: " + url);

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);

            ResponseEntity<CartaoUsuarioCia> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    CartaoUsuarioCia.class
            );

            return response.getBody();

        } catch (HttpClientErrorException.NotFound e) {
            return new CartaoUsuarioCia();
        } catch (Exception e) {
            e.printStackTrace();
            return new CartaoUsuarioCia();
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
