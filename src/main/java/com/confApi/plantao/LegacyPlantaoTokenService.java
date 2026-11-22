package com.confApi.plantao;

import com.confApi.authv2.V2;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/** Fluxo legado autenticado. Reiniciar a instância invalida os desafios, sem liberar validação global. */
@Service
public class LegacyPlantaoTokenService {
    private static final int MAX_PENDING = 10_000;
    private final RestTemplate http;
    private final String base;
    private final LegacyPlantaoTokenDecoder decoder;
    private final byte[] hmacKey = new byte[32];
    private final Map<String, Challenge> pending = new ConcurrentHashMap<>();
    private final Map<String, Window> limits = new HashMap<>();
    private final Object lock = new Object();

    public LegacyPlantaoTokenService(RestTemplateBuilder builder, @Value("${cservices-url:}") String base,
            LegacyPlantaoTokenDecoder decoder) {
        this.http = builder.setConnectTimeout(Duration.ofSeconds(3)).setReadTimeout(Duration.ofSeconds(8)).build();
        this.base = base; this.decoder = decoder; new SecureRandom().nextBytes(hmacKey);
    }

    public PlantaoTokenResponse gerar(PlantaoTokenRequest request) {
        String owner = caller();
        if (V2.enabled() || owner == null || request == null || !validIdentity(request.login(), request.canal())
                || request.reference() == null || !request.reference().matches("[A-Za-z0-9_-]{32,128}")) return failure();
        String login = request.login().trim();
        String key = login.toLowerCase(Locale.ROOT) + "|" + request.canal();
        Challenge challenge;
        synchronized (lock) {
            Instant now = now();
            pending.values().removeIf(c -> !c.expires.isAfter(now));
            limits.values().removeIf(w -> !w.expires.isAfter(now));
            for (Challenge c : pending.values()) {
                if (c.owner.equals(owner) && c.login.equals(login) && c.channel.equals(request.canal())
                        && c.reference.equals(request.reference())) return c.digest == null ? failure() : response(c);
            }
            Window window = limits.get(key);
            if (pending.size() >= MAX_PENDING || (window == null && limits.size() >= MAX_PENDING)
                    || (window != null && window.count >= 3)) return failure();
            limits.put(key, new Window(window == null ? now.plusSeconds(300) : window.expires, window == null ? 1 : window.count + 1));
            // Uma solicitação vigente por usuário/canal; trocar a referência não zera o limite.
            pending.values().removeIf(c -> c.login.equalsIgnoreCase(login) && c.channel.equals(request.canal()));
            challenge = new Challenge(UUID.randomUUID().toString(), owner, login, request.canal(), request.reference(), now.plusSeconds(300));
            pending.put(challenge.id, challenge);
        }
        try {
            String code = decoder.decode(generate(login, challenge.channel));
            synchronized (lock) {
                if (pending.get(challenge.id) != challenge || !challenge.expires.isAfter(now())) return failure();
                challenge.digest = digest(challenge.id, code);
                return response(challenge);
            }
        } catch (RuntimeException unavailable) {
            pending.remove(challenge.id, challenge); return failure();
        }
    }

    public PlantaoTokenResponse validar(PlantaoTokenValidationRequest request) {
        String owner = caller();
        if (V2.enabled() || owner == null || request == null || request.challengeId() == null) return failure();
        Challenge c = pending.get(request.challengeId());
        if (c == null) return failure();
        synchronized (c) {
            if (pending.get(c.id) != c || c.digest == null || !c.expires.isAfter(now()) || !c.owner.equals(owner)
                    || !c.login.equals(request.login() == null ? null : request.login().trim())
                    || !c.channel.equals(request.canal()) || !c.reference.equals(request.reference())) return failure();
            if (++c.attempts > 3) { pending.remove(c.id, c); return failure(); }
            if (request.token() == null || !request.token().matches("[0-9]{4}")
                    || !MessageDigest.isEqual(c.digest, digest(c.id, request.token()))) {
                if (c.attempts >= 3) pending.remove(c.id, c);
                return failure();
            }
            // Consome antes do efeito externo. Retorno incerto não permite reenviar a validação.
            if (!pending.remove(c.id, c)) return failure();
            try {
                return consume(c.login, c.channel, request.token())
                        ? new PlantaoTokenResponse(true, null, "Confirmação concluída.", c.id, c.reference, null) : failure();
            } catch (RuntimeException unavailable) { return failure(); }
        }
    }

    protected String generate(String login, String channel) {
        ResponseEntity<JsonNode> result = http.exchange(uri("api", "token", "gerartoken", key(channel), login),
                HttpMethod.GET, new HttpEntity<>(headers()), JsonNode.class);
        if (!result.getStatusCode().is2xxSuccessful() || result.getBody() == null) throw new IllegalStateException();
        return result.getBody().path("valor").asText(null);
    }
    protected boolean consume(String login, String channel, String code) {
        ResponseEntity<String> result = http.exchange(uri("api", "token", "validar", Integer.valueOf(code).toString(), key(channel)),
                HttpMethod.POST, new HttpEntity<>(Map.of("usuarioLoginAcesso", login), headers()), String.class);
        return result.getStatusCode() == HttpStatus.OK && "Token válido".equals(result.getBody());
    }
    protected Instant now() { return Instant.now(); }
    protected String caller() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)
                && auth.getName() != null && !auth.getName().isBlank() ? auth.getName() : null;
    }
    private java.net.URI uri(String... parts) {
        return UriComponentsBuilder.fromHttpUrl(base).pathSegment(parts).build().encode().toUri();
    }
    private HttpHeaders headers() { var h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON); h.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN)); return h; }
    private static String key(String channel) { return "EMAIL".equals(channel) ? "0003PLANTAO" : "0004PLANTAO"; }
    private static boolean validIdentity(String login, String channel) {
        return login != null && !login.trim().isEmpty() && login.length() <= 128
                && login.chars().noneMatch(Character::isISOControl) && ("APP".equals(channel) || "EMAIL".equals(channel));
    }
    private byte[] digest(String id, String code) {
        try { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            return mac.doFinal((id + ":" + code).getBytes(StandardCharsets.UTF_8));
        } catch (Exception impossible) { throw new IllegalStateException(impossible); }
    }
    private static PlantaoTokenResponse response(Challenge c) { return new PlantaoTokenResponse(true, null, "Confirme a solicitação de plantão.", c.id, c.reference, c.expires.toString()); }
    private static PlantaoTokenResponse failure() { return PlantaoTokenResponse.falha("Código inválido ou serviço de plantão indisponível."); }
    private static final class Challenge {
        final String id, owner, login, channel, reference; final Instant expires;
        volatile byte[] digest; int attempts;
        Challenge(String id, String owner, String login, String channel, String reference, Instant expires) {
            this.id=id; this.owner=owner; this.login=login; this.channel=channel; this.reference=reference; this.expires=expires;
        }
    }
    private record Window(Instant expires, int count) { }
}
