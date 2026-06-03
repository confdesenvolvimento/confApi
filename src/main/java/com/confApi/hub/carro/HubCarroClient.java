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

    public List<ReservarCarroResponseDTO> reservar(ReservarCarroRequestDTO reservarCarroRequestDTO) {
//        SelecionarCarroRequestHub selecionarCarroRequestHub = new SelecionarCarroRequestHub(selecionarCarroRequestDTO);

        try {
            ConfAppResp token = confAppService.token();
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<ReservarCarroRequestDTO> entity = new HttpEntity<>(reservarCarroRequestDTO, headers);

            ResponseEntity<List<ReservarCarroResponseDTO>> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_HUB + "api/carro/reservar",
                            HttpMethod.POST,
                            entity,
                            new ParameterizedTypeReference<List<ReservarCarroResponseDTO>>() {}
                    );

            List<ReservarCarroResponseDTO> body = response.getBody();

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

    public List<ReservarCarroResponseDTO> consultarReserva(ConsultarReservaCarroRequestDTO consultarReservaCarroRequestDTO) {
//        SelecionarCarroRequestHub selecionarCarroRequestHub = new SelecionarCarroRequestHub(selecionarCarroRequestDTO);

        try {
            ConfAppResp token = confAppService.token();
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<ConsultarReservaCarroRequestDTO> entity = new HttpEntity<>(consultarReservaCarroRequestDTO, headers);

            ResponseEntity<List<ReservarCarroResponseDTO>> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_HUB + "api/carro/consultarReserva",
                            HttpMethod.POST,
                            entity,
                            new ParameterizedTypeReference<List<ReservarCarroResponseDTO>>() {}
                    );

            List<ReservarCarroResponseDTO> body = response.getBody();

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

    public List<CancelarReservaCarroResponseDTO> cancelarReserva(CancelarReservaCarroRequestDTO cancelarReservaCarroRequestDTO) {
//        SelecionarCarroRequestHub selecionarCarroRequestHub = new SelecionarCarroRequestHub(selecionarCarroRequestDTO);

        try {
            ConfAppResp token = confAppService.token();
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<CancelarReservaCarroRequestDTO> entity = new HttpEntity<>(cancelarReservaCarroRequestDTO, headers);

            ResponseEntity<List<CancelarReservaCarroResponseDTO>> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_HUB + "api/carro/cancelarReserva",
                            HttpMethod.POST,
                            entity,
                            new ParameterizedTypeReference<List<CancelarReservaCarroResponseDTO>>() {}
                    );

            List<CancelarReservaCarroResponseDTO> body = response.getBody();

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

    public List<FormasPagamentoCarroResponseDTO> obterFormasPagamento(FormasPagamentoCarroRequestDTO formasPagamentoCarroRequestDTO) {
//        SelecionarCarroRequestHub selecionarCarroRequestHub = new SelecionarCarroRequestHub(selecionarCarroRequestDTO);

        try {
            ConfAppResp token = confAppService.token();
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<FormasPagamentoCarroRequestDTO> entity = new HttpEntity<>(formasPagamentoCarroRequestDTO, headers);

            ResponseEntity<List<FormasPagamentoCarroResponseDTO>> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_HUB + "api/carro/obterFormasPagamento",
                            HttpMethod.POST,
                            entity,
                            new ParameterizedTypeReference<List<FormasPagamentoCarroResponseDTO>>() {}
                    );

            List<FormasPagamentoCarroResponseDTO> body = response.getBody();

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

    public List<EmitirCarroResponseDTO> emitir(EmitirCarroRequestDTO emitirCarroRequestDTO) {
//        SelecionarCarroRequestHub selecionarCarroRequestHub = new SelecionarCarroRequestHub(selecionarCarroRequestDTO);

        try {
            ConfAppResp token = confAppService.token();
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<EmitirCarroRequestDTO> entity = new HttpEntity<>(emitirCarroRequestDTO, headers);

            ResponseEntity<List<EmitirCarroResponseDTO>> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_HUB + "api/carro/emitir",
                            HttpMethod.POST,
                            entity,
                            new ParameterizedTypeReference<List<EmitirCarroResponseDTO>>() {}
                    );

            List<EmitirCarroResponseDTO> body = response.getBody();

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
