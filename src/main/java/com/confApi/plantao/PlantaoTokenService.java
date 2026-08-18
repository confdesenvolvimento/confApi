package com.confApi.plantao;

import com.confApi.config.UrlConfig;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PlantaoTokenService {

    private static final String CHAVE_TOKEN_EMAIL = "0003PLANTAO";
    private static final String CHAVE_TOKEN_APP = "0004PLANTAO";
    private final RestTemplate restTemplate;
    private final String cservicesUrl;

    public PlantaoTokenService(@Qualifier("restTemplate") RestTemplate restTemplate,
                               @Value("${cservices-url}") String cservicesUrl) {
        this.restTemplate = restTemplate;
        this.cservicesUrl = cservicesUrl;
    }

    public PlantaoTokenResponse gerar(PlantaoTokenRequest request, String authorization) {
        if (request == null || !StringUtils.hasText(request.login()) || chaveToken(request.canal()) == null) {
            return PlantaoTokenResponse.falha("Login e chave do token s?o obrigat?rios.");
        }

        String url = UriComponentsBuilder.fromHttpUrl(cservicesUrl)
                .pathSegment("api", "token", "gerartoken", chaveToken(request.canal()), request.login().trim())
                .toUriString();
        try {
            ResponseEntity<CserviceTokenResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers()), CserviceTokenResponse.class);
            String token = response.getBody() == null ? null : response.getBody().valor();
            if (!StringUtils.hasText(token)) {
                return PlantaoTokenResponse.falha("N?o foi poss?vel gerar o token.");
            }

            if ("EMAIL".equalsIgnoreCase(request.canal())
                    && !enviarSmsParaUsuario(request.login().trim(), token, authorization, request.celular())) {
                return PlantaoTokenResponse.falha("Token gerado, mas n?o foi poss?vel envi?-lo por SMS.");
            }
            return PlantaoTokenResponse.sucesso(token);
        } catch (RestClientException exception) {
            return PlantaoTokenResponse.falha("Servi?o de token indispon?vel.");
        }
    }

    public PlantaoTokenResponse validar(PlantaoTokenValidationRequest request) {
        if (request == null || !StringUtils.hasText(request.login()) || chaveToken(request.canal()) == null
                || !StringUtils.hasText(request.token())) {
            return PlantaoTokenResponse.falha("Token inv?lido.");
        }

        Integer token;
        try {
            token = Integer.valueOf(request.token().trim());
        } catch (NumberFormatException exception) {
            return PlantaoTokenResponse.falha("Token inv?lido.");
        }

        String url = UriComponentsBuilder.fromHttpUrl(cservicesUrl)
                .pathSegment("api", "token", "validar", token.toString(), chaveToken(request.canal()))
                .toUriString();
        try {
            restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(Map.of("usuarioLoginAcesso", request.login().trim()), headers()), String.class);
            return PlantaoTokenResponse.sucesso(null);
        } catch (RestClientException exception) {
            return PlantaoTokenResponse.falha("Token inv?lido ou expirado.");
        }
    }
    private String chaveToken(String canal) {
        if (!StringUtils.hasText(canal)) {
            return null;
        }
        if ("EMAIL".equalsIgnoreCase(canal)) {
            return CHAVE_TOKEN_EMAIL;
        }
        if ("APP".equalsIgnoreCase(canal)) {
            return CHAVE_TOKEN_APP;
        }
        return null;

    }
    private boolean enviarSmsParaUsuario(String login, String token, String authorization, String celularInformado) {
        String celular = consultarCelular(login, celularInformado);
        if (!StringUtils.hasText(celular) || !StringUtils.hasText(UrlConfig.URL_CONFIANCA_HUB)) {
            return false;
        }

        String url = UriComponentsBuilder.fromHttpUrl(UrlConfig.URL_CONFIANCA_HUB)
                .pathSegment("menssage")
                .toUriString();
        HttpHeaders headers = headers();
        if (StringUtils.hasText(authorization)) {
            headers.set("Authorization", authorization);
        }
        HubMenssage menssage = new HubMenssage("account", celular,
                List.of(new HubMenssageContent("text", "Seu token de plantao e: " + token)));
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(menssage, headers), String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException exception) {
            return false;
        }
    }

    private String consultarCelular(String login, String celularInformado) {
        if (!StringUtils.hasText(UrlConfig.URL_CONFIANCA_MANAGER)) {
            return null;
        }
        String url = UriComponentsBuilder.fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER)
                .pathSegment("wooba", "turUsuarios", "loginCservice", login)
                .toUriString();
        try {
            ResponseEntity<UsuarioPlantaoResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers()), UsuarioPlantaoResponse.class);
            String celularManager = response.getBody() == null ? null : response.getBody().celular();
            return StringUtils.hasText(celularManager) ? celularManager : celularInformado;
        } catch (RestClientException exception) {
            // Em desenvolvimento o Manager pode estar indisponivel; o Payara ja consultou este dado.
        }
        return celularInformado;
    }
    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private record UsuarioPlantaoResponse(String celular) {
    }

    private record HubMenssage(String from, String to, List<HubMenssageContent> contents) {
    }

    private record HubMenssageContent(String type, String text) {
    }
    private record CserviceTokenResponse(String valor) {
    }
}
