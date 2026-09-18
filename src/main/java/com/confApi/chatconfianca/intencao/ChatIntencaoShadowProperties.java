package com.confApi.chatconfianca.intencao;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "chat-confianca.intencao-v1")
public class ChatIntencaoShadowProperties {
    private boolean shadowEnabled;
    private boolean memoryShadowEnabled;
    private boolean recoveryAuditEnabled;
    private boolean auditEnabled = true;
    private boolean decisionAuditEnabled = true;
    private boolean unifiedDecisionEnabled;
    private boolean unifiedDecisionCanaryEnabled = true;
    private List<String> unifiedDecisionCanaryIntentionPrefixes =
            new ArrayList<>(List.of("institucional."));
    private String versaoClassificador = "termos-v1.0";
    private String versaoRecuperador = "vinculos-v1.0";
    private String versaoDecisor = "decisor-v1.1";
    private BigDecimal minScore = new BigDecimal("8.000");
    private BigDecimal minMargin = new BigDecimal("2.000");
}
