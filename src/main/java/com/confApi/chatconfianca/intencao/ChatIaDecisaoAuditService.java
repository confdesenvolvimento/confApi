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
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ChatIaDecisaoAuditService {
    private static final Logger LOGGER =
            Logger.getLogger(ChatIaDecisaoAuditService.class.getName());

    private final RestTemplate restTemplate;
    private final ConfAppService confAppService;
    private final ChatIntencaoShadowProperties properties;
    private final Executor executor;

    public ChatIaDecisaoAuditService(
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
                          Integer codgUnidade,
                          String baseAtual,
                          ChatConfiancaDecisaoIa decisao,
                          boolean sugerirAtendente,
                          boolean atendenteSolicitado,
                          Long departamentoAtendimentoId,
                          long duracaoTotalMs) {
        if (!properties.isDecisionAuditEnabled()
                || conversaId == null
                || mensagemId == null
                || decisao == null) {
            return;
        }
        ChatIaDecisaoAuditRequest request = montarRequest(
                conversaId, mensagemId, codgUnidade, baseAtual, decisao,
                sugerirAtendente, atendenteSolicitado,
                departamentoAtendimentoId, duracaoTotalMs);
        try {
            executor.execute(() -> enviar(request));
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING,
                    "Fila da auditoria consolidada indisponivel; resposta do chat preservada.", ex);
        }
    }

    public void registrarEncaminhamento(
            Long conversaId, Long departamentoAtendimentoId) {
        if (!properties.isDecisionAuditEnabled() || conversaId == null) {
            return;
        }
        ChatIaEncaminhamentoAuditRequest request =
                new ChatIaEncaminhamentoAuditRequest(
                        conversaId, departamentoAtendimentoId);
        try {
            executor.execute(() -> enviarEncaminhamento(request));
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING,
                    "Fila da auditoria de encaminhamento indisponivel; atendimento preservado.", ex);
        }
    }

    void enviar(ChatIaDecisaoAuditRequest request) {
        try {
            HttpHeaders headers = headersAutenticados();
            restTemplate.exchange(
                    urlAuditoria(),
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    Void.class);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING,
                    "Falha ao registrar decisao consolidada; resposta do chat preservada.", ex);
        }
    }

    void enviarEncaminhamento(ChatIaEncaminhamentoAuditRequest request) {
        try {
            HttpHeaders headers = headersAutenticados();
            restTemplate.exchange(
                    urlAuditoria() + "/encaminhamento",
                    HttpMethod.PUT,
                    new HttpEntity<>(request, headers),
                    Void.class);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING,
                    "Falha ao registrar encaminhamento humano; atendimento preservado.", ex);
        }
    }

    private ChatIaDecisaoAuditRequest montarRequest(
            Long conversaId,
            Long mensagemId,
            Integer codgUnidade,
            String baseAtual,
            ChatConfiancaDecisaoIa decisao,
            boolean sugerirAtendente,
            boolean atendenteSolicitado,
            Long departamentoAtendimentoId,
            long duracaoTotalMs) {
        ChatIntencaoClassificacao classificacao = decisao.getClassificacaoCatalogo();
        ChatIaDecisaoAuditRequest request = new ChatIaDecisaoAuditRequest();
        request.setConversaId(conversaId);
        request.setMensagemId(mensagemId);
        request.setCodgUnidade(codgUnidade);
        request.setBaseAtual(baseAtual);
        request.setIntencaoCatalogoId(classificacao == null ? null : classificacao.getIntencaoId());
        request.setIntencaoCatalogoCodigo(classificacao == null ? null : classificacao.getCodigo());
        request.setIntencaoEfetivaCodigo(decisao.getIntencao());
        request.setIntencaoLegada(decisao.getIntencaoLegada());
        request.setStatusClassificacao(decisao.getStatus());
        request.setConfianca(confianca(decisao, classificacao));
        request.setUnificadaHabilitada(decisao.isUnificadaHabilitada());
        request.setCanarioHabilitado(decisao.isCanarioHabilitado());
        request.setCanarioElegivel(decisao.isCanarioElegivel());
        request.setAplicada(decisao.isAplicada());
        request.setModo(decisao.getModo());
        request.setFonte(decisao.getFonte());
        request.setAcao(decisao.getAcao());
        request.setFerramenta(decisao.getFerramenta());
        request.setDepartamentoSugeridoId(
                decisao.getDepartamento() == null ? null : decisao.getDepartamento().getId());
        request.setDepartamentoSugeridoConfianca(decisao.getDepartamentoConfianca());
        request.setMemorias(decisao.getMemorias() == null ? List.of()
                : decisao.getMemorias().stream()
                .filter(Objects::nonNull)
                .map(ChatIntencaoRuntimeDto.Memoria::getCodgMemoria)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        request.setSugerirAtendente(sugerirAtendente);
        request.setAtendenteSolicitado(atendenteSolicitado);
        request.setDepartamentoAtendimentoId(departamentoAtendimentoId);
        request.setStatusResultado(decisao.getStatusResultado());
        request.setDuracaoTotalMs(Math.max(0L, duracaoTotalMs));
        request.setErroCodigo(decisao.getErroCodigo());
        request.setVersaoDecisor(properties.getVersaoDecisor());
        return request;
    }

    private HttpHeaders headersAutenticados() {
        ConfAppResp token = confAppService.token();
        if (token == null || token.getToken() == null || token.getToken().isBlank()) {
            throw new IllegalStateException("Token de servico indisponivel.");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token.getToken());
        return headers;
    }

    private int confianca(ChatConfiancaDecisaoIa decisao,
                          ChatIntencaoClassificacao classificacao) {
        if ("REGRA_DETERMINISTICA".equals(decisao.getFonte())) {
            return 100;
        }
        return classificacao == null || classificacao.getConfianca() == null
                ? 0 : classificacao.getConfianca();
    }

    private String urlAuditoria() {
        String base = UrlConfig.URL_CONFIANCA_MANAGER;
        if (base == null || base.isBlank()) {
            throw new IllegalStateException("URL do Confianca Manager nao configurada.");
        }
        return (base.endsWith("/") ? base : base + "/")
                + "chatMemoria/runtime/decisoes";
    }
}
