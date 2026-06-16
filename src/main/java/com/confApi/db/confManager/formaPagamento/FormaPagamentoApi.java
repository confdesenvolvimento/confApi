package com.confApi.db.confManager.formaPagamento;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class FormaPagamentoApi {

    private static final Logger LOG = Logger.getLogger(FormaPagamentoApi.class.getName());

    private final RestTemplate restTemplate;
    private final ConfAppService confAppService;

    public FormaPagamentoApi(RestTemplate restTemplate, ConfAppService confAppService) {
        this.restTemplate = restTemplate;
        this.confAppService = confAppService;
    }

    public FormaPagamento findByNomeFormaPagto(String nomeFormaPagto) {
        if (nomeFormaPagto == null || nomeFormaPagto.trim().isEmpty()) {
            return null;
        }

        String nomeNormalizado = nomeFormaPagto.trim();

        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER)
                    .path("/formaPagamento")
                    .queryParam("nomeFormaPagto", nomeNormalizado)
                    .toUriString();

            ResponseEntity<List<FormaPagamento>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(defaultHeaders(token.getToken())),
                    new ParameterizedTypeReference<List<FormaPagamento>>() {
                    }
            );

            List<FormaPagamento> formas = response.getBody();
            if (formas == null || formas.isEmpty()) {
                return null;
            }

            String upperNome = nomeNormalizado.toUpperCase(Locale.ROOT);
            return formas.stream()
                    .filter(forma -> forma.getNomeFormaPagto() != null
                            && forma.getNomeFormaPagto().trim().equalsIgnoreCase(nomeNormalizado))
                    .findFirst()
                    .orElseGet(() -> formas.stream()
                            .filter(forma -> forma.getNomeFormaPagto() != null
                                    && forma.getNomeFormaPagto().toUpperCase(Locale.ROOT).contains(upperNome))
                            .findFirst()
                            .orElse(formas.get(0)));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Nao foi possivel consultar forma de pagamento por nome: " + nomeNormalizado, e);
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
