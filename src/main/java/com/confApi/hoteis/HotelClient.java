package com.confApi.hoteis;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.hotel.model.HotelResponse;
import com.confApi.hoteis.model.pesquisa.HotelPesquisaModelFront;
import com.confApi.hoteis.model.reserva.CancelarReservaRequestHotelFront;
import com.confApi.hoteis.model.reserva.HotelCarregaModelFront;
import com.confApi.hoteis.model.reserva.ReservarRequestFront;
import com.confApi.hub.hotel.dto.HotelPesquisaModel;
import com.confApi.hub.hotel.dto.HotelReserva;
import com.confApi.hub.hotel.dto.ReservarRequest;
import com.confApi.hub.hotel.mapper.HotelPesquisaMapper;
import com.confApi.hub.hotel.mapper.HotelReservaMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

@Component
public class HotelClient {

    private final RestTemplate restTemplate;

    private static final String API_ACTION = "api/hotel";
    @Autowired
    private ConfAppService confAppService;

    public HotelClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Chama o HUB: POST {baseUrl}/api/hotel/disponibilidade
     */
    public List<HotelResponse> pesquisar(HotelPesquisaModelFront req) {

        try {
            // 🔹 1) Converter request do ConfAPI → modelo esperado pelo HUB
            HotelPesquisaModel hubRequest = HotelPesquisaMapper.toHub(req);
            System.out.println("URL: " + UrlConfig.URL_CONFIANCA_HUB + " - " + API_ACTION);
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_HUB)
                    .path(API_ACTION + "/disponibilidade")
                    .toUriString();
            System.out.println("URL DOIDA: " + url);
            HttpHeaders headers = defaultHeaders(token.getToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<HotelPesquisaModel> entity =
                    new HttpEntity<>(hubRequest, headers);

            // 🔹 2) Parse da resposta do HUB
            ResponseEntity<List<HotelResponse>> hubResponse =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            new ParameterizedTypeReference<List<HotelResponse>>() {
                            }
                    );

            List<HotelResponse> hoteis = hubResponse.getBody();

            // 🔹 3) Converter HUB → DTO do ConfAPI
            return hoteis;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao pesquisar hotéis no HUB", e);
        }
    }


    public HotelReserva efetuarReserva(ReservarRequestFront req) {
        try {
            // 🔹 1) Converter request do ConfAPI → modelo esperado pelo HUB
            ReservarRequest hubRequest = HotelReservaMapper.toHub(req);

            System.out.println("URL: " + UrlConfig.URL_CONFIANCA_HUB + " - " + API_ACTION);

            // 🔹 2) Token
            ConfAppResp token = confAppService.token();

            // 🔹 3) URL
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_HUB)
                    .path(API_ACTION + "/efetuarReserva") // ajuste aqui se o HUB usar outro path
                    .toUriString();

            System.out.println("URL RESERVA: " + url);

            // 🔹 4) Headers + entity
            HttpHeaders headers = defaultHeaders(token.getToken());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ReservarRequest> entity = new HttpEntity<>(hubRequest, headers);

            // 🔹 5) Chamada tipada (já retorna o objeto)
            ResponseEntity<HotelReserva> hubResponse = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    HotelReserva.class
            );

            if (!hubResponse.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Erro ao chamar HUB Reserva Hotel. HTTP " + hubResponse.getStatusCode());
            }

            HotelReserva reserva = hubResponse.getBody();
            System.out.println("Resultado efetuarReserva: " + reserva);

            return reserva;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao efetuar reserva de hotel no HUB", e);
        }
    }

    public HotelReserva carregarReserva(HotelCarregaModelFront req) {
        try {
            // 🔹 1) Converter request do ConfAPI → modelo esperado pelo HUB


            System.out.println("URL: " + UrlConfig.URL_CONFIANCA_HUB + " - " + API_ACTION);

            // 🔹 2) Token
            ConfAppResp token = confAppService.token();

            // 🔹 3) URL
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_HUB)
                    .path(API_ACTION + "/carregarReserva") // ajuste aqui se o HUB usar outro path
                    .toUriString();

            System.out.println("URL RESERVA: " + url);

            // 🔹 4) Headers + entity
            HttpHeaders headers = defaultHeaders(token.getToken());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<HotelCarregaModelFront> entity = new HttpEntity<>(req, headers);

            // 🔹 5) Chamada tipada (já retorna o objeto)
            ResponseEntity<HotelReserva> hubResponse = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    HotelReserva.class
            );

            if (!hubResponse.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Erro ao chamar HUB Reserva Hotel. HTTP " + hubResponse.getStatusCode());
            }

            HotelReserva reserva = hubResponse.getBody();
            System.out.println("Resultado efetuarReserva: " + reserva);

            return reserva;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao efetuar reserva de hotel no HUB", e);
        }
    }

    public String cancelarReserva(CancelarReservaRequestHotelFront req) {
        try {
            System.out.println("URL: " + UrlConfig.URL_CONFIANCA_HUB + " - " + API_ACTION);

            // 🔹 1) Token
            ConfAppResp token = confAppService.token();

            // 🔹 2) URL
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_HUB)
                    .path(API_ACTION + "/cancelaHotel")
                    .toUriString();

            System.out.println("URL CANCELAMENTO: " + url);

            // 🔹 3) Headers
            HttpHeaders headers = defaultHeaders(token.getToken());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 🔹 4) Entity
            HttpEntity<CancelarReservaRequestHotelFront> entity =
                    new HttpEntity<>(req, headers);

            // 🔹 5) Chamada retornando STRING
            ResponseEntity<String> hubResponse = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (!hubResponse.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException(
                        "Erro ao cancelar reserva no HUB. HTTP " + hubResponse.getStatusCode()
                );
            }

            String resposta = hubResponse.getBody();
            System.out.println("Resultado cancelarReserva: " + resposta);

            return resposta;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao cancelar reserva de hotel no HUB", e);
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
