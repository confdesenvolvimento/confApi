package com.confApi.endPoints.usuario;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.db.confManager.usuario.Usuario;
import com.confApi.db.confManager.usuario.dto.AuthRequestDto;
import com.confApi.db.confManager.usuario.dto.UsuarioDto;
import com.confApi.exception.RegraDeNegocioException;
import com.confApi.exception.ServiceIndisponivelException;
import com.confApi.util.TelegramErrorAlert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class UsuarioApi {

    private static final Logger LOG = Logger.getLogger(UsuarioApi.class.getName());

    @Autowired
    private ConfAppService confAppService;

    @Autowired(required = false)
    private TelegramErrorAlert telegramErrorAlert;

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
            LOG.log(Level.SEVERE, "Erro ao consultar usuario por login: " + loginUsuario, e);
            alertarErro("Erro ao consultar usuario por login " + loginUsuario, e);
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
            LOG.log(Level.SEVERE, "Erro ao consultar usuario Wooba por login: " + loginUsuario, e);
            alertarErro("Erro ao consultar usuario Wooba por login " + loginUsuario, e);
            return e.getMessage();
        }
    }
/*
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
            LOG.log(Level.SEVERE, "Erro ao autenticar usuario no Manager.", ex);
            alertarErro("Erro ao autenticar usuario no Manager", ex);
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .build();
        }
    }*/

    public ResponseEntity<UsuarioDto> autenficarUsuarioAuth(AuthRequestDto requestDto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + new ConfAppService().token());
        HttpEntity<AuthRequestDto> requestEntity = new HttpEntity<>(requestDto, headers);

        try {
            return new RestTemplate().exchange(
                    UrlConfig.URL_CONFIANCA_MANAGER + "/usuario/auth",
                    HttpMethod.POST,
                    requestEntity,
                    UsuarioDto.class
            );

        } catch (HttpClientErrorException ex) {
            // 4xx — credencial inválida, não encontrado, etc
            LOG.log(Level.SEVERE, "Erro ao autenticar usuario no Manager.", ex);
            alertarErro("Erro 4xx ao autenticar usuario no Manager", ex);
            throw new RegraDeNegocioException(ex.getStatusCode().value(), ex.getMessage());

        } catch (ResourceAccessException ex) {
            // Manager fora do ar
            LOG.log(Level.SEVERE, "Manager indisponível: " + UrlConfig.URL_CONFIANCA_MANAGER, ex);
            alertarErro("⚠️ Manager FORA DO AR - autenticação indisponível", ex);
            throw new ServiceIndisponivelException("Serviço de autenticação indisponível. Tente novamente.");
        }
    }

    private void alertarErro(String mensagem, Exception e) {
        if (telegramErrorAlert != null) {
            telegramErrorAlert.enviar(this, mensagem, e);
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
