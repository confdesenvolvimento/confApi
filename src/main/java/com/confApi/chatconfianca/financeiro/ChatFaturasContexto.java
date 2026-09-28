package com.confApi.chatconfianca.financeiro;

import com.confApi.chatconfianca.dto.enums.RemetenteTipo;
import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatgpt.dto.ChatResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;

/** Server-owned, short-lived filters only. Never reuse invoice records or model-supplied identity. */
public final class ChatFaturasContexto {
    private ChatFaturasContexto() { }
    public static JsonNode payload(ChatResponseDTO resposta,ObjectMapper mapper) {
        if(resposta==null||resposta.history()==null)return null;
        for(var msg:resposta.history())try {
            if(msg==null||!"system".equals(msg.role())||msg.content()==null)continue;
            JsonNode n=mapper.readTree(msg.content());
            if(n!=null&&"chat.faturas.v1".equals(n.path("schema").asText()))return n;
        }catch(Exception ignored) { /* Unrelated legacy messages are not invoice state. */ }
        return null;
    }
    public static Map<String,String> parametros(JsonNode n) {
        Map<String,String> params=new LinkedHashMap<>();
        if(n!=null&&n.isObject())for(String key:ChatFaturasFiltros.PARAMETROS) {
            JsonNode value=n.get(key);
            if(value!=null&&value.isTextual()&&value.asText().length()<=100)params.put(key,value.asText());
        }
        return params;
    }
    public static String anexar(String existente,ChatResponseDTO resposta,Long conversa,ObjectMapper mapper) {
        JsonNode data=payload(resposta,mapper);
        if(data==null||!data.path("contexto").isObject()||conversa==null)return existente;
        try {
            JsonNode old=existente==null||existente.isBlank()?null:mapper.readTree(existente);
            ObjectNode out=old!=null&&old.isObject()?(ObjectNode)old:mapper.createObjectNode();
            JsonNode source=data.path("contexto");
            ObjectNode context=mapper.createObjectNode();
            context.put("conversa",conversa);
            context.put("agencia",source.path("agencia").asLong(0));
            context.put("usuario",source.path("usuario").asLong(0));
            context.put("atualizadoEm",source.path("atualizadoEm").asLong(0));
            context.put("boletos",source.path("boletos").asBoolean(false));
            context.put("statusConsulta",data.path("statusConsulta").asText("ERRO_INTEGRACAO"));
            context.set("parametros",mapper.valueToTree(parametros(source.path("parametros"))));
            out.set("consultaFaturas",context);
            return mapper.writeValueAsString(out);
        }catch(Exception ignored) {return existente;}
    }
    public record Estado(Map<String,String> parametros,boolean boletos) { }
    public static boolean permiteContinuidadeV2(List<Mensagem> historico,Integer usuario,ObjectMapper mapper) {
        if(historico==null||historico.isEmpty())return true;
        for(int i=historico.size()-1;i>=0;i--) {
            Mensagem msg=historico.get(i);if(msg==null||msg.getExcluidaEm()!=null)continue;
            if(msg.getRemetenteTipo()==RemetenteTipo.USUARIO) {
                if(!Objects.equals(msg.getRemetenteCodgUsuario(),usuario))return false;
                continue;
            }
            if(msg.getRemetenteTipo()!=RemetenteTipo.BOT||msg.getConteudoJson()==null)return false;
            try {
                JsonNode json=mapper.readTree(msg.getConteudoJson());
                String intencao=json.path("confiaV2").path("intencao").asText();
                return json.path("consultaFaturas").isObject()
                        ||Set.of("financeiro.faturas","financeiro.boletos").contains(intencao);
            }catch(Exception ignored){return false;}
        }
        return true;
    }
    public static Estado recuperar(List<Mensagem> historico,Long conversa,Long agencia,Long usuario,ObjectMapper mapper) {
        if(historico==null||conversa==null||agencia==null||agencia<=0||usuario==null||usuario<=0)return null;
        // History is supplied by the authorized conversation service, ordered oldest to newest.
        for(int i=historico.size()-1;i>=0;i--) {
            Mensagem msg=historico.get(i);
            if(msg==null||msg.getExcluidaEm()!=null)continue;
            // A human answer ends automated financial continuity as well as a different bot topic.
            if(msg.getRemetenteTipo()==RemetenteTipo.USUARIO) {
                if(msg.getRemetenteCodgUsuario()==null||msg.getRemetenteCodgUsuario().longValue()!=usuario)return null;
                continue;
            }
            if(msg.getRemetenteTipo()!=RemetenteTipo.BOT)return null;
            if(!Objects.equals(conversa,msg.getConversaId()))return null;
            try {
                if(msg.getConteudoJson()==null)return null;
                JsonNode n=mapper.readTree(msg.getConteudoJson()).path("consultaFaturas");
                long age=System.currentTimeMillis()-n.path("atualizadoEm").asLong(0);
                if(!n.isObject()||n.path("conversa").asLong(0)!=conversa
                        ||n.path("agencia").asLong(0)!=agencia||n.path("usuario").asLong(0)!=usuario
                        ||age<0||age>30*60_000L)return null;
                return new Estado(parametros(n.path("parametros")),n.path("boletos").asBoolean(false));
            }catch(Exception ignored) {return null;}
        }
        return null;
    }
}
