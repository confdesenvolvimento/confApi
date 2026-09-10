package com.confApi.chatconfianca.configuracao.ti;

import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.List;

/** Fresh authenticated content for each eligible turn. No shared cache, no customer data in the request. */
@Component
@ConditionalOnProperty(prefix = "chat-confianca.ia-config.ti-resposta", name = "enabled", havingValue = "true")
public class ChatIaTiConteudoClient {
    private final RestTemplate http;
    private final ConfAppService auth;
    public ChatIaTiConteudoClient(@Qualifier("chatIaTiHttp") RestTemplate http, ConfAppService auth) {
        this.http = http; this.auth = auth;
    }
    public ChatIaTiDocumento carregar() {
        String base = UrlConfig.URL_CONFIANCA_MANAGER;
        if (base == null || base.isBlank()) throw new IllegalStateException("URL_TI_AUSENTE");
        var uri = UriComponentsBuilder.fromHttpUrl(base.endsWith("/") ? base : base + "/")
                .path("chatIa/runtime/ti-resposta").build().encode().toUri();
        var token = auth.token();
        if (token == null || token.getToken() == null || token.getToken().isBlank())
            throw new IllegalStateException("TOKEN_TI_AUSENTE");
        var headers = new HttpHeaders();
        headers.setBearerAuth(token.getToken()); headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        var response = http.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), ChatIaTiDocumento.class);
        if (response.getStatusCode() == HttpStatus.NO_CONTENT) return null;
        if (response.getStatusCode() != HttpStatus.OK) throw new IllegalStateException("STATUS_TI_INVALIDO");
        return response.getBody();
    }
}
