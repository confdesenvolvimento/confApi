package com.confApi.endPoints.seguro.categoria;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.seguro.categoria.SeguroCategoria;
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
public class SeguroCategoriaAPI {

    private static final Logger LOG =
            Logger.getLogger(SeguroReservaAPI.class.getName());

    private static final String API_ACTION_NAME = "seguroCategoria";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ConfAppService confAppService;

    public List<SeguroCategoria> findAll() {

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

            ResponseEntity<List<SeguroCategoria>> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME + "/findAll",
                            HttpMethod.GET,
                            requestEntity,
                            new ParameterizedTypeReference<List<SeguroCategoria>>() {}
                    );

            return response.getBody() != null
                    ? response.getBody()
                    : Collections.emptyList();

        } catch (HttpClientErrorException ex) {
            LOG.log(Level.SEVERE, "Erro HTTP ao consultar seguro categoria. Status: " + ex.getStatusCode(), ex
            );
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Erro inesperado ao consultar seguro categoria", ex);
        }
        return Collections.emptyList();
    }


    public Optional<SeguroCategoria> findById(Integer id) {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            ConfAppResp token = confAppService.token();

            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            }

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<SeguroCategoria> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME + "/findById/" + id,
                            HttpMethod.GET,
                            requestEntity,
                            SeguroCategoria.class
                    );

            return Optional.ofNullable(response.getBody());

        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();

        } catch (HttpClientErrorException ex) {
            LOG.log(Level.SEVERE,
                    "Erro HTTP ao consultar seguro categoria. Status: " + ex.getStatusCode(),
                    ex);

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Erro inesperado ao consultar seguro categoria", ex);
        }

        return Optional.empty();
    }


    public SeguroCategoria save(SeguroCategoria seguroCategoria) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            ConfAppResp token = confAppService.token();

            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            }

            HttpEntity<SeguroCategoria> requestEntity =
                    new HttpEntity<>(seguroCategoria, headers);

            ResponseEntity<SeguroCategoria> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME + "/save",
                            HttpMethod.POST,
                            requestEntity,
                            SeguroCategoria.class
                    );

            return response.getBody();

        } catch (HttpClientErrorException ex) {
            LOG.log(Level.SEVERE,
                    "Erro HTTP ao salvar seguro categoria. Status: " + ex.getStatusCode(),
                    ex);

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Erro inesperado ao salvar seguro categoria", ex);
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
            LOG.log(Level.SEVERE, "Erro inesperado ao consultar seguro categoria", ex);
        }

        return false;
    }
}
