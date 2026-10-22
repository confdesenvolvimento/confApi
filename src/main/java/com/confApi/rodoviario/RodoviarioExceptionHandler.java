package com.confApi.rodoviario;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {RodoviarioController.class, RodoviarioHubController.class})
public class RodoviarioExceptionHandler {
    @ExceptionHandler(RodoviarioIntegracaoException.class)
    public ResponseEntity<JsonNode> integracao(RodoviarioIntegracaoException ex) {
        RodoviarioErroLog.registrar("controller", ex);
        return ResponseEntity.status(ex.getStatus()).body(ex.getResposta());
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<JsonNode> invalido(Exception ex) {
        return integracao(RodoviarioIntegracaoException.falha(400, "CONFAPI", "REQUEST_INVALIDO",
                "JSON ou parametro invalido. Confira os campos e formatos informados.", false));
    }
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<JsonNode> cabecalho(MissingRequestHeaderException ex) {
        return integracao(RodoviarioIntegracaoException.falha(401, "CONFAPI", "TOKEN_INDIVIDUAL_OBRIGATORIO",
                "Informe X-Usuario-Token com o token individual do Manager.", false));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<JsonNode> status(ResponseStatusException ex) {
        // O motivo original pode conter valores informados pelo cliente.
        RodoviarioErroLog.registrar("controller", ex);
        var resposta = RodoviarioIntegracaoException.falha(ex.getRawStatusCode(), "CONFAPI", "REQUEST_INVALIDO",
                ex.getReason() == null ? "Solicitacao invalida." : ex.getReason(), false);
        return ResponseEntity.status(resposta.getStatus()).body(resposta.getResposta());
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<JsonNode> inesperado(Exception ex) {
        RodoviarioErroLog.registrar("controller", ex);
        var resposta = RodoviarioIntegracaoException.falha(500, "CONFAPI", "ERRO_INTERNO",
                "Falha interna no fluxo rodoviario. Consulte os logs pelo identificador da requisicao.", false);
        return ResponseEntity.status(500).body(resposta.getResposta());
    }
}
