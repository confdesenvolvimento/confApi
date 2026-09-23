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
            if (reserva == null || reservaHotel == null
                    || reservaHotel.getCodgReservaHotel() == null
                    || reserva.getReservasHotelRsList() == null
                    || reserva.getReservasHotelRsList().isEmpty()) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Não foi possível validar a reserva retornada pelo fornecedor.");
            }
            Integer status = null;
            for (var item : reserva.getReservasHotelRsList()) {
                if (item == null || !java.util.Objects.equals(item.getIdentificador(), reservaHotel.getLocalizador())) {
                    throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "O localizador retornado pelo fornecedor não corresponde à reserva salva.");
                }
                int atual = StatusReservaHotelFornecedor.codigo(item.getStatus());
                if (status != null && status != atual) {
                    throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "A reserva possui status divergentes; o status salvo foi preservado.");
                }
                status = atual;
                item.setStatus(StatusReservaHotelFornecedor.canonico(atual));
            }

            // O status financeiro é mantido pelo Manager; confirmar o fornecedor não significa pagar.
            if (!status.equals(reservaHotel.getStatus())) {
                var atualizacao = new ReservaHotelAtualizarReservaRQ(reservaHotel.getCodgReservaHotel(), status);
                restTemplate.exchange(
                        UrlConfig.URL_CONFIANCA_MANAGER + "/reservaHotel/atualizarReserva/"
                                + reservaHotel.getCodgReservaHotel(),
                        HttpMethod.PUT, new HttpEntity<>(atualizacao, headers), Object.class);
            }
            if (reservaHotel.getCodgHotel() != null) {
                var hotel = reservaHotel.getCodgHotel();
                if (reserva.getUrlImagem() == null || reserva.getUrlImagem().isBlank()) {
                    reserva.setUrlImagem(hotel.getUrlImagemHotel());
                }
                if (reserva.getDescricao() == null || reserva.getDescricao().isBlank()) {
                    reserva.setDescricao(hotel.getDescricao());
                }
            }

            return reserva;

        } catch (org.springframework.web.server.ResponseStatusException e) {
            throw e;
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            boolean timeout = e.getRawStatusCode() == 504;
            throw new org.springframework.web.server.ResponseStatusException(
                    timeout ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY,
                    timeout ? "O fornecedor excedeu o tempo de resposta ao consultar a reserva. Tente novamente."
                            : "O serviço de reservas não conseguiu concluir a consulta.", e);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            boolean timeout = e.getMostSpecificCause() instanceof java.net.SocketTimeoutException;
            throw new org.springframework.web.server.ResponseStatusException(
                    timeout ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY,
                    "Não foi possível consultar o serviço de reservas. Tente novamente.", e);
        } catch (Exception e) {
            e.printStackTrace();
            logErro("Erro ao carregar reserva hotel", e);
            throw new RuntimeException("Erro ao consultar reserva de hotel no HUB", e);
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
