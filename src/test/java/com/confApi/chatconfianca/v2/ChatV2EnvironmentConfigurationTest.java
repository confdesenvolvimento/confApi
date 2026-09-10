package com.confApi.chatconfianca.v2;

import com.confApi.chatconfianca.configuracao.ti.ChatIaTiProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import static org.junit.jupiter.api.Assertions.*;

/** Configuration binding only: no application scan, external client or scheduler. */
class ChatV2EnvironmentConfigurationTest {
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({ChatV2Properties.class, ChatIaTiProperties.class})
    static class BindingOnly {}

    private ApplicationContextRunner runner(Map<String, Object> variables) {
        return new ApplicationContextRunner()
                .withUserConfiguration(BindingOnly.class)
                .withPropertyValues("spring.config.location=classpath:/application.properties",
                        "spring.profiles.active=prod")
                .withInitializer(context -> {
                    context.getEnvironment().getPropertySources().replace(
                            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                            new SystemEnvironmentPropertySource(
                                    StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, variables));
                    new ConfigDataApplicationContextInitializer().initialize(context);
                });
    }

    @Test
    void variaveisDocumentadasDesligamV2SemAtivarPilotoTi() {
        runner(Map.of("CHAT_CONFIANCA_V2_ENABLED", "false",
                "CHAT_CONFIANCA_V2_TRAFFIC_PERCENT", "0")).run(context -> {
            assertNull(context.getStartupFailure());
            var properties = context.getBean(ChatV2Properties.class);
            assertFalse(properties.isEnabled());
            assertEquals(0, properties.getTrafficPercent());
            assertFalse(properties.participa(501, 101));
            assertFalse(context.getBean(ChatIaTiProperties.class).isEnabled());
        });
    }

    @Test
    void variaveisPermitemTodosAssuntosMantendoIdentidadeObrigatoria() {
        runner(Map.of("CHAT_CONFIANCA_V2_ENABLED", "true",
                "CHAT_CONFIANCA_V2_TRAFFIC_PERCENT", "100",
                "CHAT_CONFIANCA_V2_INTENCOES", "*",
                "CHAT_CONFIANCA_V2_AGENCIAS", "")).run(context -> {
            assertNull(context.getStartupFailure());
            var properties = context.getBean(ChatV2Properties.class);
            assertTrue(properties.isEnabled());
            assertEquals(100, properties.getTrafficPercent());
            assertTrue(properties.participa(501, 101));
            assertFalse(properties.participa(null, 101));
            assertFalse(properties.participa(501, null));
            for (var capability : ChatV2Capability.values()) assertTrue(properties.permite(capability.code));
            assertTrue(properties.isLegacyFallbackEnabled());
            assertFalse(context.getBean(ChatIaTiProperties.class).isEnabled());
        });
    }

    @Test
    void variaveisPreservamFiltroDeAgenciaEIntencao() {
        runner(Map.of("CHAT_CONFIANCA_V2_ENABLED", "true",
                "CHAT_CONFIANCA_V2_TRAFFIC_PERCENT", "100",
                "CHAT_CONFIANCA_V2_AGENCIAS", "501",
                "CHAT_CONFIANCA_V2_INTENCOES", "financeiro.limite")).run(context -> {
            assertNull(context.getStartupFailure());
            var properties = context.getBean(ChatV2Properties.class);
            assertTrue(properties.participa(501, 101));
            assertFalse(properties.participa(999, 101));
            assertTrue(properties.permite("financeiro.limite"));
            assertFalse(properties.permite("institucional.contato"));
        });
    }
}
