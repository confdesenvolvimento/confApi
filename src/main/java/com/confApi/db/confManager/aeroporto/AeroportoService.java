package com.confApi.db.confManager.aeroporto;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.aeroporto.DTO.AeroportoParamRq;
import com.confApi.db.confManager.familia.FamiliaService;
import com.confApi.util.TelegramErrorAlert;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class AeroportoService {

    private static final Logger LOG = Logger.getLogger(AeroportoService.class.getName());
    private final RestTemplate restTemplate;
    private static final Duration TTL_AEROPORTOS_NACIONAIS = Duration.ofHours(6);

    private volatile Set<String> iatasNacionaisCache = Collections.emptySet();
    private volatile Instant iatasNacionaisAtualizadoEm = Instant.EPOCH;
    private final ObjectMapper mapper;

    @Autowired
    private ConfAppService confAppService;

    @Autowired(required = false)
    private TelegramErrorAlert telegramErrorAlert;

    @Autowired
    public AeroportoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }


    public List<Aeroporto> findAeroportoByParametros(AeroportoParamRq paramRq) {

        String url = UrlConfig.URL_CONFIANCA_MANAGER + "aeroporto/iataParam";
        try {
            HttpHeaders headers = defaultHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            HttpEntity<AeroportoParamRq> entity = new HttpEntity<>(paramRq, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return mapper.readValue(response.getBody(), new TypeReference<List<Aeroporto>>() {});
            }

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Erro ao consultar aeroportos por parâmetros", ex);
            alertarErro("Erro ao consultar aeroportos por parametros", ex);
        }

        return new ArrayList<>();
    }


    public List<Aeroporto> findAeroportoByIataPais(String iataPais) {

        List<Aeroporto> aeroportos = new ArrayList<>();

        String url = UrlConfig.URL_CONFIANCA_MANAGER + "aeroporto"+"/iataPais/" + iataPais;

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
            alertarErro("Erro ao consultar aeroportos por IATA do pais " + iataPais, ex);
        }

        return aeroportos;
    }


    public Set<String> findIatasAeroportosNacionais() {
        if (!cacheIatasNacionaisExpirado()) {
            return iatasNacionaisCache;
        }

        synchronized (this) {
            if (!cacheIatasNacionaisExpirado()) {
                return iatasNacionaisCache;
            }
            return atualizarIatasAeroportosNacionais();
        }
    }

    private boolean cacheIatasNacionaisExpirado() {
        return iatasNacionaisCache.isEmpty()
                || Instant.now().isAfter(iatasNacionaisAtualizadoEm.plus(TTL_AEROPORTOS_NACIONAIS));
    }

    private Set<String> atualizarIatasAeroportosNacionais() {
        String url = UriComponentsBuilder
                .fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER)
                .pathSegment("aeroporto", "nacionais", "iatas")
                .toUriString();

        try {
            ResponseEntity<Set<String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(defaultHeaders()),
                    new ParameterizedTypeReference<Set<String>>() {}
            );

            Set<String> body = response.getBody();
            if (body == null || body.isEmpty()) {
                return iatasNacionaisCache;
            }

            iatasNacionaisCache = body.stream()
                    .map(this::normalizarIata)
                    .filter(iata -> !iata.isEmpty())
                    .collect(Collectors.toCollection(HashSet::new));
            iatasNacionaisAtualizadoEm = Instant.now();
            return iatasNacionaisCache;
        } catch (Exception ex) {
            if (!iatasNacionaisCache.isEmpty()) {
                LOG.log(Level.WARNING, "Erro ao atualizar aeroportos nacionais; usando cache local anterior.", ex);
                return iatasNacionaisCache;
            }
            LOG.log(Level.SEVERE, "Erro ao consultar aeroportos nacionais", ex);
            alertarErro("Erro ao consultar aeroportos nacionais", ex);
            return Collections.emptySet();
        }
    }

    private String normalizarIata(String iata) {
        return iata == null ? "" : iata.trim().toUpperCase(Locale.ROOT);
    }
    public Aeroporto findAeroportoByIata(String iata) {

        String url = UrlConfig.URL_CONFIANCA_MANAGER + "aeroporto"+"/iata/" + iata;

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
            alertarErro("Erro ao consultar aeroporto por IATA " + iata, ex);
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
                alertarErro("Token de autenticacao nao encontrado no ConfAppService ao consultar aeroporto");
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Falha ao obter token de autenticação do ConfAppService", ex);
            alertarErro("Falha ao obter token de autenticacao do ConfAppService ao consultar aeroporto", ex);
        }

        return headers;
    }

    private void alertarErro(String mensagem) {
        if (telegramErrorAlert != null) {
            telegramErrorAlert.enviar(this, mensagem);
        }
    }

    private void alertarErro(String mensagem, Exception e) {
        if (telegramErrorAlert != null) {
            telegramErrorAlert.enviar(this, mensagem, e);
        }
    }

}
