package com.confApi.db.confManager.regraAereaReembolso;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.regraAereaReembolso.dto.RegraAereaReembolsoConsultaRequest;
import com.confApi.db.confManager.regraAereaReembolso.dto.RegraAereaReembolsoConsultaResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class RegraAereaReembolsoManagerService implements Serializable {

    private static final Logger LOG = Logger.getLogger(RegraAereaReembolsoManagerService.class.getName());
    private static final String API_ACTION_CONSULTAR = "regrasAereas/reembolso/consultar";

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    @Autowired
    private ConfAppService confAppService;

    @Autowired
    public RegraAereaReembolsoManagerService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }

    public RegraAereaReembolsoConsultaResponse consultar(RegraAereaReembolsoConsultaRequest request) {
        String url = montarUrl(API_ACTION_CONSULTAR);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request, defaultHeaders()),
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return mapper.readValue(response.getBody(), RegraAereaReembolsoConsultaResponse.class);
            }

            return fallback("ERRO_CONSULTA", "Nao foi possivel consultar regra de reembolso no manager.");
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Erro ao consultar regra aerea de reembolso", ex);
            return fallback("ERRO_CONSULTA", "Falha ao consultar regra de reembolso. A reserva foi carregada sem a analise de regra.");
        }
    }

    private String montarUrl(String action) {
        String base = UrlConfig.URL_CONFIANCA_MANAGER == null ? "" : UrlConfig.URL_CONFIANCA_MANAGER;
        if (!base.endsWith("/")) {
            base += "/";
        }
        return base + action;
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ConfAppResp token = confAppService.token();
            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Falha ao obter token para consultar regra aerea de reembolso", ex);
        }

        return headers;
    }

    private RegraAereaReembolsoConsultaResponse fallback(String status, String mensagem) {
        RegraAereaReembolsoConsultaResponse response = new RegraAereaReembolsoConsultaResponse();
        response.setStatus(status);
        response.setMensagem(mensagem);
        return response;
    }
}
