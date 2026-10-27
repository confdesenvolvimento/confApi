package com.confApi.chatconfianca.reservas;

import com.confApi.chatconfianca.dto.enums.RemetenteTipo;
import com.confApi.chatconfianca.dto.enums.StatusMensagem;
import com.confApi.chatconfianca.dto.enums.TipoMensagem;
import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatconfianca.dto.reserva.ReservaAereaRecenteItem;
import com.confApi.chatgpt.dto.ChatResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Recupera somente metadados estruturados do historico persistido pelo servidor. */
public final class ChatReservaConsultaContexto {
    public static final String SCHEMA = "chat.reservas-consulta.v1";
    private static final long TTL = Duration.ofMinutes(30).toMillis();
    private static final Set<String> FALHAS = Set.of("ERRO", "ERROR", "ERRO_INTEGRACAO", "CONSULTA_BLOQUEADA");
    private ChatReservaConsultaContexto() { }

    public static ReservaConsultaEstado recuperar(List<Mensagem> historico, Long conversa, Long agencia,
                                                  Long usuario, ObjectMapper mapper) {
        if (historico == null || mapper == null || !positivo(conversa) || !positivo(agencia) || !positivo(usuario)) return null;
        long agora = System.currentTimeMillis();
        for (int i = historico.size() - 1; i >= 0; i--) {
            Mensagem mensagem = historico.get(i);
            if (mensagem == null || mensagem.getExcluidaEm() != null || mensagem.getStatus() == StatusMensagem.EXCLUIDA) continue;
            if (!Objects.equals(conversa, mensagem.getConversaId())) return null;
            if (mensagem.getRemetenteTipo() == RemetenteTipo.USUARIO) {
                if (mensagem.getRemetenteCodgUsuario() == null
                        || !Objects.equals(usuario, mensagem.getRemetenteCodgUsuario().longValue())) return null;
                if (ChatReservaConsultaParser.mudancaDeAssunto(mensagem.getConteudo())) return null;
                continue;
            }
            if (mensagem.getRemetenteTipo() != RemetenteTipo.BOT) {
                if (mensagem.getRemetenteTipo() != RemetenteTipo.SISTEMA
                        || mensagem.getTipo() == TipoMensagem.TRANSFERENCIA || mensagem.getTipo() == TipoMensagem.ENCERRAMENTO) return null;
                if (interrompeAtendimento(mensagem.getConteudo())) return null;
                continue;
            }
            JsonNode raiz = ler(mensagem.getConteudoJson(), mapper);
            if (outraTarefa(raiz)) return null;
            JsonNode consulta = raiz == null ? null : raiz.get("consultaReservas");
            if (consulta == null || consulta.isNull()) {
                if (interrompeAtendimento(mensagem.getConteudo())) return null;
                continue; // Um agradecimento ou outra resposta neutra nao apaga a lista anterior.
            }
            if (!SCHEMA.equals(consulta.path("schema").asText()) || FALHAS.contains(consulta.path("statusConsulta").asText())) return null;
            JsonNode contexto = consulta.path("contexto");
            if (!Objects.equals(conversa, inteiroLongo(contexto.get("conversa")))
                    || !Objects.equals(agencia, inteiroLongo(contexto.get("agencia")))
                    || !Objects.equals(usuario, inteiroLongo(contexto.get("usuario")))) return null;
            Long instante = inteiroLongo(contexto.get("atualizadoEm"));
            if (instante == null || instante > agora || agora - instante > TTL) return null;
            if (mensagem.getEnviadaEm() == null || mensagem.getEnviadaEm().isAfter(LocalDateTime.now().plusSeconds(5))
                    || mensagem.getEnviadaEm().isBefore(LocalDateTime.now().minusMinutes(30))) return null;
            try {
                ReservaConsultaEstado estado = mapper.treeToValue(contexto, ReservaConsultaEstado.class);
                if (estado.getFiltros() == null || estado.getItens() == null || estado.getItens().size() > 50) return null;
                estado.setItens(minimizar(estado.getItens()));
                if (estado.getSelecionadaId() != null && estado.getItens().stream()
                        .noneMatch(item -> Objects.equals(item.getReservaId(), estado.getSelecionadaId()))) return null;
                return estado;
            } catch (Exception ignored) { return null; }
        }
        return null;
    }

    public static String anexar(String existente, ChatResponseDTO resposta, Long conversa, ObjectMapper mapper) {
        JsonNode consulta = payload(resposta, mapper);
        if (consulta == null) return existente;
        try {
            JsonNode original = ler(existente, mapper);
            ObjectNode raiz = original instanceof ObjectNode ? (ObjectNode) original : mapper.createObjectNode();
            if (!consulta.path("contexto").isObject() && FALHAS.contains(consulta.path("statusConsulta").asText())) {
                ObjectNode barreira = mapper.createObjectNode();
                barreira.put("schema", SCHEMA);
                barreira.put("statusConsulta", consulta.path("statusConsulta").asText());
                barreira.put("resultadoTarefa", consulta.path("resultadoTarefa").asText(consulta.path("statusConsulta").asText()));
                raiz.set("consultaReservas", barreira);
                return mapper.writeValueAsString(raiz);
            }
            ReservaConsultaEstado contexto = mapper.treeToValue(consulta.path("contexto"), ReservaConsultaEstado.class);
            if (!positivo(conversa) || !Objects.equals(conversa, contexto.getConversa())
                    || !positivo(contexto.getAgencia()) || !positivo(contexto.getUsuario()) || contexto.getFiltros() == null) {
                throw new IllegalArgumentException("Contexto da consulta nao pertence a esta conversa.");
            }
            contexto.setItens(minimizar(contexto.getItens()));
            ObjectNode copia = ((ObjectNode) consulta).deepCopy();
            copia.set("contexto", mapper.valueToTree(contexto));
            raiz.set("consultaReservas", copia);
            return mapper.writeValueAsString(raiz);
        } catch (Exception ex) { throw new IllegalStateException("Nao foi possivel persistir contexto autorizado da consulta.", ex); }
    }

    public static JsonNode payload(ChatResponseDTO resposta, ObjectMapper mapper) {
        if (resposta == null || resposta.history() == null || mapper == null) return null;
        for (int i = resposta.history().size() - 1; i >= 0; i--) {
            var mensagem = resposta.history().get(i);
            if (mensagem == null || !"system".equals(mensagem.role())) continue;
            JsonNode valor = ler(mensagem.content(), mapper);
            if (valor != null && valor.isObject() && SCHEMA.equals(valor.path("schema").asText())) return valor;
        }
        return null;
    }

    private static List<ReservaAereaRecenteItem> minimizar(List<ReservaAereaRecenteItem> itens) {
        List<ReservaAereaRecenteItem> resultado = new ArrayList<>();
        if (itens == null) return resultado;
        for (var item : itens) {
            if (item == null || item.getReservaId() == null || item.getReservaId() < 1 || resultado.size() == 50) continue;
            ReservaAereaRecenteItem copia = new ReservaAereaRecenteItem();
            copia.setReservaId(item.getReservaId());
            copia.setLocalizador(curto(item.getLocalizador(), 20));
            copia.setStatus(item.getStatus());
            copia.setStatusDescricao(curto(item.getStatusDescricao(), 80));
            copia.setCompanhiaIata(curto(item.getCompanhiaIata(), 3));
            copia.setCompanhiaNome(curto(item.getCompanhiaNome(), 100));
            List<String> passageiros = new ArrayList<>();
            if (item.getPassageiros() != null) for (String nome : item.getPassageiros()) {
                if (passageiros.size() == 50) break;
                String seguro = curto(nome, 100);
                if (seguro != null && !seguro.isBlank()) passageiros.add(seguro);
            }
            copia.setPassageiros(passageiros);
            copia.setQuantidadePassageiros(item.getQuantidadePassageiros());
            List<ReservaAereaRecenteItem.Voo> voos = new ArrayList<>();
            if (item.getVoos() != null) for (var voo : item.getVoos()) {
                if (voo == null || voos.size() == 20) continue;
                var v = new ReservaAereaRecenteItem.Voo();
                v.setTrechoId(voo.getTrechoId());
                v.setVooId(voo.getVooId());
                v.setOrigem(curto(voo.getOrigem(), 100));
                v.setDestino(curto(voo.getDestino(), 100));
                v.setCompanhiaIata(curto(voo.getCompanhiaIata(), 3));
                v.setCompanhiaNome(curto(voo.getCompanhiaNome(), 100));
                v.setNumeroVoo(curto(voo.getNumeroVoo(), 10));
                voos.add(v);
            }
            copia.setVoos(voos);
            resultado.add(copia);
        }
        return resultado;
    }

    private static boolean interrompeAtendimento(String texto) {
        String t = ChatReservaConsultaParser.normalizar(texto);
        return t.matches(".*\\b(encerrad[ao]|transferid[ao]|atendente assumiu|atendimento humano iniciado)\\b.*");
    }
    private static boolean outraTarefa(JsonNode raiz) {
        if (raiz == null) return false;
        String intencao = raiz.path("confiaV2").path("intencao").asText();
        if (intencao.isBlank()) intencao = raiz.path("observabilidade").path("intencaoEfetiva").asText();
        if (intencao.isBlank()) intencao = raiz.path("decisaoIa").path("intencao").asText();
        return intencao.matches("(?i)(?:hotel|financeiro|seguro|carro|pacote|alertas?|familias?|aereo\\.familia|conversa\\.atendimento_humano)(?:[._].*)?");
    }
    private static boolean positivo(Long valor) { return valor != null && valor > 0; }
    private static String curto(String valor, int limite) { return valor == null ? null : valor.replaceAll("[\\p{Cntrl}]", " ").trim().substring(0, Math.min(limite, valor.replaceAll("[\\p{Cntrl}]", " ").trim().length())); }
    private static Long inteiroLongo(JsonNode n) { return n != null && n.isIntegralNumber() && n.canConvertToLong() ? n.longValue() : null; }
    private static JsonNode ler(String texto, ObjectMapper mapper) {
        if (texto == null || texto.isBlank() || texto.length() > 1_000_000) return null;
        try { return mapper.readTree(texto); } catch (Exception ignored) { return null; }
    }
}
