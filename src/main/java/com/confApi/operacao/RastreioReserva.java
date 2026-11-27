package com.confApi.operacao;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 9)
public final class RastreioReserva extends OncePerRequestFilter {
    private static final String CABECALHO = "X-Reserva-Operacao-Id";
    private static final String MDC_ID = "reservaOperacaoId";
    private static final Pattern UUID_CANONICO = Pattern.compile("[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}");
    private static final Pattern ROTAS = Pattern.compile("/(?:"
            + "v[12]/hoteis/(?:efetuarReserva2?|cancelaHotel)|"
            + "v1/carro/(?:reservar|emitir|cancelarReserva)|"
            + "v1/seguro/(?:comprar|cancelarReserva)|"
            + "(?:v1/)?rodoviario/(?:reservas/)?(?:reservar|emitir|cancelar|cancelarreserva|cancelarcompra|cancelarbilhete|reconciliar)"
            + ")/?", Pattern.CASE_INSENSITIVE);

    public static String id() {
        return MDC.get(MDC_ID);
    }

    public static void propagar(HttpHeaders headers) {
        if (id() != null) headers.set(CABECALHO, id());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest pedido) {
        String caminho = pedido.getRequestURI().substring(pedido.getContextPath().length());
        return !"POST".equals(pedido.getMethod()) || !ROTAS.matcher(caminho).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest pedido, HttpServletResponse resposta, FilterChain cadeia)
            throws IOException, ServletException {
        String anterior = id();
        String recebido = pedido.getHeader(CABECALHO);
        String id = recebido != null && UUID_CANONICO.matcher(recebido).matches()
                ? UUID.fromString(recebido).toString() : UUID.randomUUID().toString();
        MDC.put(MDC_ID, id);
        resposta.setHeader(CABECALHO, id);
        try {
            cadeia.doFilter(pedido, resposta);
        } finally {
            if (anterior == null) MDC.remove(MDC_ID);
            else MDC.put(MDC_ID, anterior);
        }
    }
}