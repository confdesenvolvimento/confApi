package com.confApi.cacheHotel.cidade;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

@Component
public class CacheHotelCidadeAPI {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ConfAppService confAppService;

    private static final String API_ACTION = "/cidadeHtl";

    private HttpHeaders defaultHeaders(String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(bearerToken);
        return headers;
    }

    public CacheHotelCidade findFirstByCodeCidade(String codeCidade) {

        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CACHEHOTEL)
                    .path(API_ACTION + "/findFirstByCodeCidade/"+codeCidade)
                    .buildAndExpand(codeCidade)
                    .toUriString();

            HttpHeaders headers = defaultHeaders(token.getToken());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<CacheHotelCidade> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            CacheHotelCidade.class
                    );

            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao buscar cidade por código", e);
        }
    }

    public List<CacheHotelCidade> searchCidades(String query) {
        String termo = query == null ? "" : query.trim();
        List<CacheHotelCidade> cidades = searchCidadesNoCache(termo);
        if (!cidades.isEmpty() || !termo.contains(" ")) {
            return cidades;
        }

        // Compatibilidade com bancos que falham ao pesquisar prefixos com mais
        // de uma palavra (por exemplo, "sao p"). O ConfAPI filtra o resultado
        // completo depois, portanto não expõe cidades extras.
        String primeiroTermo = termo.split("\\s+")[0];
        return searchCidadesNoCache(primeiroTermo);
    }

    private List<CacheHotelCidade> searchCidadesNoCache(String query) {
        ConfAppResp token = confAppService.token();
        String url = UriComponentsBuilder
                .fromHttpUrl(UrlConfig.URL_CONFIANCA_CACHEHOTEL)
                .path(API_ACTION + "/search")
                .queryParam("query", query)
                .toUriString();

        HttpHeaders headers = defaultHeaders(token.getToken());
        ResponseEntity<List<CacheHotelCidade>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new org.springframework.core.ParameterizedTypeReference<List<CacheHotelCidade>>() {
                }
        );

        return response.getBody() == null ? Collections.emptyList() : response.getBody();
    }

}
