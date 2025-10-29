package com.confApi.db.confManager.familia;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;

import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.familia.dto.FamiliaCompanhia;
import com.confApi.db.enumeradores.TipoConsulta;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class FamiliaService implements Serializable {

    private static final Logger LOG = Logger.getLogger(FamiliaService.class.getName());
    private static final String API_ACTION_NAME = "familiaCompanhia";
    private static final String API_ACTION_NAME_CIA = "findByCia";
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    @Autowired
    private ConfAppService confAppService;

    @Autowired
    public FamiliaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }

    /**
     * Retorna todas as famílias de companhias
     */

    public List<FamiliaCompanhia> findByNomeOuIataCia(String valor) {
        List<FamiliaCompanhia> familiaCompanhias = new ArrayList<>();
        String url = UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME+"/"+API_ACTION_NAME_CIA+"/"+valor;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(defaultHeaders()),
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                familiaCompanhias = mapper.readValue(
                        response.getBody(),
                        new TypeReference<List<FamiliaCompanhia>>() {}
                );
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Erro ao consultar famílias de companhia", ex);
        }

        return familiaCompanhias;
    }

    public List<FamiliaCompanhia> findByAll() {
        List<FamiliaCompanhia> familiaCompanhias = new ArrayList<>();
        String url = UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(defaultHeaders()),
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                familiaCompanhias = mapper.readValue(
                        response.getBody(),
                        new TypeReference<List<FamiliaCompanhia>>() {}
                );
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Erro ao consultar famílias de companhia", ex);
        }

        return familiaCompanhias;
    }

    /**
     * Retorna as famílias de companhia por tipo de rota (Nacional/Internacional)
     */
    public List<FamiliaCompanhia> findByTipoRota(Integer tipoRota) {
        List<FamiliaCompanhia> familiaCompanhias = new ArrayList<>();
        Integer idRota = tipoRota.equals(TipoConsulta.NACIONAL.getCod()) ? 1 : 2;

        String url = UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME + "/findByTipoRota/" + idRota;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(defaultHeaders()),
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                familiaCompanhias = mapper.readValue(
                        response.getBody(),
                        new TypeReference<List<FamiliaCompanhia>>() {}
                );
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Erro ao consultar famílias de companhia por tipo de rota", ex);
        }

        return familiaCompanhias;
    }

    /**
     * Cria headers com token Bearer obtido do ConfAppService
     */
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
}
