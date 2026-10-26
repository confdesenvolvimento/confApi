package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.intencao.ChatConfiancaDecisaoIa;
import com.confApi.chatconfianca.v2.ChatV2Plan;
import com.confApi.chatgpt.dto.ChatResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;
import java.util.UUID;

/** Metadados tecnicos sem texto, documentos, payload de fornecedor ou identidade pessoal. */
public final class ChatResultadoTurno {
    private static final String INSTANCIA = UUID.randomUUID().toString();
    private ChatResultadoTurno() { }

    public static String anexar(String original, ObjectMapper mapper, Long conversaId, Long perguntaId,
            ChatConfiancaDecisaoIa decisao, ChatV2Plan plano, ChatResponseDTO resposta,
            String resultadoExplicito, String build, String ambiente, boolean teste) {
        try {
            JsonNode node = original == null || original.isBlank() ? null : mapper.readTree(original);
            ObjectNode raiz = node instanceof ObjectNode ? (ObjectNode) node : mapper.createObjectNode();
            ObjectNode observacao = raiz.putObject("observabilidade");
            observacao.put("schema", "chat.resultado-turno.v1");
            if (conversaId != null) observacao.put("conversaId", conversaId);
            if (perguntaId != null) observacao.put("mensagemPerguntaId", perguntaId);
            observacao.put("resultadoTarefa", resultadoExplicito == null ? resultado(plano, resposta) : resultadoExplicito);
            observacao.put("statusTecnico", decisao == null ? "NAO_INFORMADO" : decisao.getStatusResultado());
            observacao.put("intencaoEfetiva", decisao == null ? null : decisao.getIntencao());
            observacao.put("build", identificador(build));
            observacao.put("ambiente", identificador(ambiente));
            observacao.put("instancia", INSTANCIA);
            observacao.put("teste", teste);
            return mapper.writeValueAsString(raiz);
        } catch (Exception ex) { throw new IllegalStateException("Nao foi possivel preparar metadados da resposta.", ex); }
    }

    static String resultado(ChatV2Plan plano, ChatResponseDTO resposta) {
        if (plano != null && plano.getResultado() != null
                && !Set.of("FALLBACK_LEGADO", "AGUARDANDO_EXECUCAO").contains(plano.getResultado())) return plano.getResultado();
        if (resposta != null && resposta.history() != null) for (var m : resposta.history()) {
            if (m == null || m.content() == null || m.content().length() > 1000000) continue;
            try {
                int inicio = m.content().indexOf('{');
                if (inicio < 0) continue;
                JsonNode j = new ObjectMapper().readTree(m.content().substring(inicio));
                String granular = j.path("resultadoTarefa").asText();
                if (!granular.isBlank()) return granular;
                String status = j.path("statusConsulta").asText();
                if (!status.isBlank()) return "OK".equals(status) ? "DADOS_CONSULTADOS" : status;
            } catch (Exception ignored) { }
        }
        if (resposta == null || resposta.content() == null || resposta.content().isBlank()) return "SEM_RESULTADO";
        if (resposta.actions() != null && !resposta.actions().isEmpty()) return "ACAO_PREPARADA";
        return "RESPOSTA_FORNECIDA"; // Nao e confirmacao de resolucao pelo solicitante.
    }

    private static String identificador(String value) {
        if (value == null || value.isBlank()) return "nao_informado";
        return value.matches("[a-zA-Z0-9._-]{1,100}") ? value : "nao_informado";
    }
}
