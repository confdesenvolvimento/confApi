package com.confApi.hoteis;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.hotel.model.HotelResponse;
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
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class HotelClient {

    private final RestTemplate restTemplate;
    private final RestTemplate pesquisaHttp = criarPesquisaHttp();
    private static RestTemplate criarPesquisaHttp() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); factory.setReadTimeout(175000);
        return new RestTemplate(factory);
    }


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
        return pesquisar(req, null);
    }

    public List<HotelResponse> pesquisar(HotelPesquisaModelFront req, String fornecedor) {
        try {
            HotelPesquisaModel hubRequest = HotelPesquisaMapper.toHub(req);
            System.out.println("URL: " + UrlConfig.URL_CONFIANCA_HUB + " - " + API_ACTION);
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_HUB)
                    .path(API_ACTION + (fornecedor == null ? "/disponibilidade" : "/disponibilidade/rapida/" + fornecedor))
                    .toUriString();
            System.out.println("URL DOIDA: " + url);
            HttpHeaders headers = defaultHeaders(token.getToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<HotelPesquisaModel> entity =
                    new HttpEntity<>(hubRequest, headers);

            ResponseEntity<List<HotelResponse>> hubResponse =
                    (fornecedor == null ? restTemplate : pesquisaHttp).exchange(
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


    public HotelResponse carregarConteudo(HotelPesquisaModelFront pesquisa, HotelResponse hotel) {
        var token = confAppService.token();
        var pedido = new java.util.HashMap<String,Object>();
        pedido.put("fornecedor", hotel.getNomeSistema()); pedido.put("codigo", hotel.getCodigoHotelSistema());
        pedido.put("nome", hotel.getNome()); pedido.put("pesquisa", HotelPesquisaMapper.toHub(pesquisa));
        return restTemplate.postForObject(UrlConfig.URL_CONFIANCA_HUB + API_ACTION + "/conteudo",
                new HttpEntity<>(pedido, defaultHeaders(token.getToken())), HotelResponse.class);
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
        if (req == null || req.getIdentificador() == null || req.getIdentificador().isBlank()) {
            throw new IllegalStateException("Identificador da reserva ausente.");
        }
        String fase = "AUTENTICACAO";
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_HUB)
                    .path(API_ACTION + "/carregarReserva") // ajuste aqui se o HUB usar outro path
                    .toUriString();

            HttpHeaders headers = defaultHeaders(token.getToken());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<HotelCarregaModelFront> entity = new HttpEntity<>(req, headers);

            fase = "CONSULTA_FORNECEDOR";
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
            int status = validarStatusFornecedor(reserva, req.getIdentificador());

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            fase = "CONSULTA_VINCULO";
            ResponseEntity<JsonNode> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER + "/reservaHotel/localizador/" + req.getIdentificador(),
                            HttpMethod.GET,
                            requestEntity,
                            JsonNode.class
                    );

            JsonNode persistida = response.getBody();
            // O vínculo precisa estar presente no contrato. Campo omitido não significa reserva avulsa.
            if (!response.getStatusCode().is2xxSuccessful() || persistida == null || !persistida.isObject()
                    || !persistida.path("codgReservaHotel").isIntegralNumber()
                    || !persistida.path("codgReservaHotel").canConvertToInt()
                    || persistida.path("codgReservaHotel").asInt() <= 0
                    || !persistida.path("localizador").isTextual()
                    || !req.getIdentificador().equalsIgnoreCase(persistida.path("localizador").asText())
                    || !persistida.has("codgReservaPacote")) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Não foi possível confirmar o vínculo da reserva.");
            }

            JsonNode pacote = persistida.get("codgReservaPacote");
            if (!pacote.isNull()) {
                if (!pacote.isObject() || !pacote.path("codgPacote").isIntegralNumber()
                        || !pacote.path("codgPacote").canConvertToInt() || pacote.path("codgPacote").asInt() <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Vínculo do pacote inválido.");
                }
                // A conciliação do pacote consulta o fornecedor. Sua operação própria controla a persistência.
            } else {
                fase = "SINCRONIZACAO_AVULSA";
                sincronizarReservaAvulsa(persistida, status, headers);
            }
            complementarDadosHotel(reserva, persistida.path("codgHotel"));
            return reserva;

        } catch (Exception e) {
            Logger.getLogger(HotelClient.class.getName()).log(Level.WARNING,
                    "Consulta hotel não confirmada: fase={0}, categoria={1}, HTTP={2}",
                    new Object[]{fase, e.getClass().getSimpleName(),
                        e instanceof HttpStatusCodeException http ? http.getRawStatusCode() : -1});
            if (e instanceof ResponseStatusException statusException) {
                throw new ResponseStatusException(statusException.getStatus(), statusException.getReason());
            }
            boolean timeout = (e instanceof HttpStatusCodeException http && http.getRawStatusCode() == 504)
                    || (e instanceof ResourceAccessException acesso
                        && acesso.getMostSpecificCause() instanceof SocketTimeoutException);
            // Não anexa a exceção remota: seu corpo e sua causa podem conter dados privados da reserva.
            throw new ResponseStatusException(timeout ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY,
                    timeout ? "O serviço de reservas excedeu o tempo de resposta ao consultar a reserva. Tente novamente."
                            : "Não foi possível consultar a reserva de hotel.");
        }
    }

    private int validarStatusFornecedor(HotelReserva reserva, String identificador) {
        if (reserva == null || reserva.getReservasHotelRsList() == null || reserva.getReservasHotelRsList().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível validar a reserva retornada pelo fornecedor.");
        }
        Integer status = null;
        for (var item : reserva.getReservasHotelRsList()) {
            if (item == null || !identificador.equalsIgnoreCase(item.getIdentificador())) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "O fornecedor não confirmou a identidade da reserva.");
            }
            int atual = StatusReservaHotelFornecedor.codigo(item.getStatus());
            if (status != null && status != atual) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "A reserva possui status divergentes; o status salvo foi preservado.");
            }
            status = atual;
        }
        for (var item : reserva.getReservasHotelRsList()) {
            item.setStatus(StatusReservaHotelFornecedor.canonico(status));
        }
        return status;
    }

    private void sincronizarReservaAvulsa(JsonNode persistida, int status, HttpHeaders headers) {
        JsonNode statusSalvo = persistida.path("status");
        if (statusSalvo.isIntegralNumber() && statusSalvo.canConvertToInt() && statusSalvo.asInt() == status) {
            return;
        }
        int codgReservaHotel = persistida.path("codgReservaHotel").asInt();
        // A consulta sincroniza somente o status operacional; o pagamento permanece sob controle do Manager.
        var request = new HttpEntity<>(new ReservaHotelAtualizarReservaRQ(codgReservaHotel, status), headers);
        var response = restTemplate.exchange(UrlConfig.URL_CONFIANCA_MANAGER + "/reservaHotel/atualizarReserva/" + codgReservaHotel,
                HttpMethod.PUT, request, Object.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível atualizar o status da reserva de hotel.");
        }
    }

    private void complementarDadosHotel(HotelReserva reserva, JsonNode hotel) {
        if ((reserva.getUrlImagem() == null || reserva.getUrlImagem().isBlank())
                && hotel.path("urlImagemHotel").isTextual()) {
            reserva.setUrlImagem(hotel.path("urlImagemHotel").asText());
        }
        if ((reserva.getDescricao() == null || reserva.getDescricao().isBlank())
                && hotel.path("descricao").isTextual()) {
            reserva.setDescricao(hotel.path("descricao").asText());
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
