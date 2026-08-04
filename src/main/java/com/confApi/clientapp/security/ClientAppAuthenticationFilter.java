package com.confApi.clientapp.security;

import com.confApi.clientapp.config.MViagensBackendProperties;
import com.confApi.clientapp.integration.MViagensBackendClient;
import com.confApi.clientapp.integration.MViagensBackendUnavailableException;
import com.confApi.clientapp.integration.MViagensIntrospectionResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public final class ClientAppAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final AntPathRequestMatcher CLIENT_APP_PATH =
            new AntPathRequestMatcher("/api/client-app/v1/**");

    private final MViagensBackendProperties properties;
    private final ObjectProvider<MViagensBackendClient> backendClientProvider;
    private final ClientAppSecurityErrorWriter errorWriter;

    public ClientAppAuthenticationFilter(
            MViagensBackendProperties properties,
            ObjectProvider<MViagensBackendClient> backendClientProvider,
            ClientAppSecurityErrorWriter errorWriter
    ) {
        this.properties = properties;
        this.backendClientProvider = backendClientProvider;
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !CLIENT_APP_PATH.matches(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = ClientAppCorrelationIdFilter.from(request);
        SecurityContextHolder.clearContext();

        if (!properties.isEnabled()) {
            errorWriter.unavailable(response, correlationId);
            return;
        }

        String accessToken = extractBearer(request);
        if (accessToken == null) {
            errorWriter.unauthorized(response, correlationId);
            return;
        }

        MViagensBackendClient backendClient = backendClientProvider.getIfAvailable();
        if (backendClient == null) {
            errorWriter.unavailable(response, correlationId);
            return;
        }

        try {
            MViagensIntrospectionResponse introspection = backendClient.introspect(accessToken, correlationId);
            if (!Boolean.TRUE.equals(introspection.active())) {
                errorWriter.unauthorized(response, correlationId);
                return;
            }

            if (!introspection.scopes().contains("customer:authenticated")) {
                errorWriter.forbidden(response, correlationId);
                return;
            }
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("SCOPE_customer:authenticated")
            );

            ClientAppPrincipal principal = new ClientAppPrincipal(
                    introspection.customerPublicId(),
                    introspection.sessionPublicId(),
                    introspection.devicePublicId(),
                    introspection.agencyLinkPublicId(),
                    introspection.activeAgencyId(),
                    introspection.sessionVersion(),
                    introspection.contextVersion(),
                    List.of("customer:authenticated")
            );
            ClientAppAuthenticationToken authentication =
                    new ClientAppAuthenticationToken(principal, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (MViagensBackendUnavailableException exception) {
            SecurityContextHolder.clearContext();
            errorWriter.unavailable(response, correlationId);
        }
    }

    private String extractBearer(HttpServletRequest request) {
        List<String> values = Collections.list(request.getHeaders(HttpHeadersName.AUTHORIZATION));
        if (values.size() != 1) {
            return null;
        }

        String value = values.get(0);
        if (value == null
                || value.length() > 8200
                || !value.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }

        String token = value.substring(BEARER_PREFIX.length());
        return token.isEmpty() || !token.equals(token.trim()) ? null : token;
    }

    private static final class HttpHeadersName {
        private static final String AUTHORIZATION = "Authorization";

        private HttpHeadersName() {
        }
    }
}
