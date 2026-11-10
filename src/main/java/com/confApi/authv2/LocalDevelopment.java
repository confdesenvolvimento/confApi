package com.confApi.authv2;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.*;

/** Explicit local development only. Never enabled by a header or client request. */
@Component
public final class LocalDevelopment {
    private final boolean enabled;
    private final Set<String> origins;
    @org.springframework.beans.factory.annotation.Autowired
    public LocalDevelopment(Environment environment) {
        var profiles = Arrays.asList(environment.getActiveProfiles());
        enabled = Boolean.parseBoolean(environment.getProperty("app.dev", "false"))
            && profiles.stream().anyMatch(p -> Set.of("dev", "local").contains(p))
            && profiles.stream().noneMatch(p -> Set.of("prod", "production").contains(p));
        origins = new HashSet<>();
        if (enabled) for (String value : environment.getProperty("app.dev.allowed-origins", "").split(",")) {
            if (value.isBlank()) continue;
            URI uri = URI.create(value.trim());
            if (!validHttp(uri) || (uri.getPath() != null && !Set.of("", "/").contains(uri.getPath())))
                throw new IllegalStateException("Configure somente origens locais em APP_DEV_ALLOWED_ORIGINS");
            origins.add(origin(uri));
        }
    }
    private LocalDevelopment() { enabled = false; origins = Set.of(); }
    public static LocalDevelopment disabled() { return new LocalDevelopment(); }
    public boolean allows(URI uri) { return enabled && validHttp(uri) && origins.contains(origin(uri)); }
    public boolean allows(HttpServletRequest request) {
        try {
            return !request.isSecure() && "http".equals(request.getScheme())
                && localAddress(request.getRemoteAddr()) && localAddress(request.getLocalAddr())
                && allows(new URI("http", null, request.getServerName(), request.getServerPort(), null, null, null));
        } catch (Exception invalid) { return false; }
    }
    private static boolean validHttp(URI uri) {
        return "http".equals(uri.getScheme()) && localAddress(uri.getHost())
            && uri.getUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null;
    }
    private static String origin(URI uri) {
        return uri.getHost().toLowerCase(Locale.ROOT) + ":" + (uri.getPort() < 0 ? 80 : uri.getPort());
    }
    static boolean localAddress(String host) {
        if (host == null) return false;
        if (Set.of("localhost", "::1", "[::1]", "0:0:0:0:0:0:0:1").contains(host)) return true;
        if (host.startsWith("::ffff:")) host = host.substring(7);
        if (!host.matches("[0-9]{1,3}(?:[.][0-9]{1,3}){3}")) return false;
        int[] parts = Arrays.stream(host.split("[.]")).mapToInt(Integer::parseInt).toArray();
        if (Arrays.stream(parts).anyMatch(p -> p > 255)) return false;
        return parts[0] == 127 || parts[0] == 10 || (parts[0] == 192 && parts[1] == 168)
            || (parts[0] == 172 && parts[1] >= 16 && parts[1] <= 31);
    }
}
