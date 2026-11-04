package com.confApi.rodoviario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Registra a chegada inclusive quando o Spring Security rejeita o acesso. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RodoviarioRequestLoggingFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID = "rodoviarioRequestId";
    public static final String HEADER = "X-Rodoviario-Request-Id";
    private static final Logger log = LoggerFactory.getLogger(RodoviarioRequestLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = path(request);
        return !(path.equals("/v1/rodoviario") || path.startsWith("/v1/rodoviario/")
                || path.equals("/rodoviario") || path.startsWith("/rodoviario/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String previousId = MDC.get(REQUEST_ID);
        String requestId = UUID.randomUUID().toString();
        long start = System.nanoTime();
        boolean completed = false;
        MDC.put(REQUEST_ID, requestId);
        response.setHeader(HEADER, requestId);
        try {
            log.info("Rodoviario entrada requestId={} metodo={} rota={}", requestId,
                    safe(request.getMethod()), safe(path(request)));
            chain.doFilter(request, response);
            completed = true;
        } finally {
            // Nao registrar query, headers ou corpo: podem conter tokens e dados pessoais.
            log.info("Rodoviario saida requestId={} status={} duracaoMs={}", requestId,
                    completed ? response.getStatus() : 500,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            if (previousId == null) MDC.remove(REQUEST_ID); else MDC.put(REQUEST_ID, previousId);
        }
    }

    private static String path(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    static String safe(String value) {
        if (value == null) return "-";
        String clean = value.split("[?#]", 2)[0].replaceAll("[\\p{Cntrl}]", "_");
        return clean.substring(0, Math.min(clean.length(), 200));
    }
}