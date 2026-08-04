package com.confApi.clientapp.security;

import org.slf4j.MDC;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

public final class ClientAppCorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_ATTRIBUTE = ClientAppCorrelationIdFilter.class.getName() + ".id";
    private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9._-]{1,100}$");
    private static final AntPathRequestMatcher CLIENT_APP_PATH =
            new AntPathRequestMatcher("/api/client-app/v1/**");

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
        String correlationId = normalize(request.getHeader(CORRELATION_ID_HEADER));
        request.setAttribute(CORRELATION_ID_ATTRIBUTE, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        MDC.put("correlationId", correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }

    public static String from(HttpServletRequest request) {
        Object value = request.getAttribute(CORRELATION_ID_ATTRIBUTE);
        return value instanceof String ? (String) value : UUID.randomUUID().toString();
    }

    private String normalize(String value) {
        return value != null && SAFE_ID.matcher(value).matches() ? value : UUID.randomUUID().toString();
    }
}
