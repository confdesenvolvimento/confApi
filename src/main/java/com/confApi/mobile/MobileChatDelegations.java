package com.confApi.mobile;

import org.springframework.stereotype.Component;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

/** Identidade interna, descartável e vinculada à requisição já validada pelo gateway. */
@Component
public class MobileChatDelegations {
    public static final String HEADER = "X-Confianca-Mobile-Delegation";
    private final Map<String, Grant> grants = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;
    public MobileChatDelegations() { this(Clock.systemUTC()); }
    MobileChatDelegations(Clock clock) { this.clock = clock; }
    private record Grant(MobileSessions.Session session, String method, String path,
                         String query, byte[] digest, Instant expires) {}

    synchronized String issue(MobileSessions.Session session, String method, String path, String query, byte[] body) {
        Instant now = clock.instant();
        grants.values().removeIf(g -> !g.expires.isAfter(now));
        if (!path.startsWith("v1/chat-confianca/") || !active(session, now))
            throw new MobileFailure(401, "SESSAO_INVALIDA");
        if (grants.size() >= 10000) throw new MobileFailure(503, "INTEGRACAO_INDISPONIVEL");
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        grants.put(token, new Grant(session, method, "/" + path, query, digest(body), now.plusSeconds(30)));
        return token;
    }

    synchronized MobileChatAuthentication redeem(String token, String method, String path, String query, byte[] body) {
        Grant grant = grants.remove(token);
        Instant now = clock.instant();
        if (grant == null || !grant.expires.isAfter(now) || !active(grant.session, now)
                || !grant.method.equals(method) || !grant.path.equals(path)
                || !Objects.equals(grant.query, query) || !MessageDigest.isEqual(grant.digest, digest(body)))
            throw new MobileFailure(401, "DELEGACAO_INVALIDA");
        return new MobileChatAuthentication(grant.session.login, grant.session.profile.path("id").asInt());
    }

    synchronized void discard(String token) { grants.remove(token); }
    private static boolean active(MobileSessions.Session session, Instant now) {
        return session.primary && !session.revoked && session.until.isAfter(now)
                && session.accessUntil != null && session.accessUntil.isAfter(now);
    }
    private static byte[] digest(byte[] body) {
        try { return MessageDigest.getInstance("SHA-256").digest(body); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
