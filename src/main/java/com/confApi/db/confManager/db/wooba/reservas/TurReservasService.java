package com.confApi.db.confManager.db.wooba.reservas;



import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.confApp.dto.SandBoxResp;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.db.wooba.reservas.dto.FilterPosAtendimento;
import com.confApi.db.confManager.db.wooba.reservas.dto.PosAtendimentoAereoDto;
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
public class TurReservasService {

    private static final Logger LOG = Logger.getLogger(TurReservasService.class.getName());
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    // Ajuste o prefixo se na sua API o "reservas/" já estiver embutido em UrlConfig
    private static final String API_ACTION_RESERVAS_PREFIX = "reservas/";
    private static final String API_ACTION_POS_ATENDIMENTO = "PosAtendimentoAereo";

    @Autowired
    private ConfAppService confAppService;

    @Autowired
    public TurReservasService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }

    public List<PosAtendimentoAereoDto> findVendasByPosVendas(Date dataInicial, Date dataFinal, String rota) {
        List<PosAtendimentoAereoDto> resultado = new ArrayList<>();

        try {
            // Monta o corpo da requisição
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            FilterPosAtendimento rq = new FilterPosAtendimento();
            rq.setDataInicio(sdf.format(dataInicial));
            rq.setDataFim(sdf.format(dataFinal));
            rq.setRota(rota);

            // Monta URL da API
            String url = UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_RESERVAS_PREFIX + API_ACTION_POS_ATENDIMENTO;
            LOG.info("[TurReservasService] Endpoint: " + url);

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

            // Trata resposta
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                resultado = mapper.readValue(
                        response.getBody(),
                        new TypeReference<List<PosAtendimentoAereoDto>>() {}
                );
            } else {
                LOG.warning("[TurReservasService] Resposta não 2xx ou corpo vazio: " + response.getStatusCode());
            }

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "[TurReservasService] Erro ao consultar PosAtendimentoAereo: " + ex.getMessage(), ex);
        }

        return resultado;
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            ConfAppResp token = confAppService.token();
            if (token != null && token.getToken() != null && !token.getToken().isBlank()) {
                headers.setBearerAuth(token.getToken());
                LOG.info("[TurReservasService] Token Bearer adicionado com sucesso.");
            } else {
                LOG.warning("[TurReservasService] Token nulo ou inválido retornado por confAppService.token()");
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "[TurReservasService] Falha ao obter token do ConfAppService", e);
        }

        return headers;
    }
}
