package com.confApi.endPoints.reservaAereo;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.hub.aereo.ConsultarLocalizadorRequestHub;
import com.confApi.hub.aereo.ConsultarLocalizadorResponseHub;
import com.confApi.hub.aereo.PesquisaRequestAereoHub;
import com.confApi.hub.aereo.PesquisaResponseHub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ReservaAereoApi {

    private static final Logger LOG = Logger.getLogger(ReservaAereoApi.class.getName());

    @Autowired
    private ConfAppService confAppService;

    private final RestTemplate restTemplate;

    public ReservaAereoApi(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ConsultarLocalizadorResponseHub reservaAereoConsultaLocalizadorHub(ConsultarLocalizadorRequestHub consultarLocalizadorRequestHub) {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_HUB)
                    .path("api/aereo/consultar")
                    .toUriString();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<ConsultarLocalizadorRequestHub> entity =
                    new HttpEntity<>(consultarLocalizadorRequestHub, headers);

            ResponseEntity<ConsultarLocalizadorResponseHub> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ConsultarLocalizadorResponseHub>() {
                    }
            );

            return response.getBody();
        } catch (Exception e) {
            ConsultarLocalizadorResponseHub erro = new ConsultarLocalizadorResponseHub();
            erro.setException(e.getMessage());
            return erro;
        }
    }

    public ReservaAereo reservaAereoConsultaLocalizadorDb(String localizador) {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER)
                    .path("/reservaAereo/localizador/" + localizador)
                    .toUriString();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<ReservaAereo> entity =
                    new HttpEntity<>(null, headers);

            ResponseEntity<ReservaAereo> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ReservaAereo>() {
                    }
            );

            return response.getBody();
        } catch (Exception e) {
            ReservaAereo erro = new ReservaAereo();
            System.out.println(e.getMessage());
            return erro;
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

