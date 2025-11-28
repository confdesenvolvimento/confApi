package com.confApi.endPoints.usuario;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.db.confManager.usuario.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;

@Service
public class UsuarioApi {

    @Autowired
    private ConfAppService confAppService;

    private final RestTemplate restTemplate;

    public UsuarioApi(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    public Usuario consultaUsuarioByLogin(String loginUsuario) {
        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER)
                    .path("/usuario/findByLogin/" + loginUsuario)
                    .toUriString();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<?> entity =
                    new HttpEntity<>(null, headers);

            ResponseEntity<Usuario> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Usuario>() {
                    }
            );
            return response.getBody();
        } catch (Exception e) {
            Usuario erro = new Usuario();
            System.out.println(e.getMessage());
            return erro;
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
