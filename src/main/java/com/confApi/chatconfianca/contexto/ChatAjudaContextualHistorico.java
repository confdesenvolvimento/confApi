package com.confApi.chatconfianca.contexto;

import com.confApi.chatconfianca.dto.enums.RemetenteTipo;
import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatgpt.dto.ChatResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Objects;

/** Referencia minima de tarefa: nenhum documento, URL, credencial ou dado da tela e fonte de verdade. */
public final class ChatAjudaContextualHistorico {
    public static final String SCHEMA="chat.ajuda-contextual.v1";
    private static final long TTL=30*60_000L;
    private ChatAjudaContextualHistorico() { }

    public static JsonNode payload(ChatResponseDTO resposta,ObjectMapper mapper) {
        if(resposta==null||resposta.history()==null)return null;
        for(var h:resposta.history())if(h!=null&&"system".equals(h.role())&&h.content()!=null&&h.content().length()<131072)try {
            JsonNode j=mapper.readTree(h.content());
            if(SCHEMA.equals(j.path("schema").asText()))return j;
        }catch(Exception ignored){ }
        return null;
    }

    public static String anexar(String original,ChatResponseDTO resposta,ObjectMapper mapper) {
        JsonNode payload=payload(resposta,mapper);
        if(payload==null)return original;
        try {
            JsonNode parsed=original==null||original.isBlank()?null:mapper.readTree(original);
            ObjectNode root=parsed instanceof ObjectNode?(ObjectNode)parsed:mapper.createObjectNode();
            root.set("ajudaContextual",payload);return mapper.writeValueAsString(root);
        }catch(Exception ex){throw new IllegalStateException("Falha ao registrar referencia da ajuda contextual.",ex);}
    }

    public static ChatRecursoReferenciaDTO recuperar(List<Mensagem> historico,Long conversa,Integer agencia,Integer usuario,ObjectMapper mapper) {
        if(historico==null||conversa==null||agencia==null||usuario==null)return null;
        for(int i=historico.size()-1,limite=Math.max(0,historico.size()-80);i>=limite;i--) {
            Mensagem m=historico.get(i);if(m==null)continue;
            if(m.getConversaId()!=null&&!Objects.equals(conversa,m.getConversaId()))return null;
            if(m.getRemetenteTipo()==RemetenteTipo.USUARIO) {
                if(!Objects.equals(usuario,m.getRemetenteCodgUsuario()))return null;
                if(neutra(m.getConteudo()))continue;
                return null; // Nao ressuscitar contexto antes de um turno sem resposta.
            }
            if(m.getRemetenteTipo()!=RemetenteTipo.BOT)return null;
            try {
                String json=m.getConteudoJson();
                if(json==null||json.length()>131072)return null;
                JsonNode n=mapper.readTree(json).path("ajudaContextual");
                if(n.isMissingNode()){if(neutra(m.getConteudo()))continue;return null;}
                long idade=System.currentTimeMillis()-n.path("atualizadoEm").asLong(0);
                if(!SCHEMA.equals(n.path("schema").asText())||!"DADOS_CONSULTADOS".equals(n.path("resultadoTarefa").asText())
                        ||n.path("conversa").asLong()!=conversa||n.path("agencia").asInt()!=agencia
                        ||n.path("usuario").asInt()!=usuario||idade<0||idade>TTL||!n.path("recurso").isObject())return null;
                ChatRecursoReferenciaDTO ref=mapper.treeToValue(n.path("recurso"),ChatRecursoReferenciaDTO.class);
                ChatContextoClienteDTO ctx=new ChatContextoClienteDTO();ctx.setCanal("WEB");
                ctx.setTela("FATURA".equals(ref.getTipo())?"FATURA_DETALHE":"RESERVA_AEREA");ctx.setRecurso(ref);
                return ChatContextoClienteContrato.validar(ctx).getRecurso();
            }catch(Exception ex){return null;}
        }
        return null;
    }

    private static boolean neutra(String texto) {
        String t=com.confApi.chatconfianca.reservas.ChatReservaConsultaParser.normalizar(texto);
        return t.matches("(?:obrigad[oa]|muito obrigad[oa]|de nada|disponha|ok|certo|por nada)[.! ]*");
    }
}
