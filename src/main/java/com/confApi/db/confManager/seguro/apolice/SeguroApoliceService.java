package com.confApi.db.confManager.seguro.apolice;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class SeguroApoliceService {

    private static final Logger LOG = Logger.getLogger(SeguroApoliceService.class.getName());
    private static final String API_ACTION_NAME = "seguroApolice";
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    @Autowired
    private ConfAppService confAppService;

    @Autowired
    public SeguroApoliceService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ConfAppResp token = confAppService.token();
            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            } else {
                LOG.warning("Token de autenticação não encontrado no ConfAppService.");
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Falha ao obter token de autenticação do ConfAppService", ex);
        }

        return headers;
    }


    public List<SeguroApolice> findAll() {
        List<SeguroApolice> seguroApolice = new ArrayList<>();
        String url = UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME + "/findAll";

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(defaultHeaders()),
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                seguroApolice = mapper.readValue(
                        response.getBody(),
                        new TypeReference<List<SeguroApolice>>() {}
                );
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Erro ao consultar seguro apolice", ex);
        }

        return seguroApolice;
    }

}
