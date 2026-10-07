package com.confApi.wooba.sales;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WoobaIssuedSalesPollingServiceTest {
    @TempDir Path directory;
    private final ObjectMapper mapper = new ObjectMapper();
    private final WoobaSalesClient client = mock(WoobaSalesClient.class);
    private final WoobaIssuedAirReservationImportService importer = mock(WoobaIssuedAirReservationImportService.class);
    private final WoobaIssuedPollingProperties settings = new WoobaIssuedPollingProperties();
    private final WoobaSalesProperties sales = new WoobaSalesProperties();
    private final Instant now = Instant.parse("2026-09-22T14:00:00Z");

    @Test
    void deveSepararTiposEIgnorarReservadasCanceladasEOutrosProdutos() throws Exception {
        when(client.list(any())).thenReturn(mapper.readTree("""
                {"Success":true,"Transactions":[
                 {"TransactionType":100,"TransactionState":4,"UniqueId":"TKT-1","LastUpdate":"2026-09-22T09:34:39","Locator":"AATQKC"},
                 {"TransactionType":1,"TransactionState":4,"UniqueId":"AIR-1","LastUpdate":"2026-09-22T09:35:25","Locator":"ATPXGW"},
                 {"TransactionType":100,"TransactionState":5,"TransactionStateDescription":"Issued","UniqueId":"TKT-CANCELADO"},
                 {"TransactionType":1,"TransactionState":2,"UniqueId":"AIR-RESERVADA"},
                 {"TransactionType":2,"TransactionState":4,"UniqueId":"HOTEL"}]}
                """));
        service(now).executar(100, settings.getTickets());
        service(now).executar(1, settings.getReservations());
        verify(importer).processar("TKT-1", 100);
        verify(importer).processar("AIR-1", 1);
        verifyNoMoreInteractions(importer);
        ArgumentCaptor<JsonNode> requests = ArgumentCaptor.forClass(JsonNode.class);
        verify(client, times(2)).list(requests.capture());
        assertEquals(100, requests.getAllValues().get(0).path("TransactionTypes").get(0).asInt());
        assertEquals(1, requests.getAllValues().get(1).path("TransactionTypes").get(0).asInt());
        assertEquals(4, requests.getValue().path("TransactionStates").get(0).asInt());
        assertEquals("LastUpdate", requests.getValue().path("FilterDateType").asText());
    }

    @Test
    void deveRetomarPendenciasAposReinicioSemPerderExcedenteDoLote() throws Exception {
        settings.setMaxItemsPerCycle(1);
        when(client.list(any())).thenReturn(mapper.readTree("""
                {"Success":true,"Transactions":[
                 {"TransactionType":100,"TransactionState":4,"UniqueId":"TKT-1","LastUpdate":"2026-09-22T09:34:39"},
                 {"TransactionType":100,"TransactionState":4,"UniqueId":"TKT-2","LastUpdate":"2026-09-22T09:34:40"}]}
                """), mapper.readTree("{\"Success\":true,\"Transactions\":[]}"));
        service(now).executar(100, settings.getTickets());
        assertEquals(1, store().load().stream(100).getPending().size());
        service(now.plusSeconds(7200)).executar(100, settings.getTickets());
        assertTrue(store().load().stream(100).getPending().isEmpty());
        verify(importer).processar("TKT-1", 100);
        verify(importer).processar("TKT-2", 100);
    }

    @Test
    void deveManterFalhaPendenteEDeixarOutrosItensAvancarem() throws Exception {
        settings.setMaxItemsPerCycle(1);
        when(client.list(any())).thenReturn(mapper.readTree("""
                {"Transactions":[
                 {"TransactionType":100,"TransactionState":4,"UniqueId":"TKT-1","LastUpdate":"2026-09-22T09:34:39"},
                 {"TransactionType":100,"TransactionState":4,"UniqueId":"TKT-2","LastUpdate":"2026-09-22T09:34:40"}]}
                """));
        doThrow(new IllegalStateException("Manager indisponivel")).doNothing().when(importer).processar("TKT-1", 100);
        service(now).executar(100, settings.getTickets());
        assertEquals(2, store().load().stream(100).getPending().size());
        service(now.plusSeconds(300)).executar(100, settings.getTickets());
        verify(importer).processar("TKT-2", 100);
        service(now.plusSeconds(600)).executar(100, settings.getTickets());
        verify(importer, times(2)).processar("TKT-1", 100);
        assertTrue(store().load().stream(100).getPending().isEmpty());
    }

    @Test
    void deveIgnorarVersaoConcluidaMasReprocessarLocalizadorAlterado() throws Exception {
        JsonNode first = mapper.readTree("""
                {"Transactions":[{"TransactionType":100,"TransactionState":4,"UniqueId":"TKT-1",
                "LastUpdate":"2026-09-22T09:34:39","Locator":"AATQKC"}]}
                """);
        JsonNode split = mapper.readTree(first.toString().replace("AATQKC", "ATPXGW"));
        when(client.list(any())).thenReturn(first, first, split);
        service(now).executar(100, settings.getTickets());
        service(now.plusSeconds(300)).executar(100, settings.getTickets());
        service(now.plusSeconds(600)).executar(100, settings.getTickets());
        verify(importer, times(2)).processar("TKT-1", 100);
    }

    @Test
    void naoDeveAvancarCursorQuandoSalesRetornaFalha() throws Exception {
        when(client.list(any())).thenReturn(mapper.readTree("{\"Success\":false,\"Transactions\":[]}"));
        service(now).executar(100, settings.getTickets());
        assertNull(store().load().stream(100).getQueriedThrough());
        verifyNoInteractions(importer);
    }

    @Test
    void deveConsultarIntervalosLimitadosSemPularPeriodoAposParada() throws Exception {
        settings.getTickets().setInitialLookbackMinutes(1440);
        settings.setQueryWindowMinutes(60);
        when(client.list(any())).thenReturn(mapper.readTree("{\"Success\":true,\"Transactions\":[]}"));
        service(now).executar(100, settings.getTickets());
        service(now.plusSeconds(7200)).executar(100, settings.getTickets());
        ArgumentCaptor<JsonNode> requests = ArgumentCaptor.forClass(JsonNode.class);
        verify(client, times(2)).list(requests.capture());
        OffsetDateTime firstEnd = OffsetDateTime.parse(requests.getAllValues().get(0).path("DateTo").asText());
        OffsetDateTime nextStart = OffsetDateTime.parse(requests.getAllValues().get(1).path("DateFrom").asText());
        assertEquals(firstEnd.minusMinutes(5), nextStart);
    }

    @Test
    void parametrosDesabilitadosNaoDevemConsultar() {
        service(now).consultarBilhetes();
        service(now).conferirReservasEmitidas();
        verifyNoInteractions(client, importer);
    }

    @Test
    void naoDeveBloquearSchedulerNemAcumularExecucoesDoMesmoTipo() throws Exception {
        settings.getTickets().setEnabled(true);
        when(client.list(any())).thenReturn(mapper.readTree("""
                {"Transactions":[{"TransactionType":100,"TransactionState":4,"UniqueId":"TKT-1",
                "LastUpdate":"2026-09-22T09:34:39","Locator":"ATPXGW"}]}
                """));
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            entered.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return null;
        }).when(importer).processar("TKT-1", 100);
        WoobaIssuedSalesPollingService service = service(now);
        try {
            assertTimeoutPreemptively(java.time.Duration.ofSeconds(1), service::consultarBilhetes);
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            service.consultarBilhetes();
        } finally {
            release.countDown();
            service.shutdown();
        }
        verify(client, times(1)).list(any());
        verify(importer, times(1)).processar("TKT-1", 100);
    }

    @Test
    void arquivoDeEstadoInvalidoNaoDeveDispararImportacaoSemControle() throws Exception {
        java.nio.file.Files.writeString(directory.resolve("state.json"), "arquivo incompleto");
        service(now).executar(100, settings.getTickets());
        verifyNoInteractions(client, importer);
    }

    private WoobaIssuedPollingStore store() {
        return new WoobaIssuedPollingStore(mapper, directory.resolve("state.json").toString());
    }

    private WoobaIssuedSalesPollingService service(Instant time) {
        return new WoobaIssuedSalesPollingService(client, importer, settings, store(), sales, Clock.fixed(time, ZoneOffset.UTC));
    }
}
