package com.confApi.chatconfianca.client;

import com.confApi.confApp.ConfAppResp;
import com.confApi.confApp.ConfAppService;
import com.confApi.exception.ServiceIndisponivelException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatConfiancaTokenProviderTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void deveAutenticarUmaVezEnquantoTokenEstiverValido() {
        ConfAppService confAppService = mock(ConfAppService.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        String token = jwt(NOW.plusSeconds(3600));
        when(confAppService.token()).thenReturn(response(token));
        ChatConfiancaTokenProvider provider = provider(confAppService, meterRegistry);

        assertEquals(token, provider.bearerToken());
        assertEquals(token, provider.bearerToken());

        verify(confAppService, times(1)).token();
        assertEquals(1.0, meterRegistry.get("chatconfianca.auth.cache.requests")
                .tag("result", "hit").counter().count());
        assertEquals(1.0, meterRegistry.get("chatconfianca.auth.cache.requests")
                .tag("result", "miss").counter().count());
    }

    @Test
    void deveCompartilharRenovacaoEntreChamadasConcorrentes() throws Exception {
        ConfAppService confAppService = mock(ConfAppService.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        String token = jwt(NOW.plusSeconds(3600));
        CountDownLatch refreshStarted = new CountDownLatch(1);
        CountDownLatch releaseRefresh = new CountDownLatch(1);
        when(confAppService.token()).thenAnswer(invocation -> {
            refreshStarted.countDown();
            assertTrue(releaseRefresh.await(2, TimeUnit.SECONDS));
            return response(token);
        });
        ChatConfiancaTokenProvider provider = provider(confAppService, meterRegistry);

        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<>();
        try {
            for (int index = 0; index < threads; index++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return provider.bearerToken();
                }));
            }
            start.countDown();
            assertTrue(refreshStarted.await(2, TimeUnit.SECONDS));
            Thread.sleep(100);
            releaseRefresh.countDown();

            for (Future<String> result : results) {
                assertEquals(token, result.get(2, TimeUnit.SECONDS));
            }
        } finally {
            releaseRefresh.countDown();
            executor.shutdownNow();
        }

        verify(confAppService, times(1)).token();
    }

    @Test
    void devePreservarInterrupcaoDeQuemAguardaRenovacao() throws Exception {
        ConfAppService confAppService = mock(ConfAppService.class);
        String token = jwt(NOW.plusSeconds(3600));
        CountDownLatch refreshStarted = new CountDownLatch(1);
        CountDownLatch releaseRefresh = new CountDownLatch(1);
        when(confAppService.token()).thenAnswer(invocation -> {
            refreshStarted.countDown();
            releaseRefresh.await(2, TimeUnit.SECONDS);
            return response(token);
        });
        ChatConfiancaTokenProvider provider =
                provider(confAppService, new SimpleMeterRegistry());
        ExecutorService ownerExecutor = Executors.newSingleThreadExecutor();
        AtomicReference<Throwable> waiterFailure = new AtomicReference<>();
        AtomicBoolean waiterInterrupted = new AtomicBoolean();

        try {
            Future<String> owner = ownerExecutor.submit(provider::bearerToken);
            assertTrue(refreshStarted.await(2, TimeUnit.SECONDS));

            Thread waiter = new Thread(() -> {
                try {
                    provider.bearerToken();
                } catch (Throwable ex) {
                    waiterFailure.set(ex);
                    waiterInterrupted.set(Thread.currentThread().isInterrupted());
                }
            });
            waiter.start();
            Thread.sleep(100L);
            waiter.interrupt();
            waiter.join(2_000L);

            assertFalse(waiter.isAlive());
            assertTrue(waiterFailure.get() instanceof ServiceIndisponivelException);
            assertTrue(waiterInterrupted.get());

            releaseRefresh.countDown();
            assertEquals(token, owner.get(2, TimeUnit.SECONDS));
        } finally {
            releaseRefresh.countDown();
            ownerExecutor.shutdownNow();
        }

        verify(confAppService, times(1)).token();
    }

    @Test
    void deveRenovarComAntecedenciaAntesDoExpDoJwt() {
        ConfAppService confAppService = mock(ConfAppService.class);
        String first = jwt(NOW.plusSeconds(120));
        String second = jwt(NOW.plusSeconds(3600));
        when(confAppService.token()).thenReturn(response(first), response(second));
        MutableClock clock = new MutableClock(NOW);
        ChatConfiancaTokenProvider provider = provider(
                confAppService,
                new SimpleMeterRegistry(),
                clock);

        assertEquals(first, provider.bearerToken());
        clock.advance(Duration.ofSeconds(59));
        assertEquals(first, provider.bearerToken());
        clock.advance(Duration.ofSeconds(1));
        assertEquals(second, provider.bearerToken());

        verify(confAppService, times(2)).token();
    }

    @Test
    void deveInvalidarSomenteOTokenRejeitado() {
        ConfAppService confAppService = mock(ConfAppService.class);
        String first = jwt(NOW.plusSeconds(3600));
        String second = jwt(NOW.plusSeconds(7200));
        when(confAppService.token()).thenReturn(response(first), response(second));
        ChatConfiancaTokenProvider provider =
                provider(confAppService, new SimpleMeterRegistry());

        assertEquals(first, provider.bearerToken());
        provider.invalidateIfCurrent("outro-token");
        assertEquals(first, provider.bearerToken());
        provider.invalidateIfCurrent(first);
        assertEquals(second, provider.bearerToken());

        verify(confAppService, times(2)).token();
    }

    @Test
    void naoDeveCachearFalhaDeAutenticacao() {
        ConfAppService confAppService = mock(ConfAppService.class);
        String token = jwt(NOW.plusSeconds(3600));
        when(confAppService.token())
                .thenThrow(new IllegalStateException("falha interna"))
                .thenReturn(response(token));
        ChatConfiancaTokenProvider provider =
                provider(confAppService, new SimpleMeterRegistry());

        assertThrows(ServiceIndisponivelException.class, provider::bearerToken);
        assertEquals(token, provider.bearerToken());

        verify(confAppService, times(2)).token();
    }

    @Test
    void metricasNaoDevemConterToken() {
        ConfAppService confAppService = mock(ConfAppService.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        String token = jwt(NOW.plusSeconds(3600));
        when(confAppService.token()).thenReturn(response(token));
        ChatConfiancaTokenProvider provider = provider(confAppService, meterRegistry);

        provider.bearerToken();

        assertTrue(meterRegistry.getMeters().stream()
                .flatMap(meter -> meter.getId().getTags().stream())
                .noneMatch(tag -> tag.getValue().contains(token)));
    }

    private ChatConfiancaTokenProvider provider(
            ConfAppService confAppService,
            SimpleMeterRegistry meterRegistry) {
        return provider(
                confAppService,
                meterRegistry,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private ChatConfiancaTokenProvider provider(
            ConfAppService confAppService,
            SimpleMeterRegistry meterRegistry,
            Clock clock) {
        return new ChatConfiancaTokenProvider(
                confAppService,
                new ObjectMapper(),
                meterRegistry,
                clock,
                Duration.ofSeconds(60),
                Duration.ofMinutes(5),
                Duration.ofHours(24)
        );
    }

    private static ConfAppResp response(String token) {
        ConfAppResp response = new ConfAppResp();
        response.setLogin("service");
        response.setToken(token);
        return response;
    }

    private static String jwt(Instant expiresAt) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"exp\":" + expiresAt.getEpochSecond() + "}")
                        .getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".signature";
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(current, zone);
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
