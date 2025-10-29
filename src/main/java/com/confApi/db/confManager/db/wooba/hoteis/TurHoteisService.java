package com.confApi.db.confManager.db.wooba.hoteis;


import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.confApp.dto.SandBoxResp;
import com.confApi.config.UrlConfig;

import com.confApi.db.confManager.db.wooba.hoteis.dto.FilterPosAtendimento;
import com.confApi.db.confManager.db.wooba.hoteis.dto.ReservasHotelExibicao;
import com.confApi.db.confManager.db.wooba.hoteis.util.UtilHoteis;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class TurHoteisService {

    private static final Logger LOG = Logger.getLogger(TurHoteisService.class.getName());
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    private static final String API_ACTION_HOTEIS_PREFIX = "wooba/";
    private static final String API_ACTION_RESERVAS_HOTEL_WOOBA = "turReservasHotel";

    @Autowired
    private ConfAppService confAppService;

    @Autowired
    public TurHoteisService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }

    public List<ReservasHotelExibicao> findVendasByHotel(String codgAgencia) {
        List<ReservasHotelExibicao> reservasHotelExibicao = new ArrayList<>();

        try {
            // Define datas
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Date dataFinal = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(dataFinal);
            cal.add(Calendar.DAY_OF_MONTH, -30);
            Date dataInicial = cal.getTime();

            // Monta corpo da requisição
            FilterPosAtendimento rq = new FilterPosAtendimento();
            rq.setDataInicio(dateFormat.format(dataInicial));
            rq.setDataFim(dateFormat.format(dataFinal));
            rq.setCodgAgencia(codgAgencia);

            // Monta URL da API
            String url = UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_HOTEIS_PREFIX + API_ACTION_RESERVAS_HOTEL_WOOBA;
            LOG.info("[TurHoteisService] Endpoint: " + url);

            // Headers com Bearer Token
            HttpHeaders headers = defaultHeaders();

            // Entidade da requisição
            HttpEntity<FilterPosAtendimento> requestEntity = new HttpEntity<>(rq, headers);

            // POST
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            // Processa resposta
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                reservasHotelExibicao = mapper.readValue(
                        response.getBody(),
                        new TypeReference<List<ReservasHotelExibicao>>() {}
                );

                for (ReservasHotelExibicao r : reservasHotelExibicao) {
                    r.setNomeFornecedor(UtilHoteis.getNomeSistemaExibicao(r.getSistema()));
                    r.setFonte("Portal do Agente");
                }

                ordenarPorCriadaEmDecrescente(reservasHotelExibicao);

            } else {
                LOG.warning("[TurHoteisService] Resposta não 2xx ou corpo vazio: " + response.getStatusCode());
            }

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "[TurHoteisService] Erro ao consultar ReservasHotelWooba: " + ex.getMessage(), ex);
        }

        return reservasHotelExibicao;
    }

    private void ordenarPorCriadaEmDecrescente(List<ReservasHotelExibicao> lista) {
        try {
            lista.sort(Comparator.comparing(ReservasHotelExibicao::getCriadaEm,
                    Comparator.nullsLast(Comparator.reverseOrder())));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "[TurHoteisService] Falha ao ordenar reservas por data decrescente", e);
        }
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            ConfAppResp token = confAppService.token();
            if (token != null && token.getToken() != null && !token.getToken().isBlank()) {
                headers.setBearerAuth(token.getToken());
                LOG.info("[TurHoteisService] Token Bearer adicionado com sucesso.");
            } else {
                LOG.warning("[TurHoteisService] Token nulo ou inválido retornado por confAppService.token()");
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "[TurHoteisService] Falha ao obter token do ConfAppService", e);
        }

        return headers;
    }
}
