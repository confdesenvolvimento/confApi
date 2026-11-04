package com.confApi.rodoviario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.server.ResponseStatusException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Diagnostico sem imprimir tokens, corpos ou mensagens arbitrarias de terceiros. */
final class RodoviarioErroLog {
    private static final Logger log = LoggerFactory.getLogger(RodoviarioErroLog.class);
    private static final Set<String> CODIGOS = Set.of("REQUEST_INVALIDO", "CONFIGURACAO_INCOMPLETA",
            "AUTENTICACAO_INDISPONIVEL", "AUTENTICACAO_HUB_REJEITADA", "AUTENTICACAO_MANAGER_REJEITADA", "RESPOSTA_INVALIDA",
            "RESPOSTA_INVALIDA_FORNECEDOR", "CONFERENCIA_NECESSARIA", "VINCULOS_DESTINO_INVALIDOS", "FALHA_COMUNICACAO",
            "ERRO_HUB", "ERRO_MANAGER", "ERRO_FORNECEDOR", "TOKEN_INDIVIDUAL_OBRIGATORIO", "TOKEN_INDIVIDUAL_INVALIDO",
            "RESERVA_EXPIRADA", "ERRO_INTEGRACAO", "ERRO_INTERNO");
    private RodoviarioErroLog() { }

    static void registrar(String etapa, Exception exception) {
        int status = 500;
        String codigo = "ERRO_INTERNO", servico = "CONFAPI", motivo = "Consulte a origem da falha indicada neste log.";
        boolean conferencia = false;
        if (exception instanceof RodoviarioIntegracaoException) {
            var error = (RodoviarioIntegracaoException) exception;
            status = error.getStatus();
            String recebido = error.getResposta().path("codigo").asText();
            codigo = CODIGOS.contains(recebido) ? recebido : "ERRO_INTEGRACAO";
            String origem = error.getResposta().path("servico").asText();
            servico = Set.of("CONFAPI", "HUB", "MANAGER").contains(origem) ? origem : "INTEGRACAO";
            conferencia = error.getResposta().path("requerConferencia").asBoolean();
            if (error.getDiagnosticoLocal() != null) motivo = error.getDiagnosticoLocal();
            else motivo = "Falha devolvida pela integracao; consulte codigo e logs do Hub/Manager.";
        } else if (exception instanceof ResponseStatusException) {
            status = ((ResponseStatusException) exception).getRawStatusCode(); codigo = "REQUEST_INVALIDO";
        }
        String origem = Arrays.stream(exception.getStackTrace())
                .filter(frame -> frame.getClassName().startsWith("com.confApi."))
                .filter(frame -> !frame.getClassName().equals(RodoviarioIntegracaoException.class.getName()))
                .limit(6).map(StackTraceElement::toString).collect(Collectors.joining(" <- "));
        String formato = "Rodoviario erro requestId={} etapa={} servico={} status={} codigo={} tipo={} requerConferencia={} motivo={} origem={}";
        Object[] detalhes = {MDC.get(RodoviarioRequestLoggingFilter.REQUEST_ID), etapa, servico, status,
                codigo, exception.getClass().getSimpleName(), conferencia,
                motivo.replaceAll("[\\p{Cntrl}]", " "), origem};
        if (status >= 500) log.error(formato, detalhes); else log.warn(formato, detalhes);
    }
}
