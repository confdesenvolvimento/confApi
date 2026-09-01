package com.confApi.chatconfianca.intencao;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ChatIntencaoShadowService {

    private static final Logger LOGGER = Logger.getLogger(ChatIntencaoShadowService.class.getName());

    private final RestTemplate restTemplate;
    private final ConfAppService confAppService;
    private final ChatIntencaoTermoClassifier classifier;
    private final ChatIntencaoShadowProperties properties;
    private final ChatIntencaoShadowAuditService auditService;
    private final AtomicBoolean carregando = new AtomicBoolean(false);
    private volatile List<ChatIntencaoRuntimeDto> perfis = Collections.emptyList();
    private volatile boolean cacheInicializado;
    private volatile Instant atualizadoEm;

    public ChatIntencaoShadowService(
            @Qualifier("chatConfiancaRestTemplate") RestTemplate restTemplate,
            ConfAppService confAppService,
            ChatIntencaoTermoClassifier classifier,
            ChatIntencaoShadowProperties properties,
            ChatIntencaoShadowAuditService auditService) {
        this.restTemplate = restTemplate;
        this.confAppService = confAppService;
        this.classifier = classifier;
        this.properties = properties;
        this.auditService = auditService;
    }

    @Scheduled(
            fixedDelayString = "${chat-confianca.intencao-v1.refresh-ms:120000}",
            initialDelayString = "${chat-confianca.intencao-v1.initial-delay-ms:15000}")
    public void atualizarCache() {
        if (!properties.isShadowEnabled() || !carregando.compareAndSet(false, true)) {
            return;
        }
        try {
            ConfAppResp token = confAppService.token();
            if (token == null || token.getToken() == null || token.getToken().isBlank()) {
                throw new IllegalStateException("Token de servico indisponivel.");
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(token.getToken());
            ResponseEntity<List<ChatIntencaoRuntimeDto>> response = restTemplate.exchange(
                    urlRuntime(),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<List<ChatIntencaoRuntimeDto>>() {});
            List<ChatIntencaoRuntimeDto> recebidos = response.getBody();
            perfis = recebidos == null ? Collections.emptyList() : List.copyOf(recebidos);
            cacheInicializado = true;
            atualizadoEm = Instant.now();
            LOGGER.log(Level.INFO,
                    "Classificador de intencao V1 atualizado em modo sombra: {0} intencoes ativas.",
                    perfis.size());
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING,
                    "Falha ao atualizar classificador de intencao V1; cache anterior preservado.", ex);
        } finally {
            carregando.set(false);
        }
    }

    public ChatIntencaoClassificacao classificar(String mensagem) {
        return classificar(mensagem, null, null);
    }

    public ChatIntencaoClassificacao classificar(String mensagem,
                                                 Integer codgUnidade,
                                                 String baseAtual) {
        if (!properties.isShadowEnabled()) {
            return ChatIntencaoClassificacao.status("DESABILITADO");
        }
        if (!cacheInicializado) {
            return ChatIntencaoClassificacao.status("AGUARDANDO_CARGA");
        }
        ChatIntencaoClassificacao resultado = classifier.classificar(
                mensagem,
                perfis,
                properties.getMinScore(),
                properties.getMinMargin());
        enriquecerRecuperacaoMemoria(resultado, codgUnidade, baseAtual);
        return resultado;
    }

    public void registrarComparacao(Long conversaId,
                                    Long mensagemId,
                                    String intencaoAtual,
                                    ChatIntencaoClassificacao sombra) {
        if (sombra == null || !sombra.possuiResultadoObservavel()) {
            return;
        }
        boolean coincide = sombra.getCodigo() != null
                && Objects.equals(normalizarCodigo(intencaoAtual), normalizarCodigo(sombra.getCodigo()));
        LOGGER.log(Level.INFO,
                "chat_intencao_shadow conversa={0} mensagem={1} status={2} atual={3} nova={4} confianca={5} coincide={6} memoria_status={7} memorias={8}",
                new Object[]{conversaId, mensagemId, sombra.getStatus(), intencaoAtual,
                        sombra.getCodigo(), sombra.getConfianca(), coincide,
                        sombra.getStatusRecuperacaoMemoria(), sombra.getMemoriasRecuperadas()});
        auditService.registrar(conversaId, mensagemId, intencaoAtual, sombra);
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    private void enriquecerRecuperacaoMemoria(ChatIntencaoClassificacao resultado,
                                              Integer codgUnidade,
                                              String baseAtual) {
        long inicio = System.nanoTime();
        try {
            enriquecerRecuperacaoMemoriaInterno(resultado, codgUnidade, baseAtual);
        } finally {
            resultado.setTempoRecuperacaoMemoriaMs(
                    Math.max(0L, (System.nanoTime() - inicio) / 1_000_000L));
        }
    }

    private void enriquecerRecuperacaoMemoriaInterno(ChatIntencaoClassificacao resultado,
                                                     Integer codgUnidade,
                                                     String baseAtual) {
        if (!properties.isMemoryShadowEnabled()) {
            resultado.setStatusRecuperacaoMemoria("DESABILITADO");
            return;
        }
        if (!"CLASSIFICADA".equals(resultado.getStatus()) || resultado.getIntencaoId() == null) {
            resultado.setStatusRecuperacaoMemoria("NAO_APLICAVEL");
            return;
        }
        ChatIntencaoRuntimeDto perfil = perfis.stream()
                .filter(item -> Objects.equals(item.getId(), resultado.getIntencaoId()))
                .findFirst()
                .orElse(null);
        if (perfil == null || perfil.getMemorias() == null || perfil.getMemorias().isEmpty()) {
            resultado.setStatusRecuperacaoMemoria("SEM_MEMORIA");
            return;
        }
        List<ChatIntencaoRuntimeDto.Memoria> memoriasCompativeis = perfil.getMemorias().stream()
                .filter(memoria -> memoriaCompativelComEscopo(
                        memoria, codgUnidade, baseAtual))
                .toList();
        resultado.setMemoriasDetalhadas(List.copyOf(memoriasCompativeis));
        resultado.setMemoriasRecuperadas(memoriasCompativeis.stream()
                .map(ChatIntencaoRuntimeDto.Memoria::getCodgMemoria)
                .filter(Objects::nonNull)
                .toList());
        resultado.setStatusRecuperacaoMemoria(
                resultado.getMemoriasRecuperadas().isEmpty() ? "SEM_MEMORIA" : "RECUPERADA");
    }

    private boolean memoriaCompativelComEscopo(ChatIntencaoRuntimeDto.Memoria memoria,
                                               Integer codgUnidade,
                                               String baseAtual) {
        if (memoria == null) {
            return false;
        }
        if (memoria.getCodgUnidade() != null) {
            return Objects.equals(memoria.getCodgUnidade(), codgUnidade);
        }
        String baseMemoria = normalizarBase(memoria.getBase());
        if ("geral".equals(baseMemoria)) {
            return true;
        }
        String baseConversa = normalizarBase(baseAtual);
        return !baseConversa.isEmpty() && Objects.equals(baseMemoria, baseConversa);
    }

    private String urlRuntime() {
        String base = UrlConfig.URL_CONFIANCA_MANAGER;
        if (base == null || base.isBlank()) {
            throw new IllegalStateException("URL do Confianca Manager nao configurada.");
        }
        return (base.endsWith("/") ? base : base + "/")
                + "chatMemoria/runtime/classificador";
    }

    private String normalizarCodigo(String valor) {
        return valor == null ? null : valor.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String normalizarBase(String valor) {
        if (valor == null) {
            return "";
        }
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
