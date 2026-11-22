package com.confApi.mobile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.net.InetAddress;
import java.util.*;

/** Resolves the transport peer and only consumes X-Forwarded-For from configured proxies. */
@Component
public class MobileClientIp {
    private final List<IpAddressMatcher> trusted;
    public MobileClientIp(@Value("${mobile.client-ip.trusted-proxies:}") String proxies) {
        trusted = new ArrayList<>();
        for (String entry : proxies.split(",")) {
            if (entry.isBlank()) continue;
            String cidr = entry.trim();
            literal(cidr.split("/", -1)[0]);
            trusted.add(new IpAddressMatcher(cidr));
        }
    }

    public String resolve(HttpServletRequest request) {
        // Spring's ForwardedHeaderFilter wraps the request. Read the original peer and
        // headers, so an untrusted caller cannot select its IP through that wrapper.
        while (request instanceof HttpServletRequestWrapper wrapper) {
            request = (HttpServletRequest) wrapper.getRequest();
        }
        String peer = literal(request.getRemoteAddr());
        if (!trusted(peer)) return peer;
        String forwarded = String.join(",", Collections.list(request.getHeaders("X-Forwarded-For")));
        if (forwarded.isBlank() || forwarded.length() > 4096) throw invalid();
        String[] chain = forwarded.split(",", -1);
        if (chain.length > 20) throw invalid();
        for (int i = chain.length - 1; i >= 0 && trusted(peer); i--) {
            peer = literal(chain[i].trim());
        }
        if (trusted(peer)) throw invalid(); // The chain must contain a client beyond the trusted proxies.
        return peer;
    }

    private boolean trusted(String ip) {
        return trusted.stream().anyMatch(proxy -> proxy.matches(ip));
    }

    static String literal(String value) {
        if (value == null || value.isBlank() || value.length() > 45) throw invalid();
        try {
            if (value.indexOf(':') >= 0 && value.matches("[0-9a-fA-F:.]+")) {
                InetAddress.getByName(value);
                return value.toLowerCase(Locale.ROOT);
            }
            String[] parts = value.split("[.]", -1);
            if (parts.length != 4) throw invalid();
            for (String part : parts) {
                if (!part.matches("0|[1-9][0-9]{0,2}") || Integer.parseInt(part) > 255) throw invalid();
            }
            return value;
        } catch (Exception failure) {
            throw invalid();
        }
    }
    private static MobileFailure invalid() { return new MobileFailure(400, "ORIGEM_DISPOSITIVO_INVALIDA"); }
}
