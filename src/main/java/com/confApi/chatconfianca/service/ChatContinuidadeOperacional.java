package com.confApi.chatconfianca.service;

import com.confApi.chatconfianca.dto.enums.RemetenteTipo;
import com.confApi.chatconfianca.dto.model.Mensagem;
import com.confApi.chatgpt.util.LocalizadorAereo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/** Uses only persisted server cards from the already-authorized conversation. Never executes an operation. */
final class ChatContinuidadeOperacional {
    private ChatContinuidadeOperacional() { }

    static String completarLocalizador(String texto, List<Mensagem> historico, ObjectMapper mapper) {
        if (texto == null || !LocalizadorAereo.isValido(texto.trim())) return texto;
        String codigo = texto.trim();
        // A topic switch or conversational answer must not become a guessed reservation code.
        String palavra = normalizar(codigo);
        if (palavra.matches("financeiro|limites?|boletos?|faturas?|hotel|hoteis|hospedagem|aereo|aereas|reservas?|seguros?|carros?|pacotes?|contatos?|emergencia|suporte|atendente|cancelamento|reembolso|remarcacao|checkin|embarques|familias|calendario|tarifas|buscar|obrigad[oa]|ajuda|duvidas?|frequentes|perguntas|desisto|cancelar|cancele|reembolsar|transferir|continuar|aguardo")) return texto;
        if (!codigo.matches(".*[0-9].*") && !codigo.equals(codigo.toUpperCase(Locale.ROOT))) return texto;
        Mensagem ultima = ultimoBot(historico);
        if (!recente(ultima)) return texto;
        try {
            JsonNode json = mapper.readTree(ultima.getConteudoJson());
            for (JsonNode acao : json.path("actions")) {
                String codigoAcao = acao.path("code").asText();
                if ("selecionar_reserva_remarcacao".equals(codigoAcao) || "simular_remarcacao".equals(codigoAcao))
                    return "Simular remarcacao da reserva " + texto.trim().toUpperCase(Locale.ROOT);
            }
        } catch (Exception ignored) { }
        return texto;
    }

    static String orientarEtapa(String texto, Long conversaId, List<Mensagem> historico, ObjectMapper mapper) {
        String normalizado = normalizar(texto);
        boolean respostaCurta = normalizado.matches("(?:dia )?\\d{1,2}(?:[/-]\\d{1,2}| (?:jan|fev|mar|abr|mai|jun|jul|ago|set|out|nov|dez)[a-z]*)(?:[ /-]\\d{2,4})?[.!? ]*")
                || normalizado.matches("(?:voo )?(?:g3|ad|la|jj) ?\\d{2,4}[.!? ]*");
        if (!respostaCurta || historico == null || conversaId == null) return null;
        for (int i = historico.size() - 1; i >= 0; i--) {
            Mensagem m = historico.get(i);
            if (m == null || m.getRemetenteTipo() != RemetenteTipo.BOT || m.getExcluidaEm() != null) continue;
            // Do not resurrect an older task after another assistant reply or a topic switch.
            if (!recente(m)) return null;
            try {
                JsonNode card = mapper.readTree(m.getConteudoJson()).path("remarcacao");
                if (!card.isObject()) {
                    if (m.getConteudo() != null && (m.getConteudo().startsWith("Estamos na simulacao de remarcacao.")
                            || m.getConteudo().startsWith("Estamos escolhendo um voo para a remarcacao."))) continue;
                    return null;
                }
                if (card.path("conversaId").asLong(-1) != conversaId) return null;
                String expira = card.path("expiraEm").asText();
                if (expira.isBlank() || !LocalDateTime.parse(expira).isAfter(LocalDateTime.now())) return null;
                String status = card.path("status").asText();
                return switch (status) {
                    case "AGUARDANDO_CRITERIOS" -> "Estamos na simulacao de remarcacao. Informe essa data no campo Nova data do card da simulacao e toque em Pesquisar voos. A data enviada aqui nao alterou a reserva.";
                    case "AGUARDANDO_OPCAO" -> "Estamos escolhendo um voo para a remarcacao. No card da simulacao, selecione o voo e a familia tarifaria para calcular a previa. O numero do voo nao e um localizador; nenhuma alteracao foi realizada.";
                    case "AGUARDANDO_TRECHO" -> "Primeiro escolha o trecho completo no card da simulacao de remarcacao, incluindo suas conexoes. Depois podera informar a nova data. Nenhuma alteracao foi realizada.";
                    case "AGUARDANDO_PASSAGEIROS" -> "Primeiro escolha os passageiros no card da simulacao de remarcacao. Depois podera informar a nova data. Nenhuma alteracao foi realizada.";
                    default -> null;
                };
            } catch (Exception ignored) { return null; }
        }
        return null;
    }

    static String concluirSemPromessa(String texto) {
        if (texto == null) return null;
        // Prepared-search payloads are consumed by the UI, not a conversational waiting promise.
        if (texto.stripLeading().startsWith("{") || texto.stripLeading().startsWith("[")) return texto;
        String n = normalizar(texto);
        boolean promessa = n.matches("(?s).*(vou (?:buscar|consultar|verificar|pesquisar)|estou (?:buscando|consultando|verificando|pesquisando)).*");
        boolean espera = n.matches("(?s).*(um momento|aguarde|momento por favor|enquanto verifico|enquanto obtenho).*");
        if (!promessa || !espera) return texto;
        return "Nao tenho um resultado concluido para essa consulta. Posso ajudar a preparar a pesquisa com os dados informados ou encaminhar para atendimento humano. Nao ha uma consulta em andamento em segundo plano.";
    }

    private static Mensagem ultimoBot(List<Mensagem> historico) {
        if (historico != null) for (int i=historico.size()-1;i>=0;i--) {
            Mensagem m=historico.get(i);
            if(m!=null&&m.getRemetenteTipo()==RemetenteTipo.BOT&&m.getExcluidaEm()==null)return m;
        }
        return null;
    }
    private static boolean recente(Mensagem m) {
        if(m==null||m.getConteudoJson()==null||m.getEnviadaEm()==null)return false;
        LocalDateTime agora=LocalDateTime.now();
        return !m.getEnviadaEm().isAfter(agora)&&m.getEnviadaEm().isAfter(agora.minusMinutes(30));
    }
    private static String normalizar(String t) {
        return Normalizer.normalize(t==null?"":t,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).trim();
    }
}
