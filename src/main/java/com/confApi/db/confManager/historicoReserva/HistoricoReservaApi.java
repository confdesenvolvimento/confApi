package com.confApi.db.confManager.historicoReserva;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.AbstractTransactionServiceApi;
import com.confApi.db.confManager.historicoReserva.dto.HistoricoReserva;
import com.confApi.hub.telegram.TelegramService;
import com.confApi.hub.telegram.dto.MensagemRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class HistoricoReservaApi extends AbstractTransactionServiceApi implements Serializable {

    @Autowired
    private ConfAppService confAppService;

    @Autowired
    public TelegramService telegramService;

    @Autowired
    private RestTemplate restTemplate;

    private final String urlAPI ="historicoReserva";

    public HistoricoReserva create(HistoricoReserva historicoReserva) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // Converte objeto para JSON
            String jsonRequest = objectMapper.writeValueAsString(historicoReserva);

            // Faz POST
            String responseBody = sendHttpApiPostPublic(
                    UrlConfig.URL_CONFIANCA_MANAGER + urlAPI + "/gravar",
                    jsonRequest
            );

            // Converte resposta para objeto
            return objectMapper.readValue(responseBody, HistoricoReserva.class);

        } catch (JsonProcessingException ex) {
            Logger.getLogger(HistoricoReservaApi.class.getName()).log(Level.SEVERE, null, ex);
        }

        return new HistoricoReserva();
    }

    public List<HistoricoReserva> findByCodgReservaAereo(Integer codgReservaAereo) {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            ConfAppResp token = confAppService.token();

            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            } else {

                return Collections.emptyList();
            }

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<List<HistoricoReserva>> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER
                                    + urlAPI
                                    + "/listarByCodgReservaAereo/"
                                    + codgReservaAereo,
                            HttpMethod.GET,
                            requestEntity,
                            new ParameterizedTypeReference<List<HistoricoReserva>>() {
                            }
                    );

            return response.getBody() != null
                    ? response.getBody()
                    : Collections.emptyList();

        } catch (HttpClientErrorException ex) {
            logErro("Erro no findByCodgReservaAereo", ex);

        } catch (Exception ex) {
            logErro("Erro no findByCodgReservaAereo", ex);
        }

        return Collections.emptyList();
    }

    public List<HistoricoReserva> findByCodgReservaHotel(Integer codgReservaHotel) {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            ConfAppResp token = confAppService.token();

            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            } else {
                return Collections.emptyList();
            }

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<List<HistoricoReserva>> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER
                                    + urlAPI
                                    + "/listarByCodgReservaHotel/"
                                    + codgReservaHotel,
                            HttpMethod.GET,
                            requestEntity,
                            new ParameterizedTypeReference<List<HistoricoReserva>>() {
                            }
                    );

            return response.getBody() != null
                    ? response.getBody()
                    : Collections.emptyList();

        } catch (HttpClientErrorException ex) {
            logErro("Erro no findByCodgReservaHotel", ex);

        } catch (Exception ex) {
            logErro("Erro no findByCodgReservaHotel", ex);
        }

        return Collections.emptyList();
    }

    public List<HistoricoReserva> findByCodgReservaSeguro(Integer codgReservaSeguro) {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            ConfAppResp token = confAppService.token();

            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            } else {
                return Collections.emptyList();
            }

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<List<HistoricoReserva>> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER
                                    + urlAPI
                                    + "/listarByCodgReservaSeguro/"
                                    + codgReservaSeguro,
                            HttpMethod.GET,
                            requestEntity,
                            new ParameterizedTypeReference<List<HistoricoReserva>>() {
                            }
                    );

            return response.getBody() != null
                    ? response.getBody()
                    : Collections.emptyList();

        } catch (HttpClientErrorException ex) {
            logErro("Erro no findByCodgReservaSeguro", ex);

        } catch (Exception ex) {
            logErro("Erro no findByCodgReservaSeguro", ex);
        }

        return Collections.emptyList();
    }

    public List<HistoricoReserva> findByCodgReservaPacote(Integer codgReservaPacote) {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            ConfAppResp token = confAppService.token();

            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            } else {
                return Collections.emptyList();
            }

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<List<HistoricoReserva>> response =
                    restTemplate.exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER
                                    + urlAPI
                                    + "/listarByCodgReservaPacote/"
                                    + codgReservaPacote,
                            HttpMethod.GET,
                            requestEntity,
                            new ParameterizedTypeReference<List<HistoricoReserva>>() {
                            }
                    );

            return response.getBody() != null
                    ? response.getBody()
                    : Collections.emptyList();

        } catch (HttpClientErrorException ex) {
            logErro("Erro no findByCodgReservaPacote", ex);

        } catch (Exception ex) {
            logErro("Erro no findByCodgReservaPacote", ex);
        }

        return Collections.emptyList();
    }

    private void logErro(String mensagem, Exception e) {
        MensagemRequest msg = new MensagemRequest(mensagem + ": " + e.getMessage());
        msg.setMetodo(Thread.currentThread().getStackTrace()[2].getMethodName());
        msg.setClasse(this.getClass().getSimpleName());
        msg.setProjeto("CONFAPI");

        telegramService.enviarLogDeErros(msg);
    }


}
