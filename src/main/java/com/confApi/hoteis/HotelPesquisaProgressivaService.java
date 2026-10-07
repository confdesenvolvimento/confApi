package com.confApi.hoteis;

import com.confApi.db.confManager.hotel.model.HotelResponse;
import com.confApi.hoteis.model.pesquisa.HotelPesquisaModelFront;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;

/** Resultados anexados por fornecedor; o cursor nunca depende da ordenacao da tela. */
@Service
public class HotelPesquisaProgressivaService {
    @org.springframework.beans.factory.annotation.Autowired
    private HotelClient client;
    private final HotelSearchService service;
    private final ObjectMapper mapper;
    private final Map<String, Pesquisa> pesquisas = new ConcurrentHashMap<>();
    private final ExecutorService executor = new ThreadPoolExecutor(4, 8, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(32), new ThreadPoolExecutor.AbortPolicy());
    private final ScheduledExecutorService limpeza = Executors.newSingleThreadScheduledExecutor();
    private static final long PRAZO = 180_000, RETENCAO = 600_000;

    public HotelPesquisaProgressivaService(HotelSearchService service, ObjectMapper mapper) {
        this.service = service; this.mapper = mapper;
        limpeza.scheduleWithFixedDelay(this::limpar, 30, 30, TimeUnit.SECONDS);
    }
    public record Pagina(String id, List<HotelResponse> hoteis, int proximoCursor,
                         int totalDisponivel, boolean concluida, List<String> falhas) {}
    static final class Pesquisa {
        final String id = UUID.randomUUID().toString();
        final String dono;
        final long criada = System.currentTimeMillis();
        final HotelPesquisaModelFront criterio;
        final List<HotelResponse> hoteis = new ArrayList<>();
        final List<String> falhas = new ArrayList<>();
        final List<Future<?>> tarefas = new ArrayList<>();
        int pendentes = 2;
        boolean cancelada;
        Pesquisa(String dono, HotelPesquisaModelFront criterio) { this.dono = dono; this.criterio = criterio; }
    }
    public synchronized Pagina iniciar(String dono, HotelPesquisaModelFront criterio) {
        limpar();
        if (pesquisas.size() >= 100 || pesquisas.values().stream().filter(p -> p.dono.equals(dono)).count() >= 3)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Encerre a pesquisa anterior antes de iniciar outra.");
        Pesquisa p = new Pesquisa(dono, mapper.convertValue(criterio, HotelPesquisaModelFront.class));
        pesquisas.put(p.id, p);
        try {
            for (String fornecedor : List.of("EZLink", "Omnibees")) {
                HotelPesquisaModelFront copia = mapper.convertValue(criterio, HotelPesquisaModelFront.class);
                synchronized (p) { p.tarefas.add(executor.submit(() -> consultar(p, copia, fornecedor))); }
            }
        } catch (RejectedExecutionException e) {
            cancelar(dono, p.id);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Muitas pesquisas em andamento. Tente novamente.");
        }
        return pagina(dono, p.id, 0);
    }
    private void consultar(Pesquisa p, HotelPesquisaModelFront criterio, String fornecedor) {
        try {
            List<HotelResponse> resultado = service.pesquisarFornecedor(criterio, fornecedor);
            synchronized (p) {
                if (!p.cancelada) {
                    if (p.hoteis.size() + resultado.size() > 5000) {
                        p.falhas.add("A pesquisa excedeu o limite de resultados. Refine o destino.");
                    } else p.hoteis.addAll(resultado);
                }
            }
        } catch (Exception e) {
            synchronized (p) { if (!p.cancelada) p.falhas.add(fornecedor + ": nao foi possivel concluir a consulta."); }
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING,
                    "Falha na pesquisa " + p.id + " do fornecedor " + fornecedor, e);
        } finally {
            synchronized (p) { p.pendentes = Math.max(0, p.pendentes - 1); }
        }
    }
    public Pagina pagina(String dono, String id, int cursor) {
        Pesquisa p = obter(dono, id);
        synchronized (p) {
            verificarPrazo(p);
            if (cursor < 0 || cursor > p.hoteis.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cursor invalido.");
            int fim = Math.min(cursor + 20, p.hoteis.size());
            return new Pagina(p.id, new ArrayList<>(p.hoteis.subList(cursor, fim)), fim,
                    p.hoteis.size(), p.pendentes == 0, List.copyOf(p.falhas));
        }
    }
    public HotelResponse conteudo(String dono, String id, String fornecedor, String codigo) {
        Pesquisa p = obter(dono, id);
        HotelResponse hotel;
        synchronized (p) {
            hotel = p.hoteis.stream().filter(h -> fornecedor.equals(h.getNomeSistema()) && codigo.equals(h.getCodigoHotelSistema()))
                    .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        }
        return client.carregarConteudo(p.criterio, hotel);
    }

    Pesquisa obter(String dono, String id) {
        Pesquisa p = pesquisas.get(id);
        if (p == null || !p.dono.equals(dono) || System.currentTimeMillis() - p.criada > RETENCAO)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pesquisa expirada. Pesquise novamente.");
        return p;
    }
    public void parar(String dono, String id) {
        Pesquisa p = obter(dono, id);
        synchronized (p) { p.cancelada = true; p.pendentes = 0; p.tarefas.forEach(f -> f.cancel(true)); }
    }
    public void cancelar(String dono, String id) {
        Pesquisa p = obter(dono, id);
        synchronized (p) { p.cancelada = true; p.pendentes = 0; p.tarefas.forEach(f -> f.cancel(true)); }
        pesquisas.remove(id, p);
    }
    private void verificarPrazo(Pesquisa p) {
        if (p.pendentes > 0 && System.currentTimeMillis() - p.criada > PRAZO) {
            p.cancelada = true; p.pendentes = 0;
            p.falhas.add("Tempo limite atingido. Os resultados recebidos foram preservados.");
            p.tarefas.forEach(f -> f.cancel(true));
        }
    }
    private void limpar() {
        pesquisas.forEach((id, p) -> { synchronized (p) {
            verificarPrazo(p);
            if (System.currentTimeMillis() - p.criada > RETENCAO) pesquisas.remove(id, p);
        }});
    }
    @PreDestroy public void encerrar() { executor.shutdownNow(); limpeza.shutdownNow(); pesquisas.clear(); }
}
