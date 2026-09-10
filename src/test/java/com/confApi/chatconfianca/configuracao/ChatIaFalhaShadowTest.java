package com.confApi.chatconfianca.configuracao;

import java.net.*;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.client.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.confApi.chatconfianca.configuracao.ChatIaFalhaShadow.*;

class ChatIaFalhaShadowTest {
    @Test void distingueTimeoutConexaoETransporteSemMensagens() {
        assertEquals(Codigo.TIMEOUT,descrever(new ResourceAccessException("secreto",new SocketTimeoutException("token"))).codigo());
        assertEquals(Codigo.CONEXAO,descrever(new ResourceAccessException("secreto",new ConnectException("token"))).codigo());
        assertEquals(Codigo.CONEXAO,descrever(new UnknownHostException("servidor-interno")).codigo());
        assertEquals(Codigo.TRANSPORTE,descrever(new ResourceAccessException("secreto")).codigo());
    }
    @Test void registraSomenteStatusHttp() {
        var ex=HttpClientErrorException.create(HttpStatus.UNAUTHORIZED,"senha",null,"token-corpo".getBytes(),null);
        assertEquals(new Detalhe(Codigo.HTTP_4XX,401),descrever(ex));
        var server=HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR,"senha",null,"token-corpo".getBytes(),null);
        assertEquals(new Detalhe(Codigo.HTTP_5XX,500),descrever(server));
        assertFalse(descrever(ex).toString().contains("token"));
    }
    @Test void transporteInterrompidoDistingueFimPrematuroDeSocket() {
        var eof=new ResourceAccessException("TOKEN_PRIVADO",
                new java.io.IOException("host-restrito",new java.io.EOFException("corpo privado")));
        assertEquals(new Detalhe(Codigo.RESPOSTA_INTERROMPIDA,null),descrever(eof));
        assertEquals(new Detalhe(Codigo.CONEXAO_INTERROMPIDA,null),descrever(
                new ResourceAccessException("TOKEN_PRIVADO",new SocketException("connection reset secreto"))));
        assertFalse(descrever(eof).toString().contains("PRIVADO"));
    }
    @Test void distingueJsonFilaEFalhaInterna() {
        assertEquals(Codigo.JSON_INVALIDO,descrever(new RestClientException("secreto",
                new HttpMessageNotReadableException("mensagem privada"))).codigo());
        assertEquals(Codigo.FILA_CHEIA,descrever(new RejectedExecutionException("token")).codigo());
        assertEquals(new Detalhe(Codigo.FALHA_INTERNA,null),descrever(new IllegalArgumentException("mensagem privada")));
        assertEquals(Codigo.RESPOSTA_VAZIA,descrever(new Falha(Codigo.RESPOSTA_VAZIA)).codigo());
    }
}
