package com.confApi.chatconfianca.intencao;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.confApi.db.confManager.chatMemoria.ChatMemoriaService;
import com.confApi.db.confManager.chatMemoria.dto.ChatMemoria;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ChatMemoriaRecuperacaoShadowAuditService {

    private static final Logger LOGGER = Logger.getLogger(
            ChatMemoriaRecuperacaoShadowAuditService.class.getName());

    private final RestTemplate restTemplate;
    private final ConfAppService confAppService;
    private final ChatMemoriaService chatMemoriaService;
    private final ChatIntencaoShadowProperties properties;
    private final Executor executor;

    public ChatMemoriaRecuperacaoShadowAuditService(
            @Qualifier("chatConfiancaRestTemplate") RestTemplate restTemplate,
            ConfAppService confAppService,
            ChatMemoriaService chatMemoriaService,
            ChatIntencaoShadowProperties properties,
            @Qualifier("chatIntencaoAuditExecutor") Executor executor) {
        this.restTemplate = restTemplate;
        this.confAppService = confAppService;
        this.chatMemoriaService = chatMemoriaService;
        this.properties = properties;
        this.executor = executor;
    }

    public void registrar(Long conversaId,
                          Long mensagemId,
                          String baseAtual,
                          ChatIntencaoClassificacao sombra) {
        if (!properties.isShadowEnabled()
                || !properties.isMemoryShadowEnabled()
                || !properties.isRecoveryAuditEnabled()
                || conversaId == null
                || mensagemId == null
                || sombra == null
                || !"CLASSIFICADA".equals(sombra.getStatus())) {
            return;
        }

        ChatMemoriaRecuperacaoAuditRequest request = montarRequest(
                conversaId, mensagemId, baseAtual, sombra);
        try {
            executor.execute(() -> compararEEnviar(request));
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING,
                    "Fila da auditoria de memorias indisponivel; resposta do chat preservada.", ex);
        }
    }

    void compararEEnviar(ChatMemoriaRecuperacaoAuditRequest request) {
        try {
            long inicioAtual = System.nanoTime();
            List<ChatMemoria> atuais = chatMemoriaService.findByBase(request.getBaseAtual());
            request.setTempoAtualMs(Math.max(0L,
                    (System.nanoTime() - inicioAtual) / 1_000_000L));
            request.setMemoriasAtuais(idsDistintos(atuais));

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
                    "Falha ao comparar recuperacao de memorias; resposta do chat preservada.", ex);
        }
    }

    private ChatMemoriaRecuperacaoAuditRequest montarRequest(
            Long conversaId,
            Long mensagemId,
            String baseAtual,
            ChatIntencaoClassificacao sombra) {
        ChatMemoriaRecuperacaoAuditRequest request =
                new ChatMemoriaRecuperacaoAuditRequest();
        request.setConversaId(conversaId);
        request.setMensagemId(mensagemId);
        request.setIntencaoClassificadaId(sombra.getIntencaoId());
        request.setIntencaoClassificadaCodigo(sombra.getCodigo());
        request.setStatusClassificacao(sombra.getStatus());
        request.setConfianca(sombra.getConfianca());
        request.setBaseAtual(baseAtual == null || baseAtual.isBlank()
                ? "Confianca" : baseAtual);
        request.setStatusRecuperacaoNova(sombra.getStatusRecuperacaoMemoria());
        request.setMemoriasNovas(sombra.getMemoriasRecuperadas() == null
                ? List.of() : List.copyOf(sombra.getMemoriasRecuperadas()));
        request.setTempoNovoMs(sombra.getTempoRecuperacaoMemoriaMs());
        request.setVersaoRecuperador(properties.getVersaoRecuperador());
        return request;
    }

    private List<Integer> idsDistintos(List<ChatMemoria> memorias) {
        if (memorias == null) {
            return List.of();
        }
        return memorias.stream()
                .filter(Objects::nonNull)
                .map(ChatMemoria::getCodgMemoria)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String urlAuditoria() {
        String base = UrlConfig.URL_CONFIANCA_MANAGER;
        if (base == null || base.isBlank()) {
            throw new IllegalStateException("URL do Confianca Manager nao configurada.");
        }
        return (base.endsWith("/") ? base : base + "/")
                + "chatMemoria/runtime/recuperacoes";
    }
}
