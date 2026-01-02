package com.confApi.hub.hotel.client;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.hotel.model.HotelResponse;
import com.confApi.hub.hotel.dto.*;
import com.confApi.hub.limites.LimitesService;
import com.confApi.hub.limites.dto.Disponibilidade;
import com.confApi.hub.limites.dto.LimiteCreditoRQ;
import com.confApi.hub.limites.dto.StatusResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class HubHotelClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final String baseUrl;      // ex: https://hub.../
    private final String hotelPath;    // ex: api/hotel


    private static final Logger LOG = Logger.getLogger(HubHotelClient.class.getName());
    @Autowired
    private ConfAppService confAppService;

    public HubHotelClient(RestTemplate hubRestTemplate,
                          ObjectMapper mapper,
                          @Value("${hub.base-url}") String baseUrl,
                          @Value("${hub.hotel-path:api/hotel}") String hotelPath) {
        this.restTemplate = hubRestTemplate;
        this.mapper = mapper;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.hotelPath = hotelPath.startsWith("/") ? hotelPath.substring(1) : hotelPath;
    }

    public List<HotelResponse> pesquisarDisponibilidade(HotelPesquisaModel hotelPesquisa) {
        try {
            // 1) Token
            ConfAppResp token = confAppService.token();
            System.out.println("URL: " + UrlConfig.URL_CONFIANCA_HUB + " - " + hotelPath);
            // 2) URL
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_HUB)
                    .path(hotelPath) // ex: /api/hotel/disponibilidade
                    .toUriString();

            // 3) Headers + body
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<HotelPesquisaModel> entity = new HttpEntity<>(hotelPesquisa, headers);

            // 4) Chamada (LISTA!)
            ResponseEntity<List<HotelResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<HotelResponse>>() {}
            );

            List<HotelResponse> body = response.getBody();

            // 5) Validação mínima
            if (response.getStatusCode().is2xxSuccessful() && body != null) {
                return body;
            }

            LOG.log(Level.WARNING,
                    "pesquisarDisponibilidade retornou status {0} sem corpo válido",
                    response.getStatusCode());

            return Collections.emptyList();

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao pesquisar disponibilidade de hotéis no HUB", e);
            return Collections.emptyList();
        }
    }


    public List<HotelResponse> pesquisarDisponibilidade2(HotelPesquisaModel hotelPesquisa) {
        String url = baseUrl + hotelPath + "/disponibilidade";
        String body = postJson(url, hotelPesquisa);
        try {
            return mapper.readValue(body, new TypeReference<List<HotelResponse>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Falha ao parsear resposta HUB (disponibilidade): " + e.getMessage(), e);
        }
    }

    public HotelReserva efetuarReserva(ReservarRequest request) {
        String url = baseUrl + hotelPath + "/efetuarReserva";
        String body = postJson(url, request);
        try {
            return mapper.readValue(body, new TypeReference<HotelReserva>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Falha ao parsear resposta HUB (efetuarReserva): " + e.getMessage(), e);
        }
    }

    public HotelReserva carregarReserva(HotelCarregaModel req) {
        String url = baseUrl + hotelPath + "/carregarReserva";
        String body = postJson(url, req);

        // o seu código tinha uma heurística de erro: status+error no JSON
        if (body != null && body.contains("\"status\":") && body.contains("\"error\":")) {
            return null;
        }

        try {
            return mapper.readValue(body, new TypeReference<HotelReserva>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Falha ao parsear resposta HUB (carregarReserva): " + e.getMessage(), e);
        }
    }

    public String cancelarReserva(HotelCancelarModel request) {
        String url = baseUrl + hotelPath + "/cancelaHotel";
        return postJson(url, request);
    }

    private String postJson(String url, Object payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Se o HUB exigir auth, você injeta aqui (Bearer, api-key, etc.)
        // headers.setBearerAuth(token);

        HttpEntity<Object> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> resp = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
        );

        return resp.getBody();
    }
    private HttpHeaders defaultHeaders(String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(bearerToken);
        return headers;
    }



    private StatusResponse defaultStatusFalha(String msg) {
        StatusResponse s = new StatusResponse();
        s.setStatusCode(1);
        s.setMessage(msg);
        return s;
    }
}