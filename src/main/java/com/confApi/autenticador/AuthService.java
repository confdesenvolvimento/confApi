package com.confApi.autenticador;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.AbstractTransactionServiceApi;
import com.confApi.db.confManager.seguro.apolice.SeguroApolice;
import com.confApi.db.confManager.unidade.dto.Unidade;
import com.confApi.endPoints.seguro.reserva.SeguroReservaAPI;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class AuthService extends AbstractTransactionServiceApi {

    private static final Logger LOG =
            Logger.getLogger(SeguroReservaAPI.class.getName());

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ConfAppService confAppService;

    private final String urlAPI = "http://localhost:8082" +"/autenticacaoplantao";

    public AuthResponse validate(AuthRequest request) throws JsonProcessingException {
        if (request == null || request.login() == null || request.token() == null) {
            return AuthResponse.invalid();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            ConfAppResp token = confAppService.token();

            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            }

            HttpEntity<AuthRequest> requestEntity =
                    new HttpEntity<>(request, headers);

            ResponseEntity<AuthResponse> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER + "/autenticacaoplantao",
                            HttpMethod.POST,
                            requestEntity,
                            AuthResponse.class
                    );

            return response.getBody();

        } catch (HttpClientErrorException ex) {
            LOG.log(Level.SEVERE,
                    "Erro HTTP ao salvar seguro segurado. Status: " + ex.getStatusCode(),
                    ex);
            return AuthResponse.invalid();

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Erro inesperado ao salvar seguro apolice", ex);
            return AuthResponse.invalid();
        }
    }
}
