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

    public static boolean perguntaOrigem(String texto) {
        String t = ChatPedidoOperacional.normalizar(texto);
        return t.matches(".*\\b(?:consultou|conseguiu consultar|consulta real|consultar o sistema|consultou o sistema|orientacao geral|orientacao com base)\\b.*")
                && !t.matches(".*\\b(?:reserva|fatura) [a-z0-9]+.*");
    }

    /** Somente metadados de respostas persistidas da conversa autorizada; nunca texto do modelo. */
    public static ChatResponseDTO explicarOrigem(String texto, Long conversaId,
            java.util.List<com.confApi.chatconfianca.dto.model.Mensagem> historico, ObjectMapper mapper) {
        if (!perguntaOrigem(texto)) return null;
        boolean encontrouConsulta = false;
        String ultimoResultado = null;
        if (historico != null && conversaId != null) for (int i=historico.size()-1;i>=0;i--) {
            var m=historico.get(i);
            if (m==null || m.getExcluidaEm()!=null) continue;
            if (!java.util.Objects.equals(conversaId,m.getConversaId())) break;
            if (m.getRemetenteTipo()==com.confApi.chatconfianca.dto.enums.RemetenteTipo.USUARIO) continue;
            if (m.getRemetenteTipo()!=com.confApi.chatconfianca.dto.enums.RemetenteTipo.BOT) break;
            try {
                String payload=m.getConteudoJson(); if(payload==null||payload.length()>1_000_000) continue;
                JsonNode obs=mapper.readTree(payload).path("observabilidade");
                if (!"chat.resultado-turno.v1".equals(obs.path("schema").asText())
                        ||obs.path("conversaId").asLong()!=conversaId) continue;
                String resultado=obs.path("resultadoTarefa").asText();
                if(ultimoResultado==null)ultimoResultado=resultado;
                if(Set.of("DADOS_CONSULTADOS","DADOS_PARCIAIS","VALOR_NAO_DISPONIVEL").contains(resultado)) {
                    encontrouConsulta=true;break;
                }
            } catch(Exception ignored) { /* Sem evidencia estruturada, nao afirmar consulta. */ }
        }
        String resposta;
        if (encontrouConsulta && Set.of("DADOS_CONSULTADOS","DADOS_PARCIAIS","VALOR_NAO_DISPONIVEL").contains(ultimoResultado))
            resposta="A resposta anterior utilizou dados consultados na origem. Essa consulta não equivale a emitir, cancelar, remarcar ou pagar; documentos só são solicitados após o clique no botão correspondente.";
        else if(encontrouConsulta)
            resposta="A última resposta foi uma orientação, sem uma nova consulta comprovada nesse turno. Há consultas de dados registradas anteriormente nesta conversa; isso não confirma que os dados ainda estejam atualizados. Posso consultá-los novamente quando você indicar o registro.";
        else resposta="Não há evidência estruturada suficiente para confirmar uma consulta à origem nesta conversa. Não vou afirmar que consultei dados sem essa confirmação. Uma orientação geral não confirma valores, regras ou geração de documentos.";
        return new ChatResponseDTO(null,resposta,java.util.List.of(),null,java.util.List.of(),
                java.util.List.of(new com.confApi.chatgpt.dto.ChatMessageDTO("system",
                        "{\"schema\":\"chat.origem-consulta.v1\",\"resultadoTarefa\":\"ORIENTACAO_FORNECIDA\"}")),java.util.List.of());
    }

    private static String identificador(String value) {
        if (value == null || value.isBlank()) return "nao_informado";
        return value.matches("[a-zA-Z0-9._-]{1,100}") ? value : "nao_informado";
    }
}
