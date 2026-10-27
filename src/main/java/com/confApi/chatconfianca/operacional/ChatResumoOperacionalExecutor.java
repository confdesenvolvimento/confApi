package com.confApi.chatconfianca.operacional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

/** Bulkhead compartilhado: IO lento nao cria threads/filas ilimitadas nem bloqueia todo o resumo. */
@Component
public class ChatResumoOperacionalExecutor implements DisposableBean {
    private final ThreadPoolExecutor pool;
    private final long timeoutMillis;

    public ChatResumoOperacionalExecutor() { this(12_000); }

    ChatResumoOperacionalExecutor(long timeoutMillis) {
        this.timeoutMillis = Math.max(1, Math.min(12_000, timeoutMillis));
        pool = new ThreadPoolExecutor(4, 4, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(16), r -> {
            Thread t = new Thread(r, "chat-resumo-operacional"); t.setDaemon(true); return t;
        }, new ThreadPoolExecutor.AbortPolicy());
        pool.allowCoreThreadTimeOut(true);
    }

    public record Consulta(String codigo, String titulo, Supplier<ChatResumoOperacionalFonte> executar) { }

    public List<ChatResumoOperacionalFonte> consultar(List<Consulta> consultas) {
        if (consultas == null || consultas.size() > 5) throw new IllegalArgumentException("Consultas invalidas.");
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        List<Future<ChatResumoOperacionalFonte>> futures = new ArrayList<>();
        for (Consulta consulta : consultas) {
            try { futures.add(pool.submit(consulta.executar()::get)); }
            catch (RejectedExecutionException ex) { futures.add(null); }
        }
        List<ChatResumoOperacionalFonte> resultado = new ArrayList<>();
        try {
            for (int i = 0; i < consultas.size(); i++) {
                Consulta c = consultas.get(i);
                Future<ChatResumoOperacionalFonte> f = futures.get(i);
                try {
                    if (f == null) throw new RejectedExecutionException();
                    long restante = deadline - System.nanoTime();
                    ChatResumoOperacionalFonte fonte = f.isDone() ? f.get() : f.get(Math.max(0, restante), TimeUnit.NANOSECONDS);
                    if (fonte == null || !c.codigo().equals(fonte.codigo())) throw new IllegalStateException("Fonte incoerente.");
                    resultado.add(fonte);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    resultado.add(ChatResumoOperacionalFonte.indisponivel(c.codigo(), c.titulo()));
                } catch (ExecutionException | TimeoutException | RejectedExecutionException | IllegalStateException ex) {
                    resultado.add(ChatResumoOperacionalFonte.indisponivel(c.codigo(), c.titulo()));
                }
            }
        } finally {
            for (var f : futures) if (f != null && !f.isDone()) f.cancel(true);
            pool.purge();
        }
        return List.copyOf(resultado);
    }

    @Override public void destroy() { pool.shutdownNow(); }
}
