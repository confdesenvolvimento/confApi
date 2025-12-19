package com.confApi.endPoints.usuario;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.db.confManager.usuario.dto.AuthRequestDto;
import com.confApi.db.confManager.usuario.dto.UsuarioDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
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

    public Object consultaUsuarioByLoginWooba(String loginUsuario) {
        try {
            ConfAppResp token = confAppService.token();

            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER)
                    .path("/wooba/turUsuarios/loginDB/" + loginUsuario)
                    .toUriString();

            HttpHeaders headers = defaultHeaders(token.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Object.class
            );

            return response.getBody();

        } catch (Exception e) {
            System.err.println(e.getMessage());
            return e.getMessage();
        }
    }

    public ResponseEntity<UsuarioDto> autenficarUsuarioAuth(AuthRequestDto requestDto) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + new ConfAppService().token());

        HttpEntity<AuthRequestDto> requestEntity =
                new HttpEntity<>(requestDto, headers);

        try {
            ResponseEntity<UsuarioDto> responseEntity =
                    new RestTemplate().exchange(
                            UrlConfig.URL_CONFIANCA_MANAGER + "/usuario/auth",
                            HttpMethod.POST,
                            requestEntity,
                            UsuarioDto.class
                    );

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                return responseEntity; // retorna UsuarioDto no body
            }

            return ResponseEntity.status(responseEntity.getStatusCode()).build();

        } catch (HttpClientErrorException ex) {
            // log / tratamento
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .build();
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
