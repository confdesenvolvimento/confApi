package com.confApi.hub.carro;

import com.confApi.carros.dto.PesquisaRequestCarroDTO;
import com.confApi.carros.dto.PesquisaRequestCarroHub;
import com.confApi.carros.dto.PesquisaResponseCarroDTO;
import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.hub.seguro.HubSeguroClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class HubCarroClient {
    private final RestTemplate restTemplate;

    private static final Logger LOG = Logger.getLogger(HubSeguroClient.class.getName());

    @Autowired
    private ConfAppService confAppService;

    public HubCarroClient(RestTemplate hubRestTemplate) {
        this.restTemplate = hubRestTemplate;
    }

    public List<PesquisaResponseCarroDTO> pesquisarDisponibilidade(PesquisaRequestCarroDTO pesquisaRequestCarroDTO) {
        PesquisaRequestCarroHub pesquisaRequestCarroHub = new PesquisaRequestCarroHub(pesquisaRequestCarroDTO);
        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<PesquisaRequestCarroHub> entity = new HttpEntity<>(pesquisaRequestCarroHub, headers);

            // 4) Chamada (LISTA!)
            ResponseEntity<List<PesquisaResponseCarroDTO>> response = restTemplate.exchange(
                    UrlConfig.URL_CONFIANCA_HUB + "/api/carro/pesquisar",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<PesquisaResponseCarroDTO>>() {}
            );

            List<PesquisaResponseCarroDTO> body = response.getBody();

            // 5) Validação mínima
            if (response.getStatusCode().is2xxSuccessful() && body != null) {
                return body;
            }

            LOG.log(Level.WARNING,
                    "pesquisarDisponibilidade retornou status {0} sem corpo válido",
                    response.getStatusCode());

            return Collections.emptyList();

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao pesquisar disponibilidade de carros no HUB", e);
            return Collections.emptyList();
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
