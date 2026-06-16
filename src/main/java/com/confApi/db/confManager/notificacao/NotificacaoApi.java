package com.confApi.db.confManager.notificacao;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.util.TelegramErrorAlert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class NotificacaoApi {

    private static final Logger LOG = Logger.getLogger(NotificacaoApi.class.getName());

    private final RestTemplate restTemplate;
    private final ConfAppService confAppService;

    @Autowired(required = false)
    private TelegramErrorAlert telegramErrorAlert;

    public NotificacaoApi(RestTemplate restTemplate, ConfAppService confAppService) {
        this.restTemplate = restTemplate;
        this.confAppService = confAppService;
    }

    public void criarParaUsuario(NotificacaoConfigDTO notificacao) {
        if (notificacao == null || notificacao.getCodgUsuario() == null) {
            return;
        }

        try {
            ConfAppResp token = confAppService.token();
            String url = UriComponentsBuilder
                    .fromHttpUrl(UrlConfig.URL_CONFIANCA_MANAGER)
                    .path("/notificacao/criarNotificacaoDTO")
                    .queryParam("tipo", "exclusivo")
                    .toUriString();

            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(notificacao, defaultHeaders(token.getToken())),
                    Object.class
            );
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Nao foi possivel criar notificacao da reserva Wooba.", e);
            alertarErro("Nao foi possivel criar notificacao da reserva Wooba", e);
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
