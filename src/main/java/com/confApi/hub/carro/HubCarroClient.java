package com.confApi.hub.carro;

import com.confApi.carros.dto.*;
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

    public List<PesquisaCarroResponseDTO> pesquisarDisponibilidade(PesquisaCarroRequestDTO pesquisaRequestCarroDTO) {
        PesquisaCarroRequestHub pesquisaRequestCarroHub = new PesquisaCarroRequestHub(pesquisaRequestCarroDTO);

        try {
            ConfAppResp token = confAppService.token();
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<PesquisaCarroRequestHub> entity = new HttpEntity<>(pesquisaRequestCarroHub, headers);

            ResponseEntity<List<PesquisaCarroResponseDTO>> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_HUB + "api/carro/pesquisar",
                            HttpMethod.POST,
                            entity,
                            new ParameterizedTypeReference<List<PesquisaCarroResponseDTO>>() {}
                    );

            List<PesquisaCarroResponseDTO> body = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && body != null) {
                return body;
            }
            LOG.log(Level.WARNING,"pesquisarDisponibilidade retornou status {0} sem corpo válido", response.getStatusCode());
            return null;

        } catch (Exception e) {
            LOG.log(Level.SEVERE,"Erro ao pesquisar disponibilidade de carros no HUB", e);
            return null;
        }
    }

    public List<SelecionarCarroResponseDTO> selecionarCarro(SelecionarCarroRequestDTO selecionarCarroRequestDTO) {
        SelecionarCarroRequestHub selecionarCarroRequestHub = new SelecionarCarroRequestHub(selecionarCarroRequestDTO);

        try {
            ConfAppResp token = confAppService.token();
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<SelecionarCarroRequestHub> entity = new HttpEntity<>(selecionarCarroRequestHub, headers);

            ResponseEntity<List<SelecionarCarroResponseDTO>> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_HUB + "api/carro/selecionarCarro",
                            HttpMethod.POST,
                            entity,
                            new ParameterizedTypeReference<List<SelecionarCarroResponseDTO>>() {}
                    );

            List<SelecionarCarroResponseDTO> body = response.getBody();

            System.out.println("body: " + body);

            if (response.getStatusCode().is2xxSuccessful() && body != null) {
                return body;
            }
            LOG.log(Level.WARNING,"pesquisarDisponibilidade retornou status {0} sem corpo válido", response.getStatusCode());
            return null;

        } catch (Exception e) {
            LOG.log(Level.SEVERE,"Erro ao pesquisar disponibilidade de carros no HUB", e);
            return null;
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
