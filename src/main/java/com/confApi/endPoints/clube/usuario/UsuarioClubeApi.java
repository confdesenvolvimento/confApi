package com.confApi.endPoints.clube.usuario;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.AbstractTransactionServiceApi;
import com.confApi.db.clube.usuario.UsuarioClube;
import com.confApi.db.clube.usuario.dto.AgenciaDTO;
import com.confApi.db.clube.usuario.dto.UnidadeDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class UsuarioClubeApi extends AbstractTransactionServiceApi implements Serializable {

    @Autowired
    private ConfAppService confAppService;

    private final RestTemplate restTemplate;

    public UsuarioClubeApi(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    /*ta pronto*/
    public UsuarioClube create(UsuarioClube usuario) {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/usuario")
                    .toUriString();

          //  System.out.println("url: " + url);

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<UsuarioClube> entity = new HttpEntity<>(usuario, headers);

            ResponseEntity<UsuarioClube> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    UsuarioClube.class
            );

            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return new UsuarioClube();
        }
    }
    /*ta pronto*/
    public Page<UsuarioClube> findAllPage(int page, int size, String nome) {
        try {
            ConfAppResp token = confAppService.token();

            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/usuario/findAllPage")
                    .queryParam("page", page)
                    .queryParam("size", size);

            if (nome != null && !nome.isEmpty()) {
                builder.queryParam("nomeUsuario", nome);
            }

            String url = builder.toUriString();
           // System.out.println("url: " + url);

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);

            ResponseEntity<RestPageResponse<UsuarioClube>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<RestPageResponse<UsuarioClube>>() {}
            );

            return response.getBody();

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return Page.empty();
        }
    }
    /*ta pronto*/
    public List<UsuarioClube> listaParams(UsuarioClube usuario) {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/usuario/listaParams")
                    .toUriString();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<UsuarioClube> entity = new HttpEntity<>(usuario, headers);

            ResponseEntity<List<UsuarioClube>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<UsuarioClube>>() {}
            );

            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /*ta pronto*/
    public UsuarioClube update(Integer id, UsuarioClube usuario) {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/usuario/" + id)
                    .toUriString();

          //  System.out.println("url: " + url);

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<UsuarioClube> entity = new HttpEntity<>(usuario, headers);

            ResponseEntity<UsuarioClube> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    UsuarioClube.class
            );

            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return new UsuarioClube();
        }
    }

    /*ta pronto*/
    public List<UsuarioClube> getAll() {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/usuario")
                    .toUriString();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);

            ResponseEntity<List<UsuarioClube>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<UsuarioClube>>() {}
            );

            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /*ta pronto*/
    public UsuarioClube consultaUsuarioExiste(String loginUsuario) {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("usuario/consultaUsuarioExiste/" + loginUsuario)
                    .toUriString();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<UsuarioClube> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    UsuarioClube.class
            );

            return response.getBody();

        } catch (Exception e) {
            System.err.println(e.getMessage());
            return new UsuarioClube();
        }
    }
    /*ta pronto*/
    public UsuarioClube consultaUsuarioIDExiste(int idUsuarioManger) {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/usuario/consultaUsuarioIDExiste/" + idUsuarioManger)
                    .toUriString();


            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);

            ResponseEntity<UsuarioClube> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, UsuarioClube.class);


            return response.getBody();

        } catch (HttpClientErrorException.NotFound e) {
            return new UsuarioClube();
        } catch (Exception e) {
            e.printStackTrace();
            return new UsuarioClube();
        }
    }

   /* public UsuarioClube consultaUsuarioIDExiste(int idUsuarioManger) {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/usuario/consultaUsuarioIDExiste/" + idUsuarioManger)
                    .toUriString();
          //  System.out.println("aki : "+url);
            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);

            ResponseEntity<UsuarioClube> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    UsuarioClube.class
            );

            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return new UsuarioClube();
        }
    }*/

    /*ta pronto*/
    public List<UnidadeDTO> getDistinctUnidades() {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/usuario/distinctUnidades")
                    .toUriString();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);

            ResponseEntity<List<UnidadeDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<UnidadeDTO>>() {}
            );

            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /*ta pronto*/
    public List<AgenciaDTO> getDistinctAgencias(Integer idWoobaUnidade) {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_CLUBE)
                    .path("/usuario/distinctAgencias/" + idWoobaUnidade)
                    .toUriString();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);

            ResponseEntity<List<AgenciaDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<AgenciaDTO>>() {}
            );

            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
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
