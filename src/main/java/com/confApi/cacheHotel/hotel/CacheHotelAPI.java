package com.confApi.cacheHotel.hotel;

import com.confApi.cacheHotel.hotel.DTO.CacheHotelDTO;
import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

@Component
public class CacheHotelAPI {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ConfAppService confAppService;

    private static final String API_ACTION = "/cacheHotel";


    public List<CacheHotelDTO> gravarBusca(List<CacheHotelDTO> cacheHotelDTOS) {

        try {

            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CACHEHOTEL)
                    .path(API_ACTION + "/gravarBusca")
                    .toUriString();

            HttpHeaders headers = defaultHeaders(token.getToken());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<List<CacheHotelDTO>> entity =
                    new HttpEntity<>(cacheHotelDTOS, headers);

            ResponseEntity<List<CacheHotelDTO>> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            new ParameterizedTypeReference<List<CacheHotelDTO>>() {
                            }
                    );

            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao gravar cache de hotéis", e);
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
