package com.confApi.chatconfianca.operacional;

import com.confApi.chatconfianca.dto.enums.RemetenteTipo;
import com.confApi.chatconfianca.dto.enums.StatusMensagem;
import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatgpt.dto.ChatResponseDTO;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.*;

/** Guarda apenas dono, criterios e estado das fontes. Nunca reutiliza itens ou totais antigos. */
public final class ChatResumoOperacionalContexto {
    public static final String SCHEMA = "chat.resumo-operacional.v1";
    private ChatResumoOperacionalContexto() { }

    public static boolean recente(List<Mensagem> historico, Long conversa, Integer usuario, Integer agencia, ObjectMapper mapper) {
        if (historico == null || conversa == null || usuario == null || agencia == null) return false;
        for (int i = historico.size()-1; i >= 0; i--) {
            Mensagem m = historico.get(i);
            if (m == null || m.getExcluidaEm() != null || m.getStatus() == StatusMensagem.EXCLUIDA) continue;
            if (!Objects.equals(conversa, m.getConversaId())) return false;
            if (m.getRemetenteTipo() == RemetenteTipo.USUARIO) {
                if (!Objects.equals(usuario, m.getRemetenteCodgUsuario())) return false;
                continue;
            }
            // Nao atravessa outras respostas, mensagens humanas, transferencias ou encerramentos.
            if (m.getRemetenteTipo() != RemetenteTipo.BOT) return false;
            try {
                JsonNode p = mapper.readTree(m.getConteudoJson()).path("resumoOperacional");
                JsonNode c = p.path("contexto");
                long instante = c.path("atualizadoEm").asLong(-1), agora = System.currentTimeMillis();
                return SCHEMA.equals(p.path("schema").asText())
                        && Set.of("DADOS_PARCIAIS", "DADOS_CONSULTADOS", "AGUARDANDO_DADOS").contains(p.path("statusConsulta").asText())
                        && c.path("conversa").isIntegralNumber() && conversa == c.path("conversa").asLong()
                        && c.path("usuario").isIntegralNumber() && usuario == c.path("usuario").asInt()
                        && c.path("agencia").isIntegralNumber() && agencia == c.path("agencia").asInt()
                        && instante > 0 && instante <= agora && agora-instante <= Duration.ofMinutes(30).toMillis();
            } catch (Exception ex) { return false; }
        }
        return false;
    }

    public static JsonNode payload(ChatResponseDTO resposta, ObjectMapper mapper) {
        if (resposta == null || resposta.history() == null) return null;
        for (var m : resposta.history()) if (m != null && "system".equals(m.role())) try {
            JsonNode n = mapper.readTree(m.content());
            if (SCHEMA.equals(n.path("schema").asText())) return n;
        } catch (Exception ignored) { }
        return null;
    }

    public static ChatResumoOperacionalParser.Pedido recuperar(List<Mensagem> historico, Long conversa,
            Integer usuario, Integer agencia, ObjectMapper mapper) {
        if (!recente(historico, conversa, usuario, agencia, mapper)) return null;
        for (int i=historico.size()-1; i>=0; i--) {
            Mensagem m=historico.get(i);
            if (m==null || m.getExcluidaEm()!=null || m.getStatus()==StatusMensagem.EXCLUIDA
                    || m.getRemetenteTipo()!=RemetenteTipo.BOT) continue;
            try {
                JsonNode c=mapper.readTree(m.getConteudoJson()).path("resumoOperacional").path("contexto");
                String foco=c.path("foco").asText();
                if (!Set.of("TODOS","EMBARQUES","PRAZOS","HOTEL","ENTRADAS","SAIDAS","FINANCEIRO").contains(foco)) return null;
                java.time.LocalDate inicio=java.time.LocalDate.parse(c.path("inicio").asText());
                java.time.LocalDate fim=java.time.LocalDate.parse(c.path("fim").asText());
                if (inicio.getYear()<1900 || fim.getYear()>2100 || fim.isBefore(inicio)
                        || java.time.temporal.ChronoUnit.DAYS.between(inicio,fim)>6) return null;
                return new ChatResumoOperacionalParser.Pedido(inicio,fim,foco,null);
            } catch (Exception ex) { return null; }
        }
        return null;
    }

    public static String anexar(String existente, ChatResponseDTO resposta, ObjectMapper mapper) {
        JsonNode p = payload(resposta, mapper);
        if (p == null) return existente;
        try {
            JsonNode raiz = existente == null ? null : mapper.readTree(existente);
            ObjectNode n = raiz instanceof ObjectNode o ? o : mapper.createObjectNode();
            n.set("resumoOperacional", p.deepCopy());
            return mapper.writeValueAsString(n);
        } catch (Exception ex) { throw new IllegalStateException("Nao foi possivel persistir o estado do resumo.", ex); }
    }
}
