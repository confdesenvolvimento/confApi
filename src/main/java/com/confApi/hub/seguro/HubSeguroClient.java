package com.confApi.hub.seguro;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.hotel.model.HotelResponse;
import com.confApi.db.confManager.seguro.reserva.DTO.CancelamentoRequestDTO;
import com.confApi.hub.hotel.dto.*;
import com.confApi.hub.limites.dto.StatusResponse;
import com.confApi.seguros.dto.*;
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
public class HubSeguroClient {

    private final RestTemplate restTemplate;

    private static final Logger LOG = Logger.getLogger(HubSeguroClient.class.getName());

    @Autowired
    private ConfAppService confAppService;

    public HubSeguroClient(RestTemplate hubRestTemplate) {
        this.restTemplate = hubRestTemplate;
    }

    public List<PlanoSeguroDTO> pesquisarDisponibilidade(SeguroViagemPesquisaDTO seguroViagemPesquisaDTO) {
        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<SeguroViagemPesquisaDTO> entity = new HttpEntity<>(seguroViagemPesquisaDTO, headers);

            // 4) Chamada (LISTA!)
            ResponseEntity<List<PlanoSeguroDTO>> response = restTemplate.exchange(
                    UrlConfig.URL_CONFIANCA_HUB + "/api/seguro/pesquisar",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<PlanoSeguroDTO>>() {}
            );

            List<PlanoSeguroDTO> body = response.getBody();

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


    public String efetuarReserva(SeguroCompraModel seguroCompraModel) {
        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<SeguroCompraModel> entity = new HttpEntity<>(seguroCompraModel, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    UrlConfig.URL_CONFIANCA_HUB + "/api/seguro/emitir",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }

            LOG.log(Level.WARNING,
                    "emissão de seguro não é valida",
                    response.getStatusCode());

            return null;

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao emitir seguro hub", e);
            return null;
        }
    }

    public List<SeguroReservaDTO> carregarReserva(SeguroCarregarReservaDTO req) {
        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<SeguroCarregarReservaDTO> entity = new HttpEntity<>(req, headers);

            // 4) Chamada (LISTA!)
            ResponseEntity<List<SeguroReservaDTO>> response = restTemplate.exchange(
                    UrlConfig.URL_CONFIANCA_HUB + "/api/seguro/carregar",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<SeguroReservaDTO>>() {}
            );

            List<SeguroReservaDTO> body = response.getBody();

            // 5) Validação mínima
            if (response.getStatusCode().is2xxSuccessful() && body != null) {
                return body;
            }

            LOG.log(Level.WARNING,
                    "carregar reserva retornou status {0} sem corpo válido",
                    response.getStatusCode());

            return Collections.emptyList();

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao carregar reserva de seguro no HUB", e);
            return Collections.emptyList();
        }
    }

    public List<SeguroReservaDTO> cancelarReserva(CancelamentoRequestDTO cancelarRequest) {
        try {
            ConfAppResp token = confAppService.token();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<CancelamentoRequestDTO> entity = new HttpEntity<>(cancelarRequest, headers);

            // 4) Chamada (LISTA!)
            ResponseEntity<List<SeguroReservaDTO>> response = restTemplate.exchange(
                    UrlConfig.URL_CONFIANCA_HUB + "/api/seguro/cancelar",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<SeguroReservaDTO>>() {}
            );

            List<SeguroReservaDTO> body = response.getBody();

            // 5) Validação mínima
            if (response.getStatusCode().is2xxSuccessful() && body != null) {
                return body;
            }

            LOG.log(Level.WARNING,
                    "cancelar reserva retornou status {0} sem corpo válido",
                    response.getStatusCode());

            return Collections.emptyList();

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao cancelar reserva de seguro no HUB", e);
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