package com.confApi.db.confManager.novidade;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.AbstractTransactionServiceApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class NovidadeApi extends AbstractTransactionServiceApi implements Serializable {

    private final String urlAPI = "novidade";

    public List<Novidade> findByAll() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String responseBody = sendHttpApiGet(UrlConfig.URL_CONFIANCA_MANAGER + urlAPI);

            //System.out.println("ResponseBody: [" + responseBody + "]");

            if (responseBody == null || responseBody.isBlank()) {
                return new ArrayList<>();
            }

            return objectMapper.readValue(responseBody, new TypeReference<List<Novidade>>() {});
        } catch (JsonProcessingException ex) {
            Logger.getLogger(NovidadeApi.class.getName()).log(Level.SEVERE, "Erro ao converter JSON", ex);
            return new ArrayList<>();
        } catch (Exception ex) {
            Logger.getLogger(NovidadeApi.class.getName()).log(Level.SEVERE, "Erro ao buscar novidades", ex);
            return new ArrayList<>();
        }
    }

    public List<Novidade> findFiltros() {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            RestTemplate restTemplate = new RestTemplate();
            ConfAppService confAppService = new ConfAppService();
            ConfAppResp token = confAppService.token();

            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            } else {
                return Collections.emptyList();
            }

            HttpEntity<Novidade> requestEntity =
                    new HttpEntity<>(null, headers);

           // System.out.println("REQUEST" + requestEntity);

            ResponseEntity<List<Novidade>> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER + "/novidade",
                            HttpMethod.GET,
                            requestEntity,
                            new ParameterizedTypeReference<List<Novidade>>() {
                            }
                    );

            return response.getBody() != null
                    ? response.getBody()
                    : Collections.emptyList();
        } catch (Exception ex) {
            System.out.println("Erro inesperado ao consultar seguro reserva");
            System.out.println("Erro: " + ex.getMessage() + " - StackTrace: " + ex.getStackTrace());
        }
        return Collections.emptyList();
    }

    public Novidade findById(Integer codgNovidade) {
        Novidade novidade = null;
        ObjectMapper objectMapper = new ObjectMapper();
        String responseBody = sendHttpApiGet(UrlConfig.URL_CONFIANCA_MANAGER + urlAPI + "/" + codgNovidade);
        try {
            novidade = objectMapper.readValue(responseBody, Novidade.class);
        } catch (JsonProcessingException ex) {
            Logger.getLogger(Novidade.class.getName()).log(Level.SEVERE, null, ex);
        }
        return novidade;
    }

    public Novidade salvar(Novidade novidade, MultipartFile multipartFile) throws JsonProcessingException {
        System.out.println("NovidadeController.insertNotificacaoControle: " + novidade);
        ObjectMapper objectMapper = new ObjectMapper();
        RestTemplate restTemplate = new RestTemplate();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            ConfAppService confAppService = new ConfAppService();
            ConfAppResp token = confAppService.token();

            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            }

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            String novidadeJson = objectMapper.writeValueAsString(novidade);

            HttpHeaders novidadeHeaders = new HttpHeaders();
            novidadeHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> novidadePart = new HttpEntity<>(novidadeJson, novidadeHeaders);
            body.add("novidade", novidadePart);

            if (multipartFile != null && !multipartFile.isEmpty()) {
                ByteArrayResource resource = new ByteArrayResource(multipartFile.getBytes()) {
                    @Override
                    public String getFilename() {
                        return multipartFile.getOriginalFilename();
                    }
                };

                HttpHeaders arquivoHeaders = new HttpHeaders();
                arquivoHeaders.setContentType(
                        multipartFile.getContentType() != null
                                ? MediaType.parseMediaType(multipartFile.getContentType())
                                : MediaType.APPLICATION_OCTET_STREAM
                );

                HttpEntity<ByteArrayResource> arquivoPart = new HttpEntity<>(resource, arquivoHeaders);
                body.add("arquivo", arquivoPart);
            }

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

          //  System.out.println("TESTE: " + body);

            ResponseEntity<String> response = restTemplate.exchange(
                    UrlConfig.URL_CONFIANCA_MANAGER + urlAPI,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            return objectMapper.readValue(response.getBody(), Novidade.class);

        } catch (IOException ex) {
            Logger.getLogger(NovidadeApi.class.getName()).log(Level.SEVERE, "Erro ao montar multipart", ex);
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            Logger.getLogger(NovidadeApi.class.getName()).log(Level.SEVERE, "Erro ao salvar novidade", ex);
            throw ex;
        }
    }

    public Novidade update(Novidade novidade, Integer codgNovidade) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        try {
            RestTemplate restTemplate = new RestTemplate();
            ConfAppService confAppService = new ConfAppService();
            ConfAppResp token = confAppService.token();
            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            } else {
                return null;
            }

            HttpEntity<Novidade> requestEntity = new HttpEntity<>(novidade, headers);

            ResponseEntity<Novidade> response = restTemplate.exchange(
                    UrlConfig.URL_CONFIANCA_MANAGER + "/novidade/" + codgNovidade,
                    HttpMethod.PUT,
                    requestEntity,
                    Novidade.class
            );

         //  System.out.println("Update Status: " + response.getStatusCode());
            return response.getBody();

        } catch (Exception ex) {
            System.out.println("Erro ao atualizar novidade: " + ex.getMessage());
            return null;
        }
    }

    public String delete(Integer codgNovidade) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        try {
            RestTemplate restTemplate = new RestTemplate();
            ConfAppService confAppService = new ConfAppService();
            ConfAppResp token = confAppService.token();
            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            } else {
                return "Erro ao obter token.";
            }

            HttpEntity<Void> requestEntity = new HttpEntity<>(null, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    UrlConfig.URL_CONFIANCA_MANAGER + "/novidade/" + codgNovidade,
                    HttpMethod.DELETE,
                    requestEntity,
                    String.class
            );

          // System.out.println("Delete Status: " + response.getStatusCode());
            return null; // sucesso

        } catch (Exception ex) {
            System.out.println("Erro ao deletar novidade: " + ex.getMessage());
            return ex.getMessage();
        }
    }
}