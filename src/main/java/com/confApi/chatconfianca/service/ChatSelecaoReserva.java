package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.dto.enums.RemetenteTipo;
import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatgpt.util.LocalizadorAereo;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

/** Referencias somente a lista recente da conversa ja autorizada, nunca memoria global. */
final class ChatSelecaoReserva {
    private ChatSelecaoReserva() { }
    record Selecao(String localizador, boolean ambigua) { }

    static Selecao identificar(String texto, Long conversaId, List<Mensagem> historico, ObjectMapper mapper) {
        String t = ChatPedidoOperacional.normalizar(texto);
        if (t.length() < 3 || t.length() > 100 || historico == null || conversaId == null) return null;
        if (t.matches(".*\\b(financeiro|faturas?|boletos?|limites?|hotel|hoteis|seguro|carro|pacote|vendas|atendente|origem|destino|reembolso|cancelar|simular|remarcar)\\b.*")) return null;
        for (int i = historico.size()-1; i >= 0; i--) {
            Mensagem m = historico.get(i);
            if (m == null || m.getRemetenteTipo() != RemetenteTipo.BOT || m.getExcluidaEm() != null) continue;
            if (m.getConversaId() == null || !conversaId.equals(m.getConversaId()) || m.getEnviadaEm() == null
                    || m.getEnviadaEm().isAfter(LocalDateTime.now())
                    || m.getEnviadaEm().isBefore(LocalDateTime.now().minusMinutes(30))) return null;
            try {
                var json = mapper.readTree(m.getConteudoJson());
                var reservas = json.path("reservasRecentes").path("reservas");
                if (!reservas.isArray()) return null;
                LinkedHashSet<String> encontrados = new LinkedHashSet<>();
                for (var r : reservas) {
                    String loc = r.path("localizador").asText();
                    if (!LocalizadorAereo.isValido(loc)) continue;
                    if (loc.equalsIgnoreCase(texto.trim())) encontrados.add(loc);
                    for (var passageiro : r.path("passageiros")) {
                        String nome = ChatPedidoOperacional.normalizar(passageiro.isTextual() ? passageiro.asText() : passageiro.path("nome").asText());
                        if (!nome.isBlank() && (nome.equals(t) || nome.startsWith(t + " "))) encontrados.add(loc);
                    }
                }
                if (encontrados.size() == 1) return new Selecao(encontrados.iterator().next(), false);
                return encontrados.size() > 1 ? new Selecao(null, true) : null;
            } catch (Exception ignored) { return null; }
        }
        return null;
    }
}
