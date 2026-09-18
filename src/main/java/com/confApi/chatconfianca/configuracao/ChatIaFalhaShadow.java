package com.confApi.chatconfianca.configuracao;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/** Only fixed codes and a numeric HTTP status may leave the exception boundary. */
final class ChatIaFalhaShadow {
    enum Etapa { CAPTURA_CONTEXTO, FILA, VALIDACAO_URL, AUTENTICACAO, CONSULTA_CONFIGURACAO, RESOLUCAO, SERIALIZACAO }
    enum Codigo { URL_AUSENTE, TOKEN_AUSENTE, RESPOSTA_VAZIA, TIMEOUT, CONEXAO, TRANSPORTE,
        RESPOSTA_INTERROMPIDA, CONEXAO_INTERROMPIDA,
        HTTP_4XX, HTTP_5XX, HTTP_INESPERADO, JSON_INVALIDO, FILA_CHEIA, FALHA_INTERNA }
    record Detalhe(Codigo codigo, Integer httpStatus) {}
    static final class Falha extends RuntimeException {
        final Codigo codigo;
        Falha(Codigo codigo) { this.codigo = codigo; }
    }
    static Detalhe descrever(Throwable erro) {
        boolean transporte = false;
        Throwable causa = erro;
        for (int i = 0; causa != null && i < 8; i++, causa = causa.getCause()) {
            if (causa instanceof Falha f) return new Detalhe(f.codigo, null);
            if (causa instanceof RestClientResponseException http) {
                int status = http.getRawStatusCode();
                return new Detalhe(status >= 500 && status <= 599 ? Codigo.HTTP_5XX
                        : status >= 400 && status <= 499 ? Codigo.HTTP_4XX : Codigo.HTTP_INESPERADO, status);
            }
            if (causa instanceof InterruptedIOException) return new Detalhe(Codigo.TIMEOUT, null);
            if (causa instanceof ConnectException || causa instanceof UnknownHostException)
                return new Detalhe(Codigo.CONEXAO, null);
            if (causa instanceof java.io.EOFException) return new Detalhe(Codigo.RESPOSTA_INTERROMPIDA, null);
            if (causa instanceof java.net.SocketException) return new Detalhe(Codigo.CONEXAO_INTERROMPIDA, null);
            if (causa instanceof JsonProcessingException || causa instanceof HttpMessageConversionException)
                return new Detalhe(Codigo.JSON_INVALIDO, null);
            if (causa instanceof java.util.concurrent.RejectedExecutionException)
                return new Detalhe(Codigo.FILA_CHEIA, null);
            transporte |= causa instanceof ResourceAccessException;
        }
        return new Detalhe(transporte ? Codigo.TRANSPORTE : Codigo.FALHA_INTERNA, null);
    }
    private ChatIaFalhaShadow() {}
}
