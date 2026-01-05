package com.confApi.db.confManager.markup;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.markup.dto.Markup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class MarkupService {

    private static final Logger LOGGER = Logger.getLogger(MarkupService.class.getName());

    private final RestTemplate restTemplate;

    @Autowired
    private ConfAppService confAppService;

    private final String urlAPI = "markup";
    private static final Double DEFAULT_MARKUP = 0.75;

    public MarkupService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private HttpEntity<Object> authEntity() {
        ConfAppResp token = confAppService.token();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token.getToken());

        return new HttpEntity<>(headers);
    }

    /**
     * GET /markup/findByCodgProdutoAndAgencia/{codgProduto}/{codgAgencia}
     */
    public Markup findByCodgProduto(Integer codgProduto) {
        Integer codgAgencia = 1;

        try {
            String url = String.format("%s%s/findByCodgProdutoAndAgencia/%d/%d",
                    UrlConfig.URL_CONFIANCA_MANAGER, urlAPI, codgProduto, codgAgencia);

            System.out.println("findByCodgProduto -> " + url);

            ResponseEntity<Markup> resp = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    authEntity(),
                    Markup.class
            );

            return resp.getBody();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar Markup por produto/agencia", e);
            return null;
        }
    }

    /**
     * POST /markup/findByMkp
     * (no front você fazia setValorMarkup(null) antes — mantido)
     */
    public Markup findByParam(Markup markup) {
        try {
            markup.setValorMarkup(null);

            String url = String.format("%s%s/findByMkp", UrlConfig.URL_CONFIANCA_MANAGER, urlAPI);

            System.out.println("findByParam -> " + url);

            HttpEntity<Object> entity = buildAuthEntityWithBody(markup);

            ResponseEntity<List<Markup>> resp = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<Markup>>() {}
            );

            List<Markup> list = resp.getBody();
            return (list != null && !list.isEmpty()) ? list.get(0) : null;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar Markup por parametros", e);
            return null;
        }
    }

    /**
     * GET /markup/findVlrMarkupByProduto/{codgProduto}
     * Se vier null/vazio, retorna DEFAULT_MARKUP (0.75)
     */
    public Double findByCodProdutoValue(Integer codgProduto) {
        try {
            String url = String.format("%s%s/findVlrMarkupByProduto/%d",
                    UrlConfig.URL_CONFIANCA_MANAGER, urlAPI, codgProduto);



            ResponseEntity<Double> resp = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    authEntity(),
                    Double.class
            );

            Double body = resp.getBody();

            return body != null ? body : DEFAULT_MARKUP;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Falha ao obter markup. Usando default: " + DEFAULT_MARKUP, e);
            return DEFAULT_MARKUP;
        }
    }

    private HttpEntity<Object> buildAuthEntityWithBody(Object body) {
        ConfAppResp token = confAppService.token();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token.getToken());

        return new HttpEntity<>(body, headers);
    }
    public List<Markup> findByMkp(Markup filtro) {
        try {
            // Mesmo comportamento do front: não filtrar por valorMarkup
            if (filtro != null) {
                filtro.setValorMarkup(null);
            }

            // Token
            ConfAppResp token = confAppService.token();

            // URL
            String url = String.format("%s%s/findByMkp", UrlConfig.URL_CONFIANCA_MANAGER, urlAPI);
            System.out.println("findByMkp -> " + url);

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token.getToken());

            // Body + headers
            HttpEntity<Markup> requestEntity = new HttpEntity<>(filtro, headers);

            // POST -> List<Markup>
            ResponseEntity<List<Markup>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<List<Markup>>() {}
            );

            List<Markup> lista = responseEntity.getBody();
            return lista != null ? lista : Collections.emptyList();

        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Erro ao buscar Markup via /findByMkp", e);
            return Collections.emptyList();
        }
    }


}
