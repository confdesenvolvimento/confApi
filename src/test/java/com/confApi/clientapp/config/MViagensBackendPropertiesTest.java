package com.confApi.clientapp.config;

import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class MViagensBackendPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsEmptyTechnicalConfigurationWhileFeatureIsDisabled() {
        MViagensBackendProperties properties = new MViagensBackendProperties();

        assertThat(validator.validate(properties)).isEmpty();
    }

    @Test
    void rejectsEnabledConfigurationWithShortServiceToken() {
        MViagensBackendProperties properties = validEnabledProperties();
        properties.setServiceToken("short");

        assertThat(validator.validate(properties)).isNotEmpty();
    }

    @Test
    void rejectsPlainHttpOutsideLoopback() {
        MViagensBackendProperties properties = validEnabledProperties();
        properties.setBaseUrl(URI.create("http://mviagens.internal:8091"));

        assertThat(validator.validate(properties)).isNotEmpty();
    }

    @Test
    void acceptsHttpsWhenEnabled() {
        MViagensBackendProperties properties = validEnabledProperties();
        properties.setBaseUrl(URI.create("https://mviagens.internal"));

        assertThat(validator.validate(properties)).isEmpty();
    }

    private MViagensBackendProperties validEnabledProperties() {
        MViagensBackendProperties properties = new MViagensBackendProperties();
        properties.setEnabled(true);
        properties.setBaseUrl(URI.create("http://127.0.0.1:8091"));
        properties.setServiceToken("a-secure-technical-token-with-32-chars");
        return properties;
    }
}
