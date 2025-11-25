package com.confApi.db.confManager.unidade;

import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.AbstractTransactionServiceApi;
import com.confApi.db.confManager.unidade.dto.Unidade;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.security.auth.message.AuthException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class UnidadeService extends AbstractTransactionServiceApi implements Serializable {

    private final String urlAPI = UrlConfig.URL_CONFIANCA_MANAGER +"/unidade";

    public List<Unidade> findAll() {
        List<Unidade> unidades = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String requestBody = objectMapper.writeValueAsString(null);
            String responseBody = sendHttpApiGet(urlAPI);

            if (responseBody != null && !responseBody.isEmpty()) {
                unidades = objectMapper.readValue(responseBody, new TypeReference<List<Unidade>>() {
                });

                if (unidades.isEmpty()) {
                   // message = new FacesMessage(FacesMessage.SEVERITY_WARN, "Aviso", "Erro de requisição");
                }
            } else {
               // message = new FacesMessage(FacesMessage.SEVERITY_WARN, "Aviso", "Ocorreu um erro durante o processamento da solicitação. Por favor, tente novamente mais tarde.");
            }

        } catch (JsonProcessingException ex) {
            Logger.getLogger(UnidadeService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return unidades;
    }

    public List<Unidade> findAllParamsCodgUnidade(Integer codgUnidade) {
        //Unidade unidade = null;
        List<Unidade> unidades = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlAPI)
                    .queryParam("codgUnidade", codgUnidade);
            String Url = builder.toUriString();

            String responseBody = sendHttpApiGet(Url);

            if (responseBody != null && !responseBody.isEmpty()) {
                unidades = objectMapper.readValue(responseBody, new TypeReference<List<Unidade>>() {
                });

                if (unidades.isEmpty()) {
                   // message = new FacesMessage(FacesMessage.SEVERITY_WARN, "Aviso", "Erro de requisição");
                }
            } else {
               // message = new FacesMessage(FacesMessage.SEVERITY_WARN, "Aviso", "Ocorreu um erro durante o processamento da solicitação. Por favor, tente novamente mais tarde.");
            }

        } catch (JsonProcessingException ex) {
            Logger.getLogger(UnidadeService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return unidades;
    }

    public Unidade findAllParams(Unidade codgUnidade) {
        Unidade unidade = null;
        List<Unidade> unidades = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlAPI)
                    .queryParam("idWoobaUnidade", codgUnidade.getIdWoobaUnidade());
            String Url = builder.toUriString();

            String responseBody = sendHttpApiGet(Url);

            if (responseBody != null && !responseBody.isEmpty()) {
                unidades = objectMapper.readValue(responseBody, new TypeReference<List<Unidade>>() {
                });
                for (Unidade unidade1 : unidades) {
                    unidade = unidade1;
                }

                if (unidades.isEmpty()) {
                    //  message = new FacesMessage(FacesMessage.SEVERITY_WARN, "Aviso", "Erro de requisição");
                }
            } else {
                //message = new FacesMessage(FacesMessage.SEVERITY_WARN, "Aviso", "Ocorreu um erro durante o processamento da solicitação. Por favor, tente novamente mais tarde.");
            }

        } catch (JsonProcessingException ex) {
            Logger.getLogger(UnidadeService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return unidade;
    }

    public boolean findAllParamsExite(Unidade unidade) {
        boolean existe = false;
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + new ConfAppService().token());

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlAPI)
                .queryParam("idWoobaUnidade", unidade.getIdWoobaUnidade());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<List<Unidade>> responseEntity;

        try {
            responseEntity = new RestTemplate().exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<Unidade>>() {
                    });

            List<Unidade> unidades = responseEntity.getBody();
            // Se lista não está vazia, existe
            existe = (unidades != null && !unidades.isEmpty());
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                // Trate de modo adequado, talvez lançar AuthException ou retornar false
                try {
                    throw new AuthException("Erro de autenticação: BAD_REQUEST");
                } catch (AuthException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw ex;
            }
        }
        return existe;
    }


    public Unidade create(Unidade unidade) throws JsonProcessingException {
        Unidade u = null;
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody;

        requestBody = objectMapper.writeValueAsString(unidade);
        String responseBody = sendHttpApiPost(requestBody, urlAPI);
        u = objectMapper.readValue(responseBody, Unidade.class);

        return u;
    }





}
