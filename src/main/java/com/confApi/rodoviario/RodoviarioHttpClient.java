package com.confApi.rodoviario;

import com.confApi.confApp.ConfAppService;
import com.confApi.config.UrlConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.*;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.List;
import java.util.Locale;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Component
public class RodoviarioHttpClient {
    private static final Logger log = LoggerFactory.getLogger(RodoviarioHttpClient.class);
    public enum Servico { HUB, MANAGER }
    private final RestTemplate rest;
    private final ConfAppService auth;
    private final ObjectMapper mapper;
    public RodoviarioHttpClient(@Qualifier("rodoviarioRestTemplate") RestTemplate rest,
                                ConfAppService auth, ObjectMapper mapper) {
        this.rest = rest;
        this.auth = auth;
        this.mapper = mapper;
    }
    public <T> T trocar(Servico servico, String rota, HttpMethod metodo, Object corpo,
                        MultiValueMap<String, String> parametros, String usuario, Class<T> tipo, boolean efeito) {
        long start = System.nanoTime();
        String requestId = MDC.get(RodoviarioRequestLoggingFilter.REQUEST_ID);
        log.info("Rodoviario integracao inicio requestId={} servico={} metodo={} rota={}",
                requestId, servico, metodo, RodoviarioRequestLoggingFilter.safe(rota));
        try {
            T result = executar(servico, rota, metodo, corpo, parametros, usuario, tipo, efeito);
            log.info("Rodoviario integracao concluida requestId={} servico={} duracaoMs={}",
                    requestId, servico, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            return result;
        } catch (RodoviarioIntegracaoException ex) {
            // Nao registrar mensagem, causa ou corpo recebido de terceiros.
            log.warn("Rodoviario integracao falhou requestId={} servico={} status={} duracaoMs={}",
                    requestId, servico, ex.getStatus(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            throw ex;
        }
    }
    private <T> T executar(Servico servico, String rota, HttpMethod metodo, Object corpo,
                           MultiValueMap<String, String> parametros, String usuario, Class<T> tipo, boolean efeito) {
        String base = servico == Servico.HUB ? UrlConfig.URL_CONFIANCA_HUB : UrlConfig.URL_CONFIANCA_MANAGER;
        if (base == null || base.isBlank()) throw falha(503, servico, "CONFIGURACAO_INCOMPLETA", false);
        var uri = UriComponentsBuilder.fromHttpUrl(base).pathSegment(rota.split("/"))
                .queryParams(parametros).build().encode().toUri();
        byte[] json;
        try {
            // RestTemplate imprime apenas bytes em DEBUG, sem dados de cartao ou passageiros.
            json = corpo == null ? null : mapper.writeValueAsBytes(corpo);
        } catch (JsonProcessingException ex) {
            throw falha(400, servico, "REQUEST_INVALIDO", false);
        }
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        String requestId = MDC.get(RodoviarioRequestLoggingFilter.REQUEST_ID);
        if (requestId != null && requestId.matches("[A-Za-z0-9_-]{1,64}"))
            headers.set(RodoviarioRequestLoggingFilter.HEADER, requestId);
        log.info("Rodoviario integracao autenticacao requestId={} servico={}",
                MDC.get(RodoviarioRequestLoggingFilter.REQUEST_ID), servico);
        headers.setBearerAuth(token(servico));
        if (servico == Servico.MANAGER && usuario != null && !usuario.isBlank()) headers.set("X-Usuario-Token", usuario);
        for (int tentativa = 1; ; tentativa++) {
            try {
                log.info("Rodoviario integracao envio requestId={} servico={}",
                        MDC.get(RodoviarioRequestLoggingFilter.REQUEST_ID), servico);
                var response = rest.exchange(uri, metodo, new HttpEntity<>(json, headers), tipo);
                if (!response.getStatusCode().is2xxSuccessful() || (tipo != Void.class && response.getBody() == null))
                    throw falha(502, servico, "RESPOSTA_INVALIDA", efeito);
                return response.getBody();
            } catch (HttpStatusCodeException ex) {
                throw erroHttp(servico, ex, efeito);
            } catch (ResourceAccessException ex) {
                // Apenas tipos e destino sem query, credenciais, corpo ou mensagem da excecao.
                Throwable causa = ex.getMostSpecificCause();
                log.warn("Rodoviario integracao transporte requestId={} servico={} host={} porta={} causa={} motivo={} tentativa={}",
                        MDC.get(RodoviarioRequestLoggingFilter.REQUEST_ID), servico,
                        uri.getHost(), uri.getPort(), causa.getClass().getSimpleName(), diagnosticoTransporte(causa), tentativa);
                if (tentativa == 1 && servico == Servico.MANAGER && metodo == HttpMethod.GET && !efeito
                        && causa instanceof SocketException && !(causa instanceof ConnectException)
                        && !(causa instanceof NoRouteToHostException)) {
                    log.warn("Rodoviario consulta Manager repetindo uma vez apos interrupcao de socket requestId={}",
                            MDC.get(RodoviarioRequestLoggingFilter.REQUEST_ID));
                    continue;
                }
                throw falha(504, servico, "FALHA_COMUNICACAO", efeito);
            } catch (RestClientException ex) {
                throw falha(502, servico, "RESPOSTA_INVALIDA", efeito);
            }
        }
    }
    private String diagnosticoTransporte(Throwable causa) {
        if (causa instanceof ConnectException) return "FALHA_CONEXAO";
        if (causa instanceof NoRouteToHostException) return "ROTA_INDISPONIVEL";
        // A mensagem e usada somente para classificacao; nunca e devolvida nem impressa.
        if (causa instanceof SocketException) {
            String mensagem = String.valueOf(causa.getMessage()).toLowerCase(Locale.ROOT);
            if (mensagem.contains("reset")) return "CONEXAO_REINICIADA";
            if (mensagem.contains("abort")) return "CONEXAO_ABORTADA";
            if (mensagem.contains("closed")) return "SOCKET_FECHADO";
            if (mensagem.contains("broken pipe")) return "CONEXAO_ENCERRADA";
            return "FALHA_SOCKET";
        }
        return "FALHA_TRANSPORTE";
    }
    private String token(Servico servico) {
        try {
            var token = auth.token();
            if (token != null && token.getToken() != null && !token.getToken().isBlank()) return token.getToken();
        } catch (RuntimeException ex) {
            // Erros da autenticacao podem conter credenciais; nao propagar nem encadear.
        }
        throw falha(503, servico, "AUTENTICACAO_INDISPONIVEL", false);
    }
    private RodoviarioIntegracaoException erroHttp(Servico servico, HttpStatusCodeException ex, boolean efeito) {
        int status = ex.getRawStatusCode();
        if (servico == Servico.HUB && (status == 401 || status == 403))
            return falha(502, servico, "AUTENTICACAO_HUB_REJEITADA", false);
        if (servico == Servico.MANAGER && status == 401)
            return RodoviarioIntegracaoException.falha(401, servico.name(), "AUTENTICACAO_MANAGER_REJEITADA",
                    "Autenticacao da aplicacao recusada pelo Manager. Confira o token e o ambiente da integracao.", false);
        try {
            JsonNode body = mapper.readTree(ex.getResponseBodyAsByteArray());
            if (body != null && body.isObject()) {
                if (servico == Servico.HUB && body.path("codigo").isTextual()
                        && body.path("mensagens").isArray() && body.path("requerConferencia").isBoolean())
                    return new RodoviarioIntegracaoException(status, body);
                if (servico == Servico.MANAGER && status < 500)
                    return new RodoviarioIntegracaoException(status, body);
            }
        } catch (java.io.IOException exParse) {
            // Nao devolver HTML nem corpos inesperados.
        }
        return falha(status, servico, "ERRO_" + servico, efeito && status >= 500);
    }
    private RodoviarioIntegracaoException falha(int status, Servico servico, String codigo, boolean conferir) {
        return RodoviarioIntegracaoException.falha(status, servico.name(), codigo,
                conferir ? "Resultado nao confirmado. Consulte a reserva ou compra antes de repetir a operacao."
                        : "Nao foi possivel concluir a solicitacao ao " + servico + ".", conferir);
    }
}
