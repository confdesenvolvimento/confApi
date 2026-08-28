package com.confApi.chatconfianca.intencao;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ChatIntencaoShadowAuditService {

    private static final Logger LOGGER =
            Logger.getLogger(ChatIntencaoShadowAuditService.class.getName());

    private final RestTemplate restTemplate;
    private final ConfAppService confAppService;
    private final ChatIntencaoShadowProperties properties;
    private final Executor executor;

    public ChatIntencaoShadowAuditService(
            @Qualifier("chatConfiancaRestTemplate") RestTemplate restTemplate,
            ConfAppService confAppService,
            ChatIntencaoShadowProperties properties,
            @Qualifier("chatIntencaoAuditExecutor") Executor executor) {
        this.restTemplate = restTemplate;
        this.confAppService = confAppService;
        this.properties = properties;
        this.executor = executor;
    }

    public void registrar(Long conversaId,
                          Long mensagemId,
                          String intencaoAtual,
                          ChatIntencaoClassificacao sombra) {
        if (!properties.isShadowEnabled()
                || !properties.isAuditEnabled()
                || conversaId == null
                || mensagemId == null
                || sombra == null
                || !sombra.possuiResultadoObservavel()) {
            return;
        }
        ChatIntencaoShadowAuditRequest request =
                montarRequest(conversaId, mensagemId, intencaoAtual, sombra);
        try {
            executor.execute(() -> enviar(request));
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING,
                    "Fila de auditoria do classificador de intencao indisponivel; chat preservado.", ex);
        }
    }

    void enviar(ChatIntencaoShadowAuditRequest request) {
        try {
            ConfAppResp token = confAppService.token();
            if (token == null || token.getToken() == null || token.getToken().isBlank()) {
                throw new IllegalStateException("Token de servico indisponivel.");
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token.getToken());
            restTemplate.exchange(
                    urlAuditoria(),
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    Void.class);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING,
                    "Falha ao registrar auditoria do classificador; resposta do chat preservada.", ex);
        }
    }

    private ChatIntencaoShadowAuditRequest montarRequest(
            Long conversaId,
            Long mensagemId,
            String intencaoAtual,
            ChatIntencaoClassificacao sombra) {
        ChatIntencaoShadowAuditRequest request = new ChatIntencaoShadowAuditRequest();
        request.setConversaId(conversaId);
        request.setMensagemId(mensagemId);
        request.setIntencaoAtual(intencaoAtual);
        request.setIntencaoClassificadaId(sombra.getIntencaoId());
        request.setIntencaoClassificadaCodigo(sombra.getCodigo());
        request.setStatusClassificacao(sombra.getStatus());
        request.setScore(sombra.getScore());
        request.setSegundoScore(sombra.getSegundoScore());
        request.setConfianca(sombra.getConfianca());
        request.setTermosPositivos(copia(sombra.getTermosPositivos()));
        request.setTermosNegativos(copia(sombra.getTermosNegativos()));
        request.setFonte(sombra.getFonte());
        request.setVersaoClassificador(properties.getVersaoClassificador());
        return request;
    }

    private List<String> copia(List<String> valores) {
        return valores == null ? List.of() : List.copyOf(valores);
    }

    private String urlAuditoria() {
        String base = UrlConfig.URL_CONFIANCA_MANAGER;
        if (base == null || base.isBlank()) {
            throw new IllegalStateException("URL do Confianca Manager nao configurada.");
        }
        return (base.endsWith("/") ? base : base + "/")
                + "chatMemoria/runtime/classificacoes";
    }
}
