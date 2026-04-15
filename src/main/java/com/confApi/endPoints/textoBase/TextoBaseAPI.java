package com.confApi.endPoints.textoBase;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.textoBase.TextoBase;
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
public class TextoBaseAPI {

    private static final String API_ACTION_NAME = "textoBase";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ConfAppService confAppService;

    public List<TextoBase> findAll() {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            ConfAppResp token = confAppService.token();

            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            } else {
                return Collections.emptyList();
            }

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<List<TextoBase>> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME,
                            HttpMethod.GET,
                            requestEntity,
                            new ParameterizedTypeReference<List<TextoBase>>() {}
                    );

            return response.getBody() != null
                    ? response.getBody()
                    : Collections.emptyList();

        } catch (HttpClientErrorException ex) {
            System.out.println("Erro HTTP ao consultar seguro segurado. Status: " + ex.getStatusCode());
        } catch (Exception ex) {
            System.out.println( "Erro inesperado ao consultar seguro segurado. " + ex);
        }
        return Collections.emptyList();
    }


    public Optional<TextoBase> findById(Integer id) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        try {
            ConfAppResp token = confAppService.token();
            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            }
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<TextoBase> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME + "/findById/" + id,
                            HttpMethod.GET,
                            requestEntity,
                            TextoBase.class
                    );
            return Optional.ofNullable(response.getBody());

        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();

        } catch (HttpClientErrorException ex) {
            System.out.println("Erro HTTP ao consultar seguro segurado. Status: " + ex.getStatusCode());
        } catch (Exception ex) {
            System.out.println();
        }
        return Optional.empty();
    }

    public TextoBase save(TextoBase seguroSegurado) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            ConfAppResp token = confAppService.token();

            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            }

            HttpEntity<TextoBase> requestEntity =
                    new HttpEntity<>(seguroSegurado, headers);

            ResponseEntity<TextoBase> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME,
                            HttpMethod.POST,
                            requestEntity,
                            TextoBase.class
                    );

            return response.getBody();

        } catch (HttpClientErrorException ex) {
            System.out.println("Erro HTTP ao salvar seguro segurado. Status: " + ex.getStatusCode());
        } catch (Exception ex) {
            System.out.println();
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
                            UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME + "/" + id,
                            HttpMethod.DELETE,
                            requestEntity,
                            Void.class
                    );

            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.NotFound ex) {
            return false;
        } catch (HttpClientErrorException ex) {
            System.out.println("Erro HTTP ao consultar seguro categoria. Status: " + ex.getStatusCode());

        } catch (Exception ex) {
            System.out.println("Erro inesperado ao consultar seguro segurado" + ex);
        }

        return false;
    }
}
