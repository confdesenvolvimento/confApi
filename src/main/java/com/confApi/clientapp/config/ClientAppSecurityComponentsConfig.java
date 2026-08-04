package com.confApi.clientapp.config;

import com.confApi.clientapp.integration.MViagensBackendClient;
import com.confApi.clientapp.security.ClientAppAuthenticationFilter;
import com.confApi.clientapp.security.ClientAppCorrelationIdFilter;
import com.confApi.clientapp.security.ClientAppSecurityErrorWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MViagensBackendProperties.class)
public class ClientAppSecurityComponentsConfig {

    @Bean
    ClientAppSecurityErrorWriter clientAppSecurityErrorWriter(ObjectMapper objectMapper) {
        return new ClientAppSecurityErrorWriter(objectMapper);
    }

    @Bean
    ClientAppCorrelationIdFilter clientAppCorrelationIdFilter() {
        return new ClientAppCorrelationIdFilter();
    }

    @Bean
    ClientAppAuthenticationFilter clientAppAuthenticationFilter(
            MViagensBackendProperties properties,
            ObjectProvider<MViagensBackendClient> backendClientProvider,
            ClientAppSecurityErrorWriter errorWriter
    ) {
        return new ClientAppAuthenticationFilter(properties, backendClientProvider, errorWriter);
    }

    @Bean
    FilterRegistrationBean<ClientAppCorrelationIdFilter> disableClientAppCorrelationContainerRegistration(
            ClientAppCorrelationIdFilter filter
    ) {
        FilterRegistrationBean<ClientAppCorrelationIdFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<ClientAppAuthenticationFilter> disableClientAppAuthenticationContainerRegistration(
            ClientAppAuthenticationFilter filter
    ) {
        FilterRegistrationBean<ClientAppAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
