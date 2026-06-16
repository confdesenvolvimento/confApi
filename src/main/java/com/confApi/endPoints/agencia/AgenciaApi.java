package com.confApi.endPoints.agencia;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.AbstractTransactionServiceApi;

import com.confApi.db.confManager.agencia.dto.Agencia;
import com.confApi.util.TelegramErrorAlert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class AgenciaApi extends AbstractTransactionServiceApi implements Serializable {

    private static final Logger LOG = Logger.getLogger(AgenciaApi.class.getName());

    @Autowired
    private ConfAppService confAppService;

    @Autowired(required = false)
    private TelegramErrorAlert telegramErrorAlert;

    private final RestTemplate restTemplate;

    public AgenciaApi(RestTemplate restTemplate) {
            this.restTemplate = restTemplate;
        }

    public com.confApi.db.confManager.agencia.dto.Agencia findByIdWoobaAgencia(Integer idWoobaAgencia) {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER)
                    .path("/agencia/findByIdWoobaAgencia/" + idWoobaAgencia)
                    .toUriString();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);

            ResponseEntity<com.confApi.db.confManager.agencia.dto.Agencia> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    com.confApi.db.confManager.agencia.dto.Agencia.class
            );

            return response.getBody();

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao buscar agencia por id Wooba: " + idWoobaAgencia, e);
            alertarErro("Erro ao buscar agencia por id Wooba " + idWoobaAgencia, e);
            return new Agencia();
        }
    }

    public com.confApi.db.confManager.agencia.dto.Agencia findByCodgSistemaBackOffice(String codgSistemaBackOffice) {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER)
                    .path("/agencia/findByNameAgenciaDTOid/{codgSistemaBackOffice}")
                    .buildAndExpand(codgSistemaBackOffice)
                    .toUriString();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);

            ResponseEntity<com.confApi.db.confManager.agencia.dto.Agencia> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    com.confApi.db.confManager.agencia.dto.Agencia.class
            );

            return response.getBody();

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao buscar agencia por codg_sistema_backoffice: " + codgSistemaBackOffice, e);
            alertarErro("Erro ao buscar agencia por codg_sistema_backoffice " + codgSistemaBackOffice, e);
            return new Agencia();
        }
    }

    private void alertarErro(String mensagem, Exception e) {
        if (telegramErrorAlert != null) {
            telegramErrorAlert.enviar(this, mensagem, e);
        }
    }

    private HttpHeaders defaultHeaders(String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(bearerToken);
        return headers;
    }

}
