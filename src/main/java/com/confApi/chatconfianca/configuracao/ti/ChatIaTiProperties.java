package com.confApi.chatconfianca.configuracao.ti;

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Independent opt-in: both identity allowlists AND conversation sampling must match. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "chat-confianca.ia-config.ti-resposta")
public class ChatIaTiProperties {
    private boolean enabled;
    private int samplePercent;
    private Set<Integer> usuarios = new HashSet<>();
    private Set<Integer> agencias = new HashSet<>();

    public boolean permite(Integer usuario, Integer agencia, Long conversa) {
        return enabled && usuario != null && usuario > 0 && agencia != null && agencia > 0
                && conversa != null && conversa > 0
                && usuarios != null && usuarios.contains(usuario)
                && agencias != null && agencias.contains(agencia)
                && samplePercent > 0 && samplePercent <= 100
                && Math.floorMod(Long.hashCode(conversa), 100) < samplePercent;
    }
}
