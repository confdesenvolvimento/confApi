package com.confApi.chatconfianca.v2;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

/** Renders only public knowledge fields. Encoded/invalid JSON is never displayed as prose. */
final class ChatV2ConhecimentoTexto {
    private final ObjectMapper mapper;
    ChatV2ConhecimentoTexto(ObjectMapper mapper) { this.mapper=mapper; }

    JsonNode decode(String text) {
        if(text==null || text.length()>100_000)return null;
        String current=text.trim();
        for(int layer=0;layer<6;layer++) {
            try {
                JsonNode node=mapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(current);
                if(node==null)return null;
                if(!node.isTextual())return node;
                String next=node.textValue().trim();
                if(!pareceEstruturado(next))return node;
                current=next;
            } catch(Exception invalid) {
                // Some legacy rows contain escaped object text, without a valid JSON string wrapper.
                // Decode one string-escaping layer with Jackson; do not repair missing braces or replace quotes globally.
                if(!pareceEstruturado(current) || !current.contains("\\\""))return null;
                String escaped=current;
                if(escaped.startsWith("\"") && escaped.endsWith("\"") && escaped.length()>1)
                    escaped=escaped.substring(1,escaped.length()-1);
                try {
                    String next=mapper.readerFor(String.class).with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                            .readValue("\""+escaped+"\"");
                    if(next.equals(current))return null;
                    current=next.trim();
                } catch(Exception malformed) { return null; }
            }
        }
        return null;
    }

    static boolean pareceEstruturado(String text) {
        if(text==null)return false;
        String t=text.trim();
        int i=0;
        while(i<t.length() && (t.charAt(i)=='"' || t.charAt(i)=='\\' || Character.isWhitespace(t.charAt(i))))i++;
        return i<t.length() && (t.charAt(i)=='{' || t.charAt(i)=='[' || t.startsWith("\u0060\u0060\u0060",i));
    }

    String render(String raw, ChatV2Capability capability, String setor) {
        JsonNode doc=decode(raw);
        if(doc==null || doc.isTextual()) {
            String plain=doc==null?raw:doc.textValue();
            if(pareceEstruturado(plain))return "";
            // Generic free text cannot prove which department a contact belongs to.
            if(capability==ChatV2Capability.CONTATOS && setor!=null && !setor.isBlank())return "";
            return plain==null?"":plain.trim();
        }
        if(!doc.isObject())return "";
        JsonNode selected=doc;
        if(capability==ChatV2Capability.EMERGENCIA)selected=conteudo(doc.path("atendimentoEmergencial"));
        else if(capability==ChatV2Capability.HORARIO)selected=conteudo(doc.path("nucleoAtendimento"));
        else if(capability==ChatV2Capability.CONTATOS)selected=contato(doc,setor);
        return camposPublicos(selected);
    }

    private JsonNode conteudo(JsonNode node) { return node!=null&&node.isTextual()?decode(node.textValue()):node; }

    private JsonNode contato(JsonNode doc,String setor) {
        if(setor==null || setor.isBlank())return conteudo(doc.path("nucleoAtendimento"));
        String requested=ChatV2Planner.normalizar(setor);
        JsonNode setores=conteudo(doc.path("setores"));
        if(setores!=null && setores.isArray()) {
            for(JsonNode candidate:setores) {
                JsonNode item=conteudo(candidate);
                if(item!=null && mesmoSetor(item.path("setor").asText(),requested))return item;
            }
        } else if(setores!=null && setores.isObject()) {
            var fields=setores.fields();
            while(fields.hasNext()) {
                var field=fields.next();
                if(mesmoSetor(field.getKey(),requested))return conteudo(field.getValue());
            }
        }
        if(mesmoSetor(doc.path("setor").asText(),requested))return doc;
        return null;
    }

    private boolean mesmoSetor(String name,String requested) {
        String normalized=ChatV2Planner.normalizar(name);
        if(normalized.equals(requested))return true;
        if(requested.equals("ti") && normalized.equals("t i"))return true;
        return !requested.isBlank() && (" "+normalized+" ").contains(" "+requested+" ");
    }

    private String camposPublicos(JsonNode node) {
        if(node==null || !node.isObject())return "";
        StringBuilder out=new StringBuilder();
        append(out,node,"descricao","");
        append(out,node,"setor","Setor");
        append(out,node,"telefone","Telefone");
        append(out,node,"email","E-mail");
        append(out,node,"whatsapp","WhatsApp");
        append(out,node,"informacoes","");
        append(out,node,"observacao","Observação");
        append(out,node,"segunda_sexta","Segunda a sexta");
        append(out,node,"sabado","Sábado");
        append(out,node,"domingos_feriados","Domingos e feriados");
        append(out,node,"domingo_feriados","Domingos e feriados");
        append(out,node,"fora_horario","Fora do horário");
        for(String key:List.of("horario","horarios")) {
            JsonNode hours=conteudo(node.path(key));
            if(hours!=null && hours.isObject()) {
                String rendered=camposPublicos(hours);
                if(!rendered.isBlank())out.append(rendered).append('\n');
            }
        }
        append(out,node,"aviso","Aviso");
        return out.toString().trim();
    }

    private void append(StringBuilder out,JsonNode node,String field,String label) {
        JsonNode value=node.path(field);
        if(!value.isTextual() || value.asText().isBlank() || pareceEstruturado(value.asText()))return;
        if(!label.isEmpty())out.append(label).append(": ");
        out.append(value.asText().trim()).append('\n');
    }
}
