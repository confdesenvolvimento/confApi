package com.confApi.endPoints.seguro.segurado;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.seguro.segurado.SeguroSegurado;
import com.confApi.endPoints.seguro.reserva.SeguroReservaAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class SeguroSeguradoAPI {

    private static final Logger LOG =
            Logger.getLogger(SeguroReservaAPI.class.getName());

    private static final String API_ACTION_NAME = "seguroSegurado";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ConfAppService confAppService;

    public List<SeguroSegurado> findAll() {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            ConfAppResp token = confAppService.token();

            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            } else {
                LOG.warning("Token de autenticação não obtido do ConfAppService.");
                return Collections.emptyList();
            }

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<List<SeguroSegurado>> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME + "/findAll",
                            HttpMethod.GET,
                            requestEntity,
                            new ParameterizedTypeReference<List<SeguroSegurado>>() {}
                    );

            return response.getBody() != null
                    ? response.getBody()
                    : Collections.emptyList();

        } catch (HttpClientErrorException ex) {
            LOG.log(Level.SEVERE, "Erro HTTP ao consultar seguro segurado. Status: " + ex.getStatusCode(), ex
            );
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Erro inesperado ao consultar seguro segurado", ex);
        }
        return Collections.emptyList();
    }


    public Optional<SeguroSegurado> findById(Integer id) {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            ConfAppResp token = confAppService.token();

            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            }

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<SeguroSegurado> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME + "/findById/" + id,
                            HttpMethod.GET,
                            requestEntity,
                            SeguroSegurado.class
                    );

            return Optional.ofNullable(response.getBody());

        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();

        } catch (HttpClientErrorException ex) {
            LOG.log(Level.SEVERE,
                    "Erro HTTP ao consultar seguro segurado. Status: " + ex.getStatusCode(),
                    ex);

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Erro inesperado ao consultar seguro segurado", ex);
        }

        return Optional.empty();
    }


    public SeguroSegurado save(SeguroSegurado seguroSegurado) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            ConfAppResp token = confAppService.token();

            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            }

            HttpEntity<SeguroSegurado> requestEntity =
                    new HttpEntity<>(seguroSegurado, headers);

            ResponseEntity<SeguroSegurado> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME + "/save",
                            HttpMethod.POST,
                            requestEntity,
                            SeguroSegurado.class
                    );

            return response.getBody();

        } catch (HttpClientErrorException ex) {
            LOG.log(Level.SEVERE,
                    "Erro HTTP ao salvar seguro segurado. Status: " + ex.getStatusCode(),
                    ex);

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Erro inesperado ao salvar seguro segurado", ex);
        }

        return null;
    }

    public List<SeguroSegurado> saveAll(List <SeguroSegurado> seguroSegurados) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            ConfAppResp token = confAppService.token();

            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            }

            HttpEntity<List<SeguroSegurado>> requestEntity =
                    new HttpEntity<>(seguroSegurados, headers);

            ResponseEntity<List<SeguroSegurado>> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME + "/saveAll",
                            HttpMethod.POST,
                            requestEntity,
                            new ParameterizedTypeReference<List<SeguroSegurado>>() {}
                    );

            return response.getBody();

        } catch (HttpClientErrorException ex) {
            LOG.log(Level.SEVERE,
                    "Erro HTTP ao salvar seguro segurado. Status: " + ex.getStatusCode(),
                    ex);

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Erro inesperado ao salvar seguro segurado", ex);
        }

        return null;
    }


    public boolean deleteById(Integer id) {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            ConfAppResp token = confAppService.token();

            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            }

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Void> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME + "/deleteById/" + id,
                            HttpMethod.DELETE,
                            requestEntity,
                            Void.class
                    );

            return response.getStatusCode().is2xxSuccessful();

        } catch (HttpClientErrorException.NotFound ex) {
            return false;

        } catch (HttpClientErrorException ex) {
            LOG.log(Level.SEVERE,
                    "Erro HTTP ao consultar seguro categoria. Status: " + ex.getStatusCode(),
                    ex);

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Erro inesperado ao consultar seguro segurado", ex);
        }

        return false;
    }
}
