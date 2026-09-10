package com.confApi.chatconfianca.configuracao;

import com.confApi.chatconfianca.dto.model.DepartamentoUnidade;
import com.confApi.chatconfianca.intencao.ChatConfiancaDecisaoIa;
import com.confApi.chatconfianca.intencao.ChatIntencaoRuntimeDto;
import com.confApi.chatconfianca.v2.ChatV2Capability;
import com.confApi.chatconfianca.v2.ChatV2Plan;
import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.confApi.chatconfianca.configuracao.ChatIaConfiguracaoResolver.Contexto;
import com.confApi.chatconfianca.configuracao.ChatIaFalhaShadow.*;

/** Additional, bounded diagnostic lane. Its return value cannot affect the live conversation. */
@Service
@ConditionalOnProperty(prefix = "chat-confianca.ia-config", name = "shadow-enabled", havingValue = "true")
public class ChatIaConfiguracaoShadowService {
    private static final Logger LOG = Logger.getLogger(ChatIaConfiguracaoShadowService.class.getName());
    private final RestTemplate http;
    private final ConfAppService auth;
    private final Executor executor;
    private final ObjectMapper mapper;
    private final int samplePercent;
    private final AtomicLong indisponivelAte = new AtomicLong();
    private final ChatIaConfiguracaoResolver resolver = new ChatIaConfiguracaoResolver();
    private final ChatIaContinuidadeShadow continuidade = new ChatIaContinuidadeShadow();

    public ChatIaConfiguracaoShadowService(@Qualifier("chatIaConfiguracaoShadowHttp") RestTemplate http,
            ConfAppService auth, @Qualifier("chatIaConfiguracaoShadowExecutor") Executor executor,
            ObjectMapper mapper, @Value("${chat-confianca.ia-config.shadow-sample-percent:10}") int samplePercent) {
        this.http = http; this.auth = auth; this.executor = executor; this.mapper = mapper;
        this.samplePercent = Math.max(0, Math.min(100, samplePercent));
    }

    public void observar(Long conversaId, Long mensagemId, String base, Integer unidade,
            ChatConfiancaDecisaoIa decisao, ChatV2Plan plano, boolean humanoExplicito,
            List<DepartamentoUnidade> departamentos) {
        observar(conversaId, mensagemId, base, unidade, decisao, plano, humanoExplicito,
                departamentos, null, null, null, false);
    }

    public void observar(Long conversaId, Long mensagemId, String base, Integer unidade,
            ChatConfiancaDecisaoIa decisao, ChatV2Plan plano, boolean humanoExplicito,
            List<DepartamentoUnidade> departamentos, Integer usuario, Integer agencia,
            String mensagemAtual, boolean conversaAssistida) {
        long inicio = System.nanoTime();
        Etapa etapa = Etapa.CAPTURA_CONTEXTO;
        try {
            if (decisao == null || conversaId == null || mensagemId == null
                    || Math.floorMod(Long.hashCode(conversaId), 100) >= samplePercent) return;
            Contexto capturado = capturar(conversaId, mensagemId, base, unidade, decisao, plano, humanoExplicito, departamentos);
            var classificacao = decisao.getClassificacaoCatalogo();
            // Track the topic before cooldown/queue checks, so an HTTP failure cannot erase it.
            Contexto contexto = continuidade.observar(capturado, usuario, agencia, mensagemAtual, conversaAssistida,
                    classificacao != null && "SEM_EVIDENCIA".equals(classificacao.getStatus()));
            if (System.currentTimeMillis() < indisponivelAte.get()) return;
            // The worker retains only immutable metadata, never the message, user request or session.
            etapa = Etapa.FILA;
            executor.execute(() -> comparar(contexto));
        } catch (RuntimeException ex) {
            registrarFalha("IA_CONFIG_SHADOW_DESCARTADO", conversaId, mensagemId, etapa, inicio, 0, ex);
        }
    }

    static Contexto capturar(Long conversaId, Long mensagemId, String base, Integer unidade,
            ChatConfiancaDecisaoIa decisao, ChatV2Plan plano, boolean humanoExplicito,
            List<DepartamentoUnidade> departamentos) {
        var classificacao = decisao.getClassificacaoCatalogo();
        String atual = decisao.getIntencao();
        String consulta = decisao.isAplicada() ? atual : classificacao == null ? null : classificacao.getCodigo();
        boolean humano = humanoExplicito || ChatV2Capability.HUMANO.code.equals(atual)
                || (plano != null && plano.capability() == ChatV2Capability.HUMANO);
        if (humano && plano != null && ChatV2Capability.from(plano.getAssuntoHandoff()) != null)
            consulta = plano.getAssuntoHandoff();
        Integer confianca = classificacao != null && Objects.equals(consulta, classificacao.getCodigo())
                && "CLASSIFICADA".equals(classificacao.getStatus()) ? classificacao.getConfianca() : null;
        String fonteConfianca = confianca == null ? "NAO_DISPONIVEL" : "CLASSIFICADOR_LEGADO_MESMA_INTENCAO";
        if ("REGRA_DETERMINISTICA".equals(decisao.getFonte()) && Objects.equals(consulta, atual)) {
            confianca = 100; fonteConfianca = "REGRA_DETERMINISTICA";
        }
        Set<Long> permitidos = departamentos == null ? Set.of() : departamentos.stream().filter(Objects::nonNull)
                .filter(d -> Boolean.TRUE.equals(d.getAtivo()) && d.getDepartamentoId() != null)
                .filter(d -> unidade != null && unidade.equals(d.getCodgUnidade()))
                .map(DepartamentoUnidade::getDepartamentoId).collect(Collectors.toSet());
        Set<Integer> memorias = decisao.getMemorias() == null ? Set.of() : decisao.getMemorias().stream()
                .filter(Objects::nonNull).map(ChatIntencaoRuntimeDto.Memoria::getCodgMemoria)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        return new Contexto(conversaId, mensagemId, atual, consulta, base, unidade, confianca,
                humano, permitidos, memorias, plano == null ? decisao.getStatusResultado() : plano.getResultado(),
                decisao.getFonte(), fonteConfianca);
    }

    void comparar(Contexto contexto) {
        if (System.currentTimeMillis() < indisponivelAte.get()) return;
        long inicio = System.nanoTime();
        Etapa etapa = Etapa.VALIDACAO_URL;
        try {
            ChatIaConfiguracaoSnapshot snapshot = new ChatIaConfiguracaoSnapshot();
            String codigo = contexto.intencaoConsulta();
            if (codigo != null && codigo.matches("[a-z][a-z0-9_.-]{0,119}")) {
                String base = UrlConfig.URL_CONFIANCA_MANAGER;
                if (base == null || base.isBlank()) throw new Falha(Codigo.URL_AUSENTE);
                var uri = UriComponentsBuilder.fromHttpUrl(base.endsWith("/") ? base : base + "/")
                        .path("chatIa/runtime/configuracao").queryParam("intencao", codigo).build().encode().toUri();
                etapa = Etapa.AUTENTICACAO;
                var token = auth.token();
                if (token == null || token.getToken() == null || token.getToken().isBlank())
                    throw new Falha(Codigo.TOKEN_AUSENTE);
                HttpHeaders headers = new HttpHeaders(); headers.setBearerAuth(token.getToken());
                headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                etapa = Etapa.CONSULTA_CONFIGURACAO;
                snapshot = http.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), ChatIaConfiguracaoSnapshot.class).getBody();
                if (snapshot == null) throw new Falha(Codigo.RESPOSTA_VAZIA);
            }
            etapa = Etapa.RESOLUCAO;
            var resultado = resolver.resolver(snapshot, contexto, LocalDate.now());
            etapa = Etapa.SERIALIZACAO;
            // Separate from decision-audit tables: this is not an answer delivered to the customer.
            LOG.info("IA_CONFIG_SHADOW " + mapper.writeValueAsString(Map.of("contexto", contexto, "proposta", resultado)));
        } catch (Exception ex) {
            indisponivelAte.set(System.currentTimeMillis() + 30_000L);
            registrarFalha("IA_CONFIG_SHADOW_INDISPONIVEL", contexto.conversaId(), contexto.mensagemId(),
                    etapa, inicio, 30, ex);
        }
    }

    private static void registrarFalha(String evento, Long conversa, Long mensagem, Etapa etapa,
            long inicio, int pausaSegundos, Exception ex) {
        var falha = ChatIaFalhaShadow.descrever(ex);
        // Do not attach exceptions, messages, bodies, URLs, tokens or request/response content.
        LOG.warning(evento + " conversa=" + conversa + " mensagem=" + mensagem + " etapa=" + etapa
                + " erroCodigo=" + falha.codigo()
                + (falha.httpStatus() == null ? "" : " httpStatus=" + falha.httpStatus())
                + " duracaoMs=" + Math.max(0L, (System.nanoTime() - inicio) / 1_000_000L)
                + " pausaSegundos=" + pausaSegundos + "; resposta preservada.");
    }
}
