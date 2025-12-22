package com.confApi.db.confManager.aeroporto;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.aeroporto.DTO.AeroportoParamRq;
import com.confApi.db.confManager.familia.FamiliaService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class AeroportoService {

    private static final Logger LOG = Logger.getLogger(FamiliaService.class.getName());
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    @Autowired
    private ConfAppService confAppService;

    @Autowired
    public AeroportoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }


    public List<Aeroporto> findAeroportoByParametros(AeroportoParamRq paramRq) {

        List<Aeroporto> aeroportos = new ArrayList<>();

        String urlBase = UrlConfig.URL_CONFIANCA_MANAGER + "aeroporto/iataParametros";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(urlBase);

        if (paramRq.getIataPais() != null) {
            builder.queryParam("iataPais", paramRq.getIataPais());
        }

        // REGRA DO MANAGER: iataAeroporto nunca pode ser null
        if (paramRq.getIataAeroporto() != null) {
            builder.queryParam("iataAeroporto", paramRq.getIataAeroporto());
        } else {
            builder.queryParam("iataAeroporto", "");
        }

        if (paramRq.getNomeAeroporto() != null) {
            builder.queryParam("nomeAeroporto", paramRq.getNomeAeroporto());
        }

        String url = builder.toUriString();
        System.out.println("URL AEROPORTO PARAMETROS: " + url);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(defaultHeaders()),
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                aeroportos = mapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Aeroporto>>() {}
                );
            }

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Erro ao consultar aeroportos por parâmetros", ex);
        }

        return aeroportos;
    }



    public List<Aeroporto> findAeroportoByIataPais(String iataPais) {

        List<Aeroporto> aeroportos = new ArrayList<>();

        String url = UrlConfig.URL_CONFIANCA_MANAGER + "aeroporto"+"/iataPais/" + iataPais;

        System.out.println("URL AEROPORTO POR PAIS: " + url);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(defaultHeaders()),
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                aeroportos = mapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Aeroporto>>() {}
                );
            }

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Erro ao consultar aeroportos por IATA do país: " + iataPais, ex);
        }

        return aeroportos;
    }


    public Aeroporto findAeroportoByIata(String iata) {

        String url = UrlConfig.URL_CONFIANCA_MANAGER + "aeroporto"+"/iata/" + iata;

        System.out.println("URL AEROPORTO POR IATA: " + url);

        try {
            ResponseEntity<Aeroporto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            new HttpEntity<>(defaultHeaders()),
                            Aeroporto.class
                    );

            return response.getBody();

        } catch (HttpClientErrorException.NotFound e) {
            // 404 vindo do controller
            LOG.log(Level.WARNING, "Aeroporto não encontrado para IATA: " + iata);
            return null;

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Erro ao consultar aeroporto por IATA", ex);
            return null;
        }
    }


    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ConfAppResp token = confAppService.token();
            if (token != null && token.getToken() != null) {
                headers.setBearerAuth(token.getToken());
            } else {
                LOG.warning("Token de autenticação não encontrado no ConfAppService.");
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Falha ao obter token de autenticação do ConfAppService", ex);
        }

        return headers;
    }

}
