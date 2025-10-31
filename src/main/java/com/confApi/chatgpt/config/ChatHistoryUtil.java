package com.confApi.chatgpt.config;

import com.confApi.chatgpt.dto.ChatMessageDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChatHistoryUtil {

    private ChatHistoryUtil() {}

    // ======= Configurações padrão (ajuste conforme seu modelo) =======
    /** Orçamento total aproximado de tokens para o histórico */
    public static final int DEFAULT_MAX_TOKENS = 12000;        // ex.: gpt-4-turbo / gpt-4o long context
    /** Aproximação: 1 token ≈ 4 caracteres */
    public static final int DEFAULT_CHARS_PER_TOKEN = 4;

    /** Reserva aproximada (tokens) para a primeira system */
    public static final int DEFAULT_RESERVED_SYSTEM_TOKENS = 800;   // ~3200 chars
    /** Quantidade mínima de trocas recentes (user/assistant) a preservar */
    public static final int DEFAULT_MIN_RECENT_EXCHANGES = 3;
    /** Reserva aproximada (tokens) para as últimas trocas */
    public static final int DEFAULT_RESERVED_RECENT_TOKENS = 2500;  // ~10k chars

    /** Limite de caracteres para conteúdo de mensagens tool (após cortar, adiciona “...”) */
    public static final int DEFAULT_TOOL_CHAR_LIMIT = 6000;

    // ======= API pública simples =======

    public static List<ChatMessageDTO> trimHistory(List<ChatMessageDTO> history) {
        return trimHistory(history,
                DEFAULT_MAX_TOKENS,
                DEFAULT_CHARS_PER_TOKEN,
                DEFAULT_RESERVED_SYSTEM_TOKENS,
                DEFAULT_MIN_RECENT_EXCHANGES,
                DEFAULT_RESERVED_RECENT_TOKENS,
                DEFAULT_TOOL_CHAR_LIMIT
        );
    }

    // ======= API pública configurável =======

    public static List<ChatMessageDTO> trimHistory(
            List<ChatMessageDTO> history,
            int maxTokens,
            int approxCharsPerToken,
            int reservedSystemTokens,
            int minRecentExchanges,
            int reservedRecentTokens,
            int toolCharLimit
    ) {
        if (history == null || history.isEmpty()) return new ArrayList<>();

        // Clona para evitar mutação externa
        List<ChatMessageDTO> src = new ArrayList<>(history);

        // 1) Identifica a primeira system (se existir)
        int systemIndex = firstIndexOfRole(src, "system");
        ChatMessageDTO systemMsg = (systemIndex >= 0) ? src.get(systemIndex) : null;

        // 2) Prepara lista base (sem a system), já limitando conteúdo de tool
        List<ChatMessageDTO> withoutSystem = new ArrayList<>();
        for (int i = 0; i < src.size(); i++) {
            if (i == systemIndex) continue;
            ChatMessageDTO m = limitToolIfNeeded(src.get(i), toolCharLimit);
            if (m != null) withoutSystem.add(m);
        }

        // 3) Garante as últimas N trocas (user/assistant)
        // Estratégia: percorre do fim ao início, coletando mensagens até cobrir N "pivôs" de usuário ou assistente.
        List<ChatMessageDTO> recentTail = collectRecentExchanges(withoutSystem, minRecentExchanges);

        // 4) Converte orçamento de tokens para caracteres
        int maxCharsTotal = maxTokens * approxCharsPerToken;
        int reservedSystemChars = reservedSystemTokens * approxCharsPerToken;
        int reservedRecentChars = reservedRecentTokens * approxCharsPerToken;

        // 5) Monta o resultado final, começando por system (se houver)
        List<ChatMessageDTO> result = new ArrayList<>();
        int usedChars = 0;

        if (systemMsg != null) {
            int sysLen = lengthOf(systemMsg);
            // Se a system for maior que a reserva, ainda assim a preservamos
            usedChars += sysLen;
            result.add(systemMsg);
        }

        // 6) Adiciona as últimas trocas (na ordem original)
        int recentChars = 0;
        for (ChatMessageDTO m : recentTail) {
            int len = lengthOf(m);
            if (recentChars + len <= reservedRecentChars || result.size() == 1 /* manter fluidez */) {
                result.add(m);
                recentChars += len;
                usedChars += len;
            } else {
                break; // estourou a reserva recente
            }
        }

        // 7) Preenche com mensagens mais antigas (anteriores ao bloco recentTail), até consumir o orçamento total
        // Precisamos saber quais mensagens restaram além de recentTail
        List<ChatMessageDTO> remaining = new ArrayList<>(withoutSystem);
        // remove tudo o que já está no recentTail
        remaining.removeAll(recentTail);

        // Vamos adicionar do começo (mais antigas primeiro) ou do meio?
        // Estratégia: adicionar do MEIO para trás (para manter coerência incremental) pode complicar;
        // aqui vamos adicionar do início (as mais antigas) mantendo coerência quando o histórico não é gigante.
        for (ChatMessageDTO m : remaining) {
            int len = lengthOf(m);
            if (usedChars + len <= maxCharsTotal) {
                result.add(result.size() > 0 ? result.size() : 0, m); // opcional: inserir antes das recentes
                usedChars += len;
            } else {
                break;
            }
        }

        // 8) Caso o resultado tenha passado do orçamento (em cenários extremos), aparar do início (exceto system)
        result = enforceMaxChars(result, maxCharsTotal, systemMsg != null);

        return result;
    }

    // ======= Helpers =======

    private static int firstIndexOfRole(List<ChatMessageDTO> list, String role) {
        for (int i = 0; i < list.size(); i++) {
            if (roleEquals(list.get(i), role)) return i;
        }
        return -1;
    }

    private static boolean roleEquals(ChatMessageDTO m, String role) {
        return m != null && m.role() != null && m.role().equalsIgnoreCase(role);
    }

    private static ChatMessageDTO limitToolIfNeeded(ChatMessageDTO msg, int toolCharLimit) {
        if (msg == null) return null;
        if (!roleEquals(msg, "tool")) return msg;
        String content = safe(msg.content());
        if (content.length() <= toolCharLimit) return msg;

        String truncated = content.substring(0, Math.max(0, toolCharLimit - 3)) + "...";
        return new ChatMessageDTO(msg.role(), truncated);
    }

    /**
     * Coleta um “bloco” de últimas trocas (user/assistant),
     * caminhando do fim ao início até cobrir minRecentExchanges “pivôs”.
     */
    private static List<ChatMessageDTO> collectRecentExchanges(List<ChatMessageDTO> messages, int minRecentExchanges) {
        if (messages.isEmpty() || minRecentExchanges <= 0) return new ArrayList<>();

        List<ChatMessageDTO> tail = new ArrayList<>();
        int pivots = 0;

        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessageDTO m = messages.get(i);
            tail.add(m);
            if (roleEquals(m, "user") || roleEquals(m, "assistant")) {
                pivots++;
                if (pivots >= minRecentExchanges) break;
            }
        }

        Collections.reverse(tail); // volta à ordem natural
        return tail;
    }

    private static int lengthOf(ChatMessageDTO m) {
        if (m == null) return 0;
        // Mede por conteúdo + papel (mínimo) para aproximar custo
        int base = safe(m.content()).length() + safe(m.role()).length();
        // Dê um pequeno peso extra a tool (metadados internos costumam ser verbosos)
        if (roleEquals(m, "tool")) base = (int) Math.round(base * 1.10);
        return base;
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    /**
     * Enforce final: se exceder maxChars, remove do início mantendo a system (se existir).
     */
    private static List<ChatMessageDTO> enforceMaxChars(List<ChatMessageDTO> list, int maxChars, boolean hasSystem) {
        int total = 0;
        for (ChatMessageDTO m : list) total += lengthOf(m);
        if (total <= maxChars) return list;

        List<ChatMessageDTO> out = new ArrayList<>(list);
        int startIdx = hasSystem ? 1 : 0;

        while (total > maxChars && out.size() > startIdx) {
            ChatMessageDTO removed = out.remove(startIdx); // remove do começo (preserva system)
            total -= lengthOf(removed);
        }
        return out;
    }
}
