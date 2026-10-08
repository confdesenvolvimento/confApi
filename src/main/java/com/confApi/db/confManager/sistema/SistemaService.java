package com.confApi.db.confManager.sistema;

import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
public class SistemaService {
    private final RestTemplate restTemplate;
    private final ConfAppService confAppService;

    public SistemaService(RestTemplate restTemplate, ConfAppService confAppService) {
        this.restTemplate = restTemplate;
        this.confAppService = confAppService;
    }

    public List<Sistema> findByCodgProduto(Integer codgProduto) {
        String url = UriComponentsBuilder.fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER)
                .pathSegment("sistema")
                .queryParam("produto.codgProduto", codgProduto)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(confAppService.token().getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        try {
            var response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                    new ParameterizedTypeReference<List<Sistema>>() {});
            List<Sistema> sistemas = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || sistemas == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Nao foi possivel consultar os fornecedores do produto.");
            }
            return sistemas.stream()
                    .filter(sistema -> sistema != null && sistema.getProduto() != null
                            && codgProduto.equals(sistema.getProduto().getCodgProduto()))
                    .toList();
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Nao foi possivel consultar os fornecedores do produto.");
        }
    }
}
