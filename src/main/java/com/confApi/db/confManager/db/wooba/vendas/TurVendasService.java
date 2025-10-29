package com.confApi.db.confManager.db.wooba.vendas;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.confApp.dto.SandBoxResp;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.db.wooba.vendas.dto.RQConsultaVendasDto;
import com.confApi.db.confManager.db.wooba.vendas.dto.VendasAereasExibicao;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class TurVendasService {

    private static final Logger LOG = Logger.getLogger(TurVendasService.class.getName());
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    private static final String API_ACTION_VENDAS = "wooba/turVendasAereas/vendasByParamAndUsuario";


    @Autowired
    private ConfAppService confAppService;


    public TurVendasService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }

    public List<VendasAereasExibicao> findVendasWoobaByParam(RQConsultaVendasDto rq) {
        List<VendasAereasExibicao> reservasAereasExibicao = new ArrayList<>();

        try {
            // Monta URL da API
            String url = UrlConfig.URL_CONFIANCA_MANAGER + API_ACTION_VENDAS;
            LOG.info("[TurVendasService] Endpoint: " + url);

            // Usa headers com token
            HttpHeaders headers = defaultHeaders();

            // Cria a entidade com corpo e headers
            HttpEntity<RQConsultaVendasDto> requestEntity = new HttpEntity<>(rq, headers);

            // Chamada POST via RestTemplate
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            // Processa resposta
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                reservasAereasExibicao = mapper.readValue(
                        response.getBody(),
                        new TypeReference<List<VendasAereasExibicao>>() {}
                );

                // Ajusta sigla JJ → LA
                for (VendasAereasExibicao venda : reservasAereasExibicao) {
                    if ("JJ".equalsIgnoreCase(venda.getSiglaCia())) {
                        venda.setSiglaCia("LA");
                    }
                }

            } else {
                LOG.warning("[TurVendasService] Resposta não 2xx ou vazia da API: " + response.getStatusCode());
            }

        } catch (Exception ex) {
          System.out.println("[TurVendasService] Erro ao consultar vendas Wooba: " + ex.getMessage());
        }

        return reservasAereasExibicao;
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            ConfAppResp token = confAppService.token();
            if (token != null && token.getToken() != null && !token.getToken().isBlank()) {
                headers.setBearerAuth(token.getToken());
                LOG.info("[turVendasService] Token Bearer adicionado com sucesso.");
            } else {
                LOG.warning("[turVendasService] Token nulo ou inválido retornado por confAppService.token()");
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "[turVendasService] Falha ao obter token do ConfAppService", e);
        }

        return headers;
    }
}

