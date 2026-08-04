package com.confApi.chatconfianca.client;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.exception.ServiceIndisponivelException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class ChatConfiancaTokenProvider {
    private final ConfAppService confAppService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Clock clock;
    private final Duration refreshSkew;
    private final Duration fallbackTtl;
    private final Duration maxTtl;
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Object refreshLock = new Object();

    private volatile TokenEntry cachedToken;
    private volatile CompletableFuture<TokenEntry> refreshInProgress;

    @Autowired
    public ChatConfiancaTokenProvider(
            ConfAppService confAppService,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            @Value("${chat-confianca.auth.refresh-skew-seconds:60}") long refreshSkewSeconds,
            @Value("${chat-confianca.auth.fallback-ttl-seconds:300}") long fallbackTtlSeconds,
            @Value("${chat-confianca.auth.max-ttl-seconds:86400}") long maxTtlSeconds) {
        this(
                confAppService,
                objectMapper,
                meterRegistry,
                Clock.systemUTC(),
                Duration.ofSeconds(positivo(refreshSkewSeconds, 60)),
                Duration.ofSeconds(positivo(fallbackTtlSeconds, 300)),
                Duration.ofSeconds(positivo(maxTtlSeconds, 86400))
        );
    }

    ChatConfiancaTokenProvider(
            ConfAppService confAppService,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            Clock clock,
            Duration refreshSkew,
            Duration fallbackTtl,
            Duration maxTtl) {
        this.confAppService = Objects.requireNonNull(confAppService, "confAppService");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.refreshSkew = validarDuracao(refreshSkew, "refreshSkew");
        this.fallbackTtl = validarDuracao(fallbackTtl, "fallbackTtl");
        this.maxTtl = validarDuracao(maxTtl, "maxTtl");
        this.cacheHits = Counter.builder("chatconfianca.auth.cache.requests")
                .tag("result", "hit")
                .register(meterRegistry);
        this.cacheMisses = Counter.builder("chatconfianca.auth.cache.requests")
                .tag("result", "miss")
                .register(meterRegistry);
    }

    public String bearerToken() {
        Instant agora = clock.instant();
        TokenEntry atual = cachedToken;
        if (utilizavel(atual, agora)) {
            cacheHits.increment();
            return atual.token();
        }

        cacheMisses.increment();
        return aguardarOuRenovar().token();
    }

    public void invalidateIfCurrent(String rejectedToken) {
        if (rejectedToken == null || rejectedToken.isBlank()) {
            return;
        }
        synchronized (refreshLock) {
            TokenEntry atual = cachedToken;
            if (atual != null && Objects.equals(atual.token(), rejectedToken)) {
                cachedToken = null;
            }
        }
    }

    private TokenEntry aguardarOuRenovar() {
        CompletableFuture<TokenEntry> refresh;
        boolean responsavelPelaRenovacao = false;

        synchronized (refreshLock) {
            TokenEntry atual = cachedToken;
            if (utilizavel(atual, clock.instant())) {
                return atual;
            }
            refresh = refreshInProgress;
            if (refresh == null) {
                refresh = new CompletableFuture<>();
                refreshInProgress = refresh;
                responsavelPelaRenovacao = true;
            }
        }

        if (responsavelPelaRenovacao) {
            renovar(refresh);
        }
        return aguardar(refresh);
    }

    private void renovar(CompletableFuture<TokenEntry> refresh) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            ConfAppResp response = confAppService.token();
            if (response == null || response.getToken() == null || response.getToken().isBlank()) {
                throw new ServiceIndisponivelException(
                        "Nao foi possivel obter token para chamar o manager.");
            }
            Instant agora = clock.instant();
            TokenEntry novo = new TokenEntry(
                    response.getToken(),
                    calcularRefreshAt(response.getToken(), agora)
            );
            synchronized (refreshLock) {
                cachedToken = novo;
                clearRefreshLocked(refresh);
            }
            refresh.complete(novo);
        } catch (RuntimeException ex) {
            outcome = "failure";
            clearRefresh(refresh);
            refresh.completeExceptionally(normalizarFalha(ex));
        } finally {
            sample.stop(Timer.builder("chatconfianca.auth.refresh")
                    .tag("outcome", outcome)
                    .register(meterRegistry));
            clearRefresh(refresh);
        }
    }

    private void clearRefresh(CompletableFuture<TokenEntry> refresh) {
        synchronized (refreshLock) {
            clearRefreshLocked(refresh);
        }
    }

    private void clearRefreshLocked(CompletableFuture<TokenEntry> refresh) {
        if (refreshInProgress == refresh) {
            refreshInProgress = null;
        }
    }

    private TokenEntry aguardar(CompletableFuture<TokenEntry> refresh) {
        try {
            return refresh.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServiceIndisponivelException(
                    "Autenticacao do manager interrompida.");
        } catch (ExecutionException ex) {
            Throwable causa = ex.getCause();
            if (causa instanceof ServiceIndisponivelException) {
                throw (ServiceIndisponivelException) causa;
            }
            throw new ServiceIndisponivelException(
                    "Nao foi possivel obter token para chamar o manager.");
        }
    }

    private RuntimeException normalizarFalha(RuntimeException ex) {
        if (ex instanceof ServiceIndisponivelException) {
            return ex;
        }
        return new ServiceIndisponivelException(
                "Nao foi possivel obter token para chamar o manager.");
    }

    private Instant calcularRefreshAt(String token, Instant agora) {
        Instant limiteMaximo = agora.plus(maxTtl);
        Instant expiracao = extrairExpiracao(token);
        Instant limite = expiracao == null || expiracao.isAfter(limiteMaximo)
                ? limiteMaximo
                : expiracao;
        if (expiracao == null) {
            limite = agora.plus(fallbackTtl);
            if (limite.isAfter(limiteMaximo)) {
                limite = limiteMaximo;
            }
        }
        Instant refreshAt = limite.minus(refreshSkew);
        return refreshAt.isAfter(agora) ? refreshAt : agora;
    }

    private Instant extrairExpiracao(String token) {
        try {
            String[] partes = token.split("\\.");
            if (partes.length < 2) {
                return null;
            }
            byte[] payload = Base64.getUrlDecoder().decode(partes[1]);
            JsonNode exp = objectMapper
                    .readTree(new String(payload, StandardCharsets.UTF_8))
                    .get("exp");
            return exp != null && exp.canConvertToLong()
                    ? Instant.ofEpochSecond(exp.asLong())
                    : null;
        } catch (RuntimeException | java.io.IOException ex) {
            return null;
        }
    }

    private boolean utilizavel(TokenEntry entry, Instant agora) {
        return entry != null && entry.refreshAt().isAfter(agora);
    }

    private static Duration validarDuracao(Duration valor, String nome) {
        if (valor == null || valor.isNegative() || valor.isZero()) {
            throw new IllegalArgumentException(nome + " deve ser positivo.");
        }
        return valor;
    }

    private static long positivo(long valor, long padrao) {
        return valor > 0 ? valor : padrao;
    }

    private record TokenEntry(String token, Instant refreshAt) {
    }
}
