package com.confApi.mobile;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Registrado apenas na cadeia de segurança da API, antes do JWT técnico. */
public class MobileChatIdentityFilter extends OncePerRequestFilter {
    private final MobileChatDelegations delegations;
    public MobileChatIdentityFilter(MobileChatDelegations delegations) { this.delegations = delegations; }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                              FilterChain chain) throws ServletException, IOException {
        String token = request.getHeader(MobileChatDelegations.HEADER);
        if (token == null) { chain.doFilter(request, response); return; }
        byte[] body = request.getInputStream().readNBytes(20_000_001);
        MobileChatAuthentication authentication;
        try {
            if (body.length > 20_000_000) throw new MobileFailure(413, "CORPO_EXCEDIDO");
            authentication = delegations.redeem(token, request.getMethod(),
                    request.getRequestURI().substring(request.getContextPath().length()), request.getQueryString(), body);
        } catch (MobileFailure failure) {
            response.setStatus(failure.status);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"" + failure.code + "\"}");
            return;
        }
        var previous = SecurityContextHolder.getContext();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        try { chain.doFilter(new BodyRequest(request, body), response); }
        finally { SecurityContextHolder.setContext(previous); }
    }

    private static final class BodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;
        BodyRequest(HttpServletRequest request, byte[] body) { super(request); this.body = body; }
        @Override public ServletInputStream getInputStream() {
            var input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return input.read(); }
                @Override public int read(byte[] b, int offset, int length) { return input.read(b, offset, length); }
                @Override public boolean isFinished() { return input.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException("Synchronous request"); }
            };
        }
        @Override public BufferedReader getReader() {
            String encoding = getCharacterEncoding();
            return new BufferedReader(new InputStreamReader(getInputStream(),
                    encoding == null ? StandardCharsets.UTF_8 : java.nio.charset.Charset.forName(encoding)));
        }
    }
}
