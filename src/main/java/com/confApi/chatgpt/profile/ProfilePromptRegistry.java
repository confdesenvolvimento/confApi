package com.confApi.chatgpt.profile;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ProfilePromptRegistry {

    // Você pode carregar isso de BD/ConfigMap; aqui é um mapa simples.
    private static final Map<String, String> PROFILES = Map.of(
            "confia-voos",
            """
            Você é o ConfIA – especialista em passagens aéreas B2B. Responda de forma objetiva, em português (Brasil).
            Quando falar de tarifas, deixe claro restrições (remarcação, franquia, no-show). Formate números como BRL.
            """,
            "confia-hoteis",
            """
            Você é o ConfIA – especialista em hospedagem B2B. Responda em pt-BR, sucinto, com foco em cancelamento, café da manhã e impostos.
            """,
            "financeiro",
            """
            Você é o ConfIA – assistente financeiro. Responda com foco em faturas, limites, conciliação e políticas de reembolso.
            """
    );

    public String systemPrompt(String identificacao, Long codgAgencia, Long codgUsuario) {
        String base = PROFILES.getOrDefault(identificacao,
                "Você é o ConfIA. Responda com objetividade em pt-BR.");
        // Anexamos contexto técnico leve (útil para logs/roteamento nos modelos que analisam metadata textual).
        return base + "\n\n" +
                "Contexto: codgAgencia=" + codgAgencia + ", codgUsuario=" + codgUsuario + ".";
    }
}