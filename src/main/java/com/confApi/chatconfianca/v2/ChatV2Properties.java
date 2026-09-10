package com.confApi.chatconfianca.v2;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Rollout opt-in. Existing V1/canary configuration is untouched. */
@Data
@Component
@ConfigurationProperties(prefix = "chat-confianca.v2")
public class ChatV2Properties {
    private boolean enabled;
    private boolean semanticEnabled = true;
    private boolean legacyFallbackEnabled = true;
    private int trafficPercent;
    private List<Integer> agencias = new ArrayList<>();
    private List<String> intencoes = new ArrayList<>(List.of("*"));
    private int timeoutSeconds = 12;
    private int contextMinutes = 60;

    public boolean participa(Integer agencia, Integer usuario) {
        if (!enabled || agencia == null || agencia <= 0 || usuario == null || usuario <= 0) return false;
        if (agencias != null && !agencias.isEmpty() && !agencias.contains(agencia)) return false;
        int percentual = Math.max(0, Math.min(100, trafficPercent));
        return Math.floorMod(Objects.hash(agencia, usuario), 100) < percentual;
    }

    public boolean permite(String codigo) {
        return intencoes != null && intencoes.stream().anyMatch(i -> "*".equals(i) || Objects.equals(i, codigo));
    }
}
