package com.confApi.endPoints.clube.beneficioTransacaoCartao;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.clube.beneficioTransacaoCartao.BeneficioTransacaoCartao;
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
public class BeneficioTransacaoCartaoApi implements Serializable {

    @Autowired
    private ConfAppService confAppService;

    private final RestTemplate restTemplate;

    public BeneficioTransacaoCartaoApi(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    public List<BeneficioTransacaoCartao> getAllByTransacaoUsuarioExtrato(int cartaoUsuarioId) {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("api/beneficioTransacaoCartao/transacaoUsuarioExtrato/cartaoUsuario/" + cartaoUsuarioId)
                    .toUriString();

            System.out.println("url: " + url);

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);

            ResponseEntity<List<BeneficioTransacaoCartao>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<BeneficioTransacaoCartao>>() {}
            );

            return response.getBody();

        } catch (HttpClientErrorException.NotFound e) {
            return new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
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
