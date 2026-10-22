package com.confApi.rodoviario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class RodoviarioIntegracaoException extends RuntimeException {
    private final int status;
    private final JsonNode resposta;
    private String diagnosticoLocal;
    public RodoviarioIntegracaoException(int status, JsonNode resposta) {
        super(resposta.path("codigo").asText("ERRO_INTEGRACAO"));
        this.status = status;
        this.resposta = resposta;
    }
    String getDiagnosticoLocal() { return diagnosticoLocal; }
    public int getStatus() { return status; }
    public JsonNode getResposta() { return resposta; }
    public static RodoviarioIntegracaoException falha(int status, String servico, String codigo,
                                                     String mensagem, boolean conferir) {
        var body = JsonNodeFactory.instance.objectNode();
        body.put("servico", servico).put("codigo", codigo).put("requerConferencia", conferir);
        body.putArray("mensagens").add(mensagem);
        var exception = new RodoviarioIntegracaoException(status, body);
        // Apenas mensagens criadas pelo ConfApi; respostas de terceiros nao preenchem este campo.
        exception.diagnosticoLocal = mensagem;
        return exception;
    }
}