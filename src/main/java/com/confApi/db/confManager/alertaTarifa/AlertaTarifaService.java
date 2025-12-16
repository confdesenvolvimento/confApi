package com.confApi.db.confManager.alertaTarifa;
import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;

import com.confApi.db.confManager.alertaTarifa.dto.AlertaTarifaDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class AlertaTarifaService implements Serializable {

    private static final Logger LOG = Logger.getLogger(AlertaTarifaService.class.getName());

    private static final String API_ACTION_NAME = "alertas";


    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    @Autowired
    private ConfAppService confAppService;

    @Autowired
    public AlertaTarifaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Se necessário: mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /** POST /alertas */
    public AlertaTarifaDTO salvar(AlertaTarifaDTO dto) {
        try {
            String body = mapper.writeValueAsString(dto);

            ResponseEntity<String> resp = restTemplate.exchange(
                    UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME,
                    HttpMethod.POST,
                    new HttpEntity<>(body, defaultHeaders()),
                    String.class
            );

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return mapper.readValue(resp.getBody(), AlertaTarifaDTO.class);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao salvar alerta", e);
        }
        return null;
    }

    /** GET /alertas/usuario/{codgUsuario} (detalhado) */
    public List<AlertaTarifaDTO> listarPorUsuarioDetalhado(Integer codgUsuario) {
        String url = UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME + "/usuario/" + codgUsuario;
        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(defaultHeaders()),
                    String.class
            );
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {

                return mapper.readValue(resp.getBody(), new TypeReference<List<AlertaTarifaDTO>>() {});
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao buscar alertas detalhados do usuário", e);
        }
        return Collections.emptyList();
    }

    /** PUT /alertas/desativar/{id} */
    public boolean desativar(Integer id) {
        String url = UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME + "/desativar/" + id;
        try {
            ResponseEntity<Void> resp = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>("", defaultHeaders()),
                    Void.class
            );
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao desativar alerta", e);
            return false;
        }
    }

    /** GET /alertas/ativos */
    public List<AlertaTarifaDTO> listarAtivos() {
        String url = UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME + "/ativos";
        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(defaultHeaders()),
                    String.class
            );
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return mapper.readValue(resp.getBody(), new TypeReference<List<AlertaTarifaDTO>>() {});
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao listar alertas ativos", e);
        }
        return new ArrayList<>();
    }

    /** GET /alertas/usuario/{codgUsuario} */
    public List<AlertaTarifaDTO> listarPorUsuario(Integer codgUsuario) {
        String url = UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME + "/usuario/" + codgUsuario;
        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(defaultHeaders()),
                    String.class
            );
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {

                return mapper.readValue(resp.getBody(), new TypeReference<List<AlertaTarifaDTO>>() {});
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao buscar alertas por usuário", e);
        }
        return new ArrayList<>();
    }

    /** GET /alertas/agencia/{codgAgencia} */
    public List<AlertaTarifaDTO> listarPorAgencia(Integer codgAgencia) {
        String url = UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_NAME + "/agencia/" + codgAgencia;
        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(defaultHeaders()),
                    String.class
            );
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return mapper.readValue(resp.getBody(), new TypeReference<List<AlertaTarifaDTO>>() {});
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao buscar alertas por agência", e);
        }
        return new ArrayList<>();
    }

    /** Headers com Bearer Token do ConfApp */
    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            ConfAppResp token = confAppService.token();
            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            } else {
                LOG.warning("Token não encontrado ao montar headers do AlertaTarifaService.");
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Falha ao obter token do ConfAppService", ex);
        }

        return headers;
    }
}
