package com.confApi.wooba.sales;

import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.endPoints.recebimento.RecebimentoApi;
import com.confApi.endPoints.reservaAereo.ReservaAereoApi;
import com.confApi.wooba.sales.dto.WoobaSalesDetailsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WoobaIssuedAirReservationImportServiceTest {
    private final ObjectMapper json = new ObjectMapper();
    private final WoobaSalesClient client = mock(WoobaSalesClient.class);
    private final WoobaAirReservationMapper mapper = new WoobaAirReservationMapper();
    private final WoobaAirReservationManagerResolver resolver = mock(WoobaAirReservationManagerResolver.class);
    private final WoobaAirReservationSyncService sync = mock(WoobaAirReservationSyncService.class);
    private final ReservaAereoApi reservas = mock(ReservaAereoApi.class);
    private final RecebimentoApi recebimentos = mock(RecebimentoApi.class);
    private final Map<String, ReservaAereo> database = new HashMap<>();
    private final java.util.ArrayList<Double> paymentTotalsWritten = new java.util.ArrayList<>();
    private final WoobaIssuedAirReservationImportService service = new WoobaIssuedAirReservationImportService(
            client, mapper, resolver, sync, reservas, recebimentos);

    WoobaIssuedAirReservationImportServiceTest() {
        when(reservas.findByLocalizadorCompanhiaParaSincronizacao(anyString(), any()))
                .thenAnswer(invocation -> database.get(invocation.getArgument(0)));
        when(resolver.resolverReferenciasManager(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sync.sincronizar(any())).thenAnswer(invocation -> {
            ReservaAereo reservation = invocation.getArgument(0);
            paymentTotalsWritten.add(reservation.getRecebimentos() == null ? 0.0 : reservation.getRecebimentos().stream()
                    .mapToDouble(payment -> payment.getValrRecebimento()).sum());
            reservation.setCodgReservaAereo(999);
            ReservaAereo existing = database.get(reservation.getLocalizador());
            if (existing == null) {
                database.put(reservation.getLocalizador(), reservation);
            } else {
                for (var passenger : reservation.getPassageiros()) {
                    existing.getPassageiros().stream()
                            .filter(saved -> saved.getNomePassageiro().equals(passenger.getNomePassageiro()))
                            .forEach(saved -> saved.setBilhetes(passenger.getBilhetes()));
                }
                if (reservation.getRecebimentos() != null && !reservation.getRecebimentos().isEmpty()) {
                    existing.setRecebimentos(reservation.getRecebimentos());
                }
                existing.setStatus(reservation.getStatus());
            }
            return WoobaAirReservationSyncResult.processed(reservation);
        });
    }

    @Test
    void naoDeveResolverReferenciasNemGravarBilheteJaImportado() throws Exception {
        WoobaSalesDetailsResponse ticket = details(100, "ATPXGW", "VALERIA", "1272312361467");
        database.put("ATPXGW", mapper.toReservaAereo(ticket, null));
        when(client.details("TKT-ATPXGW")).thenReturn(ticket);
        service.processar("TKT-ATPXGW", 100);
        verifyNoInteractions(resolver, sync);
        verify(client, times(1)).details(anyString());
    }

    @Test
    void deveCriarDivididaPeloHeaderPreservandoLocalizadorDoVoo() throws Exception {
        WoobaSalesDetailsResponse original = details(100, "AATQKC", "ARTHUR", "1272312361466");
        database.put("AATQKC", mapper.toReservaAereo(original, null));
        WoobaSalesDetailsResponse ticket = details(100, "ATPXGW", "VALERIA", "1272312361467");
        WoobaSalesDetailsResponse parent = details(1, "ATPXGW", "VALERIA", "1272312361467");
        when(client.details("TKT-ATPXGW")).thenReturn(ticket);
        when(client.details("AIR-ATPXGW")).thenReturn(parent);
        service.processar("TKT-ATPXGW", 100);
        service.processar("TKT-ATPXGW", 100);
        verify(sync, times(2)).sincronizar(any());
        ReservaAereo saved = database.get("ATPXGW");
        assertEquals(3, saved.getStatus());
        assertEquals("AATQKC", saved.getTrechos().get(0).getVoos().get(0).getLocalizadorCia());
        assertEquals("VALERIA", saved.getPassageiros().get(0).getNomePassageiro());
        assertEquals("1272312361467", saved.getPassageiros().get(0).getBilhetes().get(0).getNumrBilhete());
        assertEquals(1, saved.getRecebimentos().size());
        assertEquals("ARTHUR", database.get("AATQKC").getPassageiros().get(0).getNomePassageiro());
    }

    @Test
    void deveCriarComTodosPassageirosDaReservaEnaoSomenteDoBilhete() throws Exception {
        WoobaSalesDetailsResponse ticket = details(100, "AATQKC", "ARTHUR", "1272312361466");
        WoobaSalesDetailsResponse parent = details(1, "AATQKC", "ARTHUR", "1272312361466");
        var passengers = (com.fasterxml.jackson.databind.node.ArrayNode) parent.getTransaction()
                .path("ProductDetail").path("AirReservationDetail").path("Passengers");
        ObjectNode second = ((ObjectNode) passengers.get(0)).deepCopy();
        second.with("Person").put("FirstName", "VALERIA");
        second.put("ProviderId", "2.1");
        second.put("Ticket", "1272312361467");
        passengers.add(second);
        when(client.details("TKT-AATQKC")).thenReturn(ticket);
        when(client.details("AIR-AATQKC")).thenReturn(parent);
        service.processar("TKT-AATQKC", 100);
        assertEquals(2, database.get("AATQKC").getPassageiros().size());
    }

    @Test
    void deveManterPendenteSeNaoConseguirConferirDetailsDaDivisao() throws Exception {
        WoobaSalesDetailsResponse original = details(100, "AATQKC", "VALERIA", "1272312361467");
        database.put("AATQKC", mapper.toReservaAereo(original, null));
        when(client.details("TKT-ATPXGW")).thenReturn(details(100, "ATPXGW", "VALERIA", "1272312361467"));
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.processar("TKT-ATPXGW", 100));
        assertTrue(error.getMessage().contains("details"));
        verify(reservas, never()).reconciliarDivisaoWooba(any(), any());
        verifyNoInteractions(resolver, sync);
        assertNull(database.get("ATPXGW"));
    }

    @Test
    void deveComplementarPagamentoAusenteMesmoComBilheteExistente() throws Exception {
        WoobaSalesDetailsResponse ticket = details(100, "ATPXGW", "VALERIA", "1272312361467");
        ReservaAereo existing = mapper.toReservaAereo(ticket, null);
        existing.setCodgReservaAereo(999);
        existing.setRecebimentos(List.of());
        database.put("ATPXGW", existing);
        when(recebimentos.findByReservaAereoParaSincronizacao(999)).thenReturn(List.of());
        when(client.details("TKT-ATPXGW")).thenReturn(ticket);
        when(client.details("AIR-ATPXGW")).thenReturn(details(1, "ATPXGW", "VALERIA", "1272312361467"));
        service.processar("TKT-ATPXGW", 100);
        verify(sync).sincronizar(any());
        assertEquals(404.45, database.get("ATPXGW").getRecebimentos().get(0).getValrRecebimento());
    }

    @Test
    void deveConsultarBilhetesVinculadosMesmoQuandoAirNaoTrazNumeros() throws Exception {
        WoobaSalesDetailsResponse parent = details(1, "ATPXGW", "VALERIA", null);
        ((ObjectNode) parent.getTransaction()).putArray("Links").addObject()
                .put("TransactionType", 100).put("TransactionState", 4).put("UniqueId", "TKT-ATPXGW");
        database.put("ATPXGW", mapper.toReservaAereo(parent, null));
        when(client.details("AIR-ATPXGW")).thenReturn(parent);
        when(client.details("TKT-ATPXGW")).thenReturn(details(100, "ATPXGW", "VALERIA", "1272312361467"));
        service.processar("AIR-ATPXGW", 1);
        verify(sync).sincronizar(any());
        assertEquals("1272312361467", database.get("ATPXGW").getPassageiros().get(0).getBilhetes().get(0).getNumrBilhete());
    }

    @Test
    void naoDeveRelacionarPassageirosDiferentesApenasPorProviderId() throws Exception {
        database.put("ATPXGW", mapper.toReservaAereo(details(1, "ATPXGW", "ARTHUR", null), null));
        when(client.details("AIR-ATPXGW")).thenReturn(details(1, "ATPXGW", "VALERIA", "1272312361467"));
        assertThrows(IllegalStateException.class, () -> service.processar("AIR-ATPXGW", 1));
        verifyNoInteractions(resolver, sync);
    }

    @Test
    void naoDeveGravarQuandoConsultaAoManagerFalhar() throws Exception {
        when(client.details("AIR-ATPXGW")).thenReturn(details(1, "ATPXGW", "VALERIA", "1272312361467"));
        when(reservas.findByLocalizadorCompanhiaParaSincronizacao(anyString(), any()))
                .thenThrow(new IllegalStateException("Manager indisponivel"));
        assertThrows(IllegalStateException.class, () -> service.processar("AIR-ATPXGW", 1));
        verifyNoInteractions(resolver, sync);
    }

    @Test
    void deveIgnorarCustomerENaoReativarCanceladas() throws Exception {
        WoobaSalesDetailsResponse ticket = details(100, "ATPXGW", "VALERIA", "1272312361467");
        ((ObjectNode) ticket.getTransaction().path("Context")).putObject("Customer").put("Id", 10);
        when(client.details("TKT-ATPXGW")).thenReturn(ticket);
        service.processar("TKT-ATPXGW", 100);
        verifyNoInteractions(reservas, sync);

        ((ObjectNode) ticket.getTransaction().path("Context")).putNull("Customer");
        ReservaAereo canceled = mapper.toReservaAereo(ticket, null);
        canceled.setStatus(2);
        database.put("ATPXGW", canceled);
        service.processar("TKT-ATPXGW", 100);
        verifyNoInteractions(sync);
    }

    @Test
    void deveRejeitarVinculoAirDeOutroLocalizador() throws Exception {
        when(client.details("TKT-ATPXGW")).thenReturn(details(100, "ATPXGW", "VALERIA", "1272312361467"));
        when(client.details("AIR-ATPXGW")).thenReturn(details(1, "AATQKC", "VALERIA", "1272312361467"));
        assertThrows(IllegalStateException.class, () -> service.processar("TKT-ATPXGW", 100));
        verifyNoInteractions(sync);
    }

    @Test
    void deveManterPendenteQuandoManagerNaoConfirmarGravacao() throws Exception {
        when(client.details("AIR-ATPXGW")).thenReturn(details(1, "ATPXGW", "VALERIA", "1272312361467"));
        doReturn(WoobaAirReservationSyncResult.processed(new ReservaAereo())).when(sync).sincronizar(any());
        assertThrows(IllegalStateException.class, () -> service.processar("AIR-ATPXGW", 1));
    }

    @Test
    void naoDeveGravarPagamentoAgregadoDoAirAntesDoPagamentoDoBilhete() throws Exception {
        WoobaSalesDetailsResponse parent = details(1, "ATPXGW", "VALERIA", "1272312361467");
        ((ObjectNode) parent.getTransaction().path("Payments").get(0)).put("TransactionAmount", 808.90);
        when(client.details("AIR-ATPXGW")).thenReturn(parent);
        when(client.details("TKT-ATPXGW")).thenReturn(details(100, "ATPXGW", "VALERIA", "1272312361467"));
        service.processar("TKT-ATPXGW", 100);
        verify(sync, times(2)).sincronizar(any());
        assertEquals(List.of(0.0, 404.45), paymentTotalsWritten);
        assertEquals(404.45, database.get("ATPXGW").getRecebimentos().get(0).getValrRecebimento());
    }

    @Test
    void deveDetectarBilheteDaOriginalMesmoQuandoNumeroVierSomenteNosLinksDoAir() throws Exception {
        database.put("AATQKC", mapper.toReservaAereo(details(100, "AATQKC", "VALERIA", "1272312361467"), null));
        WoobaSalesDetailsResponse parent = details(1, "ATPXGW", "VALERIA", null);
        ((ObjectNode) parent.getTransaction()).withArray("Links").addObject().put("TransactionType", 100)
                .put("TransactionState", 4).put("Ticket", "1272312361467").put("UniqueId", "TKT-ATPXGW");
        when(client.details("AIR-ATPXGW")).thenReturn(parent);
        assertThrows(IllegalStateException.class, () -> service.processar("AIR-ATPXGW", 1));
        verifyNoInteractions(sync);
    }

    @Test
    void deveReconciliarGkpxntPorBilheteSemDuplicarNaReexecucao() throws Exception {
        prepararDivisaoGkpxnt();
        service.processar("TKT-GKPXNT", 100);
        service.processar("TKT-GKPXNT", 100);
        service.processar("AIR-GKPXNT", 1);
        verificarDivisaoGkpxnt();
    }

    @Test
    void deveReconciliarGkpxntPeloAirSemBilhetesNosPassageiros() throws Exception {
        prepararDivisaoGkpxnt();
        service.processar("AIR-GKPXNT", 1);
        verificarDivisaoGkpxnt();
    }

    @Test
    void naoDeveTransferirEnquantoPassageiroAindaConstarNoDetailsOriginal() throws Exception {
        prepararDivisaoGkpxnt();
        when(client.details("AIR-AEFIBN")).thenReturn(details(1, "AEFIBN", "DIVIDIDO", null));
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.processar("AIR-GKPXNT", 1));
        assertTrue(error.getMessage().contains("ainda consta"));
        verify(reservas, never()).reconciliarDivisaoWooba(any(), any());
        verifyNoInteractions(sync);
    }

    @Test
    void naoDeveTransferirComAgenciaDiferenteNoBilhete() throws Exception {
        prepararDivisaoGkpxnt();
        var ticket = client.details("TKT-GKPXNT");
        ((ObjectNode) ticket.getTransaction().path("Context").path("Agency")).put("Id", 999);
        assertThrows(IllegalStateException.class, () -> service.processar("AIR-GKPXNT", 1));
        verify(reservas, never()).reconciliarDivisaoWooba(any(), any());
    }

    @Test
    void falhaDaApiDeDivisaoNaoDeveCairNaCriacaoNormal() throws Exception {
        prepararDivisaoGkpxnt();
        when(reservas.reconciliarDivisaoWooba(any(), any())).thenThrow(new IllegalStateException("Manager indisponivel"));
        assertThrows(IllegalStateException.class, () -> service.processar("AIR-GKPXNT", 1));
        assertNull(database.get("GKPXNT"));
        assertEquals(2, database.get("AEFIBN").getPassageiros().size());
        verifyNoInteractions(sync);
    }

    @Test
    void deveManterPendenteSeApiDivisaoResponderSemTransferir() throws Exception {
        prepararDivisaoGkpxnt();
        when(reservas.reconciliarDivisaoWooba(any(), any())).thenReturn(999);
        assertThrows(IllegalStateException.class, () -> service.processar("AIR-GKPXNT", 1));
        verifyNoInteractions(sync);
    }

    private void prepararDivisaoGkpxnt() throws Exception {
        var ticket = details(100, "GKPXNT", "DIVIDIDO", "1272312369758");
        var parent = details(1, "GKPXNT", "DIVIDIDO", null);
        for (var response : List.of(ticket, parent)) {
            var product = response.getTransaction().path("ProductDetail");
            var air = product.path(response == parent ? "AirReservationDetail" : "AirTicketDetail");
            ((ObjectNode) air.path("Flights").get(0)).put("AirlineLocator", "AEFIBN");
        }
        ((ObjectNode) parent.getTransaction()).putArray("Payments");
        ((ObjectNode) parent.getTransaction()).withArray("Links").addObject().put("TransactionType", 100)
                .put("TransactionState", 4).put("Ticket", "1272312369758").put("UniqueId", "TKT-GKPXNT");
        ReservaAereo source = mapper.toReservaAereo(details(1, "AEFIBN", "ORIGINAL", "1272312369757"), null);
        source.setCodgReservaAereo(256643);
        source.setPassageiros(new java.util.ArrayList<>(source.getPassageiros()));
        var moved = mapper.toReservaAereo(ticket, null).getPassageiros().get(0);
        moved.setCodgPassageiro(42);
        moved.setIdPassageiroCia("2.1");
        source.getPassageiros().add(moved);
        source.setRecebimentos(mapper.toReservaAereo(ticket, null).getRecebimentos());
        database.put("AEFIBN", source);
        when(client.details("AIR-AEFIBN")).thenReturn(details(1, "AEFIBN", "ORIGINAL", null));
        when(client.details("TKT-GKPXNT")).thenReturn(ticket);
        when(client.details("AIR-GKPXNT")).thenReturn(parent);
        when(reservas.reconciliarDivisaoWooba(eq(256643), any())).thenAnswer(invocation -> {
            ReservaAereo destination = invocation.getArgument(1);
            assertEquals("1272312369758", destination.getPassageiros().get(0).getBilhetes().get(0).getNumrBilhete());
            assertEquals(1, destination.getRecebimentos().size());
            moved.setIdPassageiroCia(destination.getPassageiros().get(0).getIdPassageiroCia());
            destination.setPassageiros(List.of(moved));
            destination.setCodgReservaAereo(999);
            source.getPassageiros().remove(moved);
            source.setRecebimentos(List.of());
            database.put("GKPXNT", destination);
            return 999;
        });
    }

    @Test
    void deveConferirOriginalCriadaPorWebhookTktUsandoWoobaAirUniqueId() throws Exception {
        prepararDivisaoGkpxnt();
        database.get("AEFIBN").setRegraReserva("WoobaUniqueId=TKT-ANTIGO; WoobaAirUniqueId=AIR-AEFIBN");
        service.processar("AIR-GKPXNT", 1);
        verificarDivisaoGkpxnt();
    }

    private void verificarDivisaoGkpxnt() {
        verify(reservas, times(1)).reconciliarDivisaoWooba(eq(256643), any());
        verifyNoInteractions(sync);
        assertEquals(42, database.get("GKPXNT").getPassageiros().get(0).getCodgPassageiro());
        assertEquals(1, database.get("GKPXNT").getRecebimentos().size());
        assertEquals(1, database.get("AEFIBN").getPassageiros().size());
        assertEquals("1272312369757", database.get("AEFIBN").getPassageiros().get(0).getBilhetes().get(0).getNumrBilhete());
    }

    private WoobaSalesDetailsResponse details(int type, String locator, String name, String ticket) throws Exception {
        WoobaSalesDetailsResponse response = json.readValue("""
                {"Success":true,"Transaction":{
                  "Header":{"TransactionType":1,"TransactionState":4,"LastUpdate":"2026-09-22T09:35:25"},
                  "Context":{"Customer":null,"Agency":{"Id":1872}},"User":{"Username":"teste"},
                  "Payments":[{"PaymentType":1,"PaymentState":4,"PaymentFormDescription":"Invoice",
                    "TransactionAmount":404.45,"Amount":404.45,"Date":"2026-09-22T00:00:00","Links":[]}],
                  "Links":[],"ProductDetail":{"AirReservationDetail":{
                    "Airline":{"Code":"G3"},"IssueDate":"2026-09-22T00:00:00",
                    "Flights":[{"AirlineLocator":"AATQKC","MarketingAirline":{"Code":"G3"}}],
                    "Passengers":[{"Person":{"FirstName":"TESTE","Surname":"SILVA"},"ProviderId":"1.1","TypeCode":"ADT"}]}}
                }}
                """, WoobaSalesDetailsResponse.class);
        ObjectNode transaction = (ObjectNode) response.getTransaction();
        transaction.with("Header").put("TransactionType", type).put("Locator", locator)
                .put("UniqueId", (type == 1 ? "AIR-" : "TKT-") + locator).put("Ticket", ticket);
        ObjectNode detail = (ObjectNode) transaction.path("ProductDetail").path("AirReservationDetail");
        ObjectNode passenger = (ObjectNode) detail.path("Passengers").get(0);
        passenger.with("Person").put("FirstName", name);
        passenger.put("Ticket", ticket);
        ObjectNode payment = (ObjectNode) transaction.path("Payments").get(0);
        payment.withArray("Links").addObject().put("UniqueId", "TKT-" + locator).put("Ticket", ticket);
        if (type == 100) {
            transaction.with("ProductDetail").set("AirTicketDetail", detail);
            transaction.with("ProductDetail").putNull("AirReservationDetail");
            transaction.withArray("Links").addObject().put("TransactionType", 1)
                    .put("TransactionTypeDescription", "AirReservation").put("UniqueId", "AIR-" + locator);
        }
        return response;
    }
}
