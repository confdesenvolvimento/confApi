package com.confApi.hoteis;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.hotel.model.HotelResponse;
import com.confApi.db.confManager.reservaHotel.dto.ReservaHotel;
import com.confApi.hoteis.model.pesquisa.HotelPesquisaModelFront;
import com.confApi.hoteis.model.reserva.CancelarReservaRequestHotelFront;
import com.confApi.hoteis.model.reserva.HotelCarregaModelFront;
import com.confApi.hoteis.model.reserva.ReservaHotelAtualizarReservaRQ;
import com.confApi.hoteis.model.reserva.ReservarRequestFront;
import com.confApi.hub.hotel.dto.HotelPesquisaModel;
import com.confApi.hub.hotel.dto.HotelReserva;
import com.confApi.hub.hotel.dto.ReservarRequest;
import com.confApi.hub.hotel.mapper.HotelPesquisaMapper;
import com.confApi.hub.hotel.mapper.HotelReservaMapper;
import com.confApi.hub.telegram.TelegramService;
import com.confApi.hub.telegram.dto.MensagemRequest;
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

    @Autowired
    public TelegramService telegramService;

    public HotelClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Chama o HUB: POST {baseUrl}/api/hotel/disponibilidade
     */
    public List<HotelResponse> pesquisar(HotelPesquisaModelFront req) {
        try {
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

            ResponseEntity<List<HotelResponse>> hubResponse =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            new ParameterizedTypeReference<List<HotelResponse>>() {
                            }
                    );

            List<HotelResponse> hoteis = hubResponse.getBody();
            return hoteis;
        } catch (Exception e) {
            e.printStackTrace();
            logErro("Erro ao pesquisar hotéis", e);
            throw new RuntimeException("Erro ao pesquisar hotéis no HUB", e);
        }
    }


    public HotelReserva efetuarReserva(ReservarRequestFront req) {
        try {
            ReservarRequest hubRequest = HotelReservaMapper.toHub(req);
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_HUB)
                    .path(API_ACTION + "/efetuarReserva") // ajuste aqui se o HUB usar outro path
                    .toUriString();

            HttpHeaders headers = defaultHeaders(token.getToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ReservarRequest> entity = new HttpEntity<>(hubRequest, headers);

            ResponseEntity<HotelReserva> hubResponse = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    HotelReserva.class
            );

            if (!hubResponse.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Erro ao chamar HUB Reserva Hotel. HTTP " + hubResponse.getStatusCode());
            }
            return hubResponse.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            logErro("Erro ao efetuar reserva hotel", e);
            throw new RuntimeException("Erro ao efetuar reserva de hotel no HUB", e);
        }
    }

    public HotelReserva carregarReserva(HotelCarregaModelFront req) {
        try {
            System.out.println("HotelCarregaModelFront: " + req);
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_HUB)
                    .path(API_ACTION + "/carregarReserva") // ajuste aqui se o HUB usar outro path
                    .toUriString();

            System.out.println("URL RESERVA: " + url);

            HttpHeaders headers = defaultHeaders(token.getToken());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<HotelCarregaModelFront> entity = new HttpEntity<>(req, headers);

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
            System.out.println("Resultado consulda API: " + reserva);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<ReservaHotel> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER + "/reservaHotel/localizador/" + req.getIdentificador(),
                            HttpMethod.GET,
                            requestEntity,
                            ReservaHotel.class
                    );

            ReservaHotel reservaHotel = response.getBody();
            System.out.println("Resultado consulta db: " + reservaHotel.getStatus());

            String statusStr = reserva.getReservasHotelRsList().get(0).getStatus();

            int status = 0;

            if (statusStr != null) {
                if (statusStr.contains("Cancel")) {
                    status = 2;
                } else if (statusStr.contains("Rejected")) {
                    status = 3;
                } else if (statusStr.contains("Confirmed")) {
                    status = 1;
                }
            }

            ReservaHotelAtualizarReservaRQ reservaHotelAtualizarReservaRQ =
                    new ReservaHotelAtualizarReservaRQ(reservaHotel.getCodgReservaHotel(), status);

            HttpEntity<ReservaHotelAtualizarReservaRQ> requestEntity1 =
                    new HttpEntity<>(reservaHotelAtualizarReservaRQ, headers);

            ResponseEntity<?> responseUpdate =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER + "/reservaHotel/atualizarReserva/"
                                    + reservaHotel.getCodgReservaHotel(),
                            HttpMethod.PUT,
                            requestEntity1,
                            Object.class
                    );

            return reserva;

        } catch (Exception e) {
            e.printStackTrace();
            logErro("Erro ao carregar reserva hotel", e);
            throw new RuntimeException("Erro ao efetuar reserva de hotel no HUB", e);
        }
    }

    public String cancelarReserva(CancelarReservaRequestHotelFront req) {
        try {
            System.out.println("URL: " + UrlConfig.URL_CONFIANCA_HUB + " - " + API_ACTION);
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_HUB)
                    .path(API_ACTION + "/cancelaHotel")
                    .toUriString();

            System.out.println("URL CANCELAMENTO: " + url);
            HttpHeaders headers = defaultHeaders(token.getToken());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<CancelarReservaRequestHotelFront> entity =
                    new HttpEntity<>(req, headers);

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
            logErro("Erro ao cancelar reserva hotel", e);
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

    private void logErro(String mensagem, Exception e) {
        MensagemRequest msg = new MensagemRequest(mensagem + ": " + e.getMessage());
        msg.setMetodo(Thread.currentThread().getStackTrace()[2].getMethodName());
        msg.setClasse(this.getClass().getSimpleName());
        msg.setProjeto("CONFAPI");

        telegramService.enviarLogDeErros(msg);
    }

}
