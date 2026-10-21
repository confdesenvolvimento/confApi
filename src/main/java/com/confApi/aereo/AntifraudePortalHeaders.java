package com.confApi.aereo;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Aceita origem declarada apenas do consumidor e do peer configurados do portal. */
@Component
public class AntifraudePortalHeaders {
    private final Set<String> clients;
    private final Set<String> peers;

    public AntifraudePortalHeaders(@Value("${antifraude.portal-clientes:}") String clients,
                                   @Value("${antifraude.portal-peers:}") String peers) {
        this.clients = Arrays.stream(clients.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        this.peers = Arrays.stream(peers.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(AntifraudePortalHeaders::numericPeer).collect(Collectors.toSet());
    }

    public HttpHeaders extract(HttpServletRequest request) {
        HttpHeaders result = new HttpHeaders();
        String operation = request.getHeader("X-Antifraude-Operacao");
        if (operation == null || operation.isBlank()) return result;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !clients.contains(auth.getName())
                || !peers.contains(numericPeer(request.getRemoteAddr()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Origem da confirmação não autorizada.");
        }
        String user = request.getHeader("X-Usuario-Token");
        String ip = request.getHeader("X-Antifraude-IP");
        if (!operation.matches("[a-fA-F0-9-]{36}") || user != null && (user.length() > 8192 || user.contains("\r") || user.contains("\n"))
                || ip == null || ip.length() > 45) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contexto de confirmação inválido.");
        }
        numericPeer(ip);
        result.set("X-Antifraude-Operacao", operation);
        if (user != null && !user.isBlank()) result.set("X-Usuario-Token", user);
        result.set("X-Antifraude-IP", ip);
        for (String name : java.util.List.of("X-Antifraude-Contexto", "X-Antifraude-Sessao")) {
            String value = request.getHeader(name);
            if (value != null) {
                if (!value.matches("[a-f0-9]{64}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contexto inválido.");
                result.set(name, value);
            }
        }
        return result;
    }

    private static String numericPeer(String value) {
        if (value == null || value.length() > 45
                || !(value.contains(":") && value.matches("[0-9a-fA-F:.]+") || value.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+"))) {
            throw new IllegalArgumentException("Endereço numérico obrigatório.");
        }
        try { return InetAddress.getByName(value).getHostAddress(); }
        catch (java.net.UnknownHostException ex) { throw new IllegalArgumentException("Endereço numérico inválido."); }
    }
}
