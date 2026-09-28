package com.confApi.wooba.sales;

import com.confApi.db.confManager.reservaAereo.ReservaAereo;
import com.confApi.endPoints.reservaAereo.ReservaAereoApi;
import com.confApi.wooba.sales.dto.WoobaManualImportRequest;
import com.confApi.wooba.sales.dto.WoobaSalesDetailsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WoobaManualImportServiceTest {
    private final ObjectMapper json = new ObjectMapper();
    private final WoobaSalesClient client = mock(WoobaSalesClient.class);
    private final WoobaSalesProperties properties = new WoobaSalesProperties();
    private final WoobaAirReservationMapper mapper = new WoobaAirReservationMapper();
    private final WoobaAirReservationManagerResolver resolver = mock(WoobaAirReservationManagerResolver.class);
    private final WoobaAirReservationSyncService sync = mock(WoobaAirReservationSyncService.class);
    private final WoobaIssuedAirReservationImportService issued = mock(WoobaIssuedAirReservationImportService.class);
    private final ReservaAereoApi reservas = mock(ReservaAereoApi.class);
    private final WoobaManualImportService service = new WoobaManualImportService(client, properties, mapper, resolver, sync, issued, reservas);
    private ReservaAereo saved;
    private WoobaSalesDetailsResponse detail;
    private ObjectNode list;

    @BeforeEach
    void setup() throws Exception {
        detail = details("AIR-JTTOQR", "G3", 2);
        list = json.createObjectNode().put("Success", true);
        list.putArray("Transactions").addObject().put("TransactionType", 1).put("TransactionState", 2)
                .put("Locator", "JTTOQR").put("UniqueId", "AIR-JTTOQR");
        when(client.list(any())).thenReturn(list);
        when(client.details("AIR-JTTOQR")).thenReturn(detail);
        when(reservas.findByLocalizadorCompanhiaParaSincronizacao(anyString(), any())).thenAnswer(i -> saved);
        when(resolver.resolverReferenciasManager(any())).thenAnswer(i -> i.getArgument(0));
        when(sync.sincronizar(any())).thenAnswer(i -> {
            saved = i.getArgument(0);
            saved.setCodgReservaAereo(264800);
            return WoobaAirReservationSyncResult.processed(saved);
        });
    }

    @Test
    void deveImportarJttoqrComFiltroDeCriacaoDoDiaEConfirmarRegistro() {
        var response = service.importar(request(" jttoqr ", "2026-09-23", null));
        assertTrue(response.sucesso());
        assertEquals("CRIADA", response.acao());
        assertEquals(264800, response.codgReservaAereo());
        assertEquals(2, response.passageiros());
        assertEquals("Reservada", response.descricaoStatus());
        ArgumentCaptor<com.fasterxml.jackson.databind.JsonNode> query = ArgumentCaptor.forClass(com.fasterxml.jackson.databind.JsonNode.class);
        verify(client).list(query.capture());
        assertEquals("Create", query.getValue().path("FilterDateType").asText());
        assertEquals("JTTOQR", query.getValue().path("Locator").asText());
        assertEquals("2026-09-23T00:00-03:00", query.getValue().path("DateFrom").asText());
        assertEquals("2026-09-23T23:59:59.999-03:00", query.getValue().path("DateTo").asText());
        assertEquals("[1]", query.getValue().path("TransactionTypes").toString());
        assertFalse(query.getValue().has("TransactionStates"));
        verifyNoInteractions(issued);
    }

    @Test
    void segundaImportacaoDeveConferirMesmoRegistro() {
        service.importar(request("JTTOQR", "2026-09-23", null));
        var response = service.importar(request("JTTOQR", "2026-09-23", null));
        assertEquals("CONFERIDA", response.acao());
        assertEquals(264800, response.codgReservaAereo());
    }

    @Test
    void emitidaDeveReutilizarFluxoQueTrataBilhetesPagamentosEDivisoes() throws Exception {
        detail = details("AIR-JTTOQR", "G3", 4);
        when(client.details("AIR-JTTOQR")).thenReturn(detail);
        doAnswer(i -> {
            saved = mapper.toReservaAereo(i.getArgument(0), null);
            saved.setCodgReservaAereo(264800);
            return null;
        }).when(issued).processarDetails(any(), eq(1));
        var response = service.importar(request("JTTOQR", "2026-09-23", null));
        assertEquals("Emitida", response.descricaoStatus());
        verify(issued).processarDetails(detail, 1);
        verifyNoInteractions(sync, resolver);
    }

    @Test
    void deveRecusarLocalizadorAmbiguoAntesDeGravar() throws Exception {
        list.withArray("Transactions").addObject().put("TransactionType", 1).put("Locator", "JTTOQR").put("UniqueId", "AIR-LA");
        when(client.details("AIR-LA")).thenReturn(details("AIR-LA", "LA", 2));
        assertStatus(409, request("JTTOQR", "2026-09-23", null));
        verifyNoInteractions(reservas, resolver, sync, issued);
    }

    @Test
    void companhiaOpcionalDeveDesempatarEAceitarLaJj() throws Exception {
        list.withArray("Transactions").addObject().put("TransactionType", 1).put("Locator", "JTTOQR").put("UniqueId", "AIR-LA");
        when(client.details("AIR-LA")).thenReturn(details("AIR-LA", "LA", 2));
        assertEquals("LA", service.importar(request("JTTOQR", "2026-09-23", "JJ")).companhia());
    }

    @Test
    void deveIgnorarResultadosDeOutrosLocalizadoresEProdutos() {
        list.putArray("Transactions").addObject().put("TransactionType", 2).put("Locator", "JTTOQR").put("UniqueId", "HTL-1");
        list.withArray("Transactions").addObject().put("TransactionType", 1).put("Locator", "OUTRO").put("UniqueId", "AIR-2");
        assertStatus(404, request("JTTOQR", "2026-09-23", null));
        verify(client, never()).details(any());
        verifyNoInteractions(sync, issued);
    }

    @Test
    void customerPreenchidoNaoDeveGravar() {
        ((ObjectNode) detail.getTransaction().path("Context")).putObject("Customer").put("Id", 123);
        assertStatus(409, request("JTTOQR", "2026-09-23", null));
        verifyNoInteractions(sync, issued);
    }

    @Test
    void detailsRelidosAntesDeGravarDevemBloquearCancelamentoRecente() throws Exception {
        when(client.details("AIR-JTTOQR")).thenReturn(detail, details("AIR-JTTOQR", "G3", 5));
        assertStatus(409, request("JTTOQR", "2026-09-23", null));
        verifyNoInteractions(sync, issued);
    }

    @Test
    void naoDeveReativarCanceladaNemRebaixarEmitida() {
        saved = mapper.toReservaAereo(detail, null);
        saved.setStatus(2);
        assertStatus(409, request("JTTOQR", "2026-09-23", null));
        saved.setStatus(3);
        assertStatus(409, request("JTTOQR", "2026-09-23", null));
        verifyNoInteractions(sync, issued);
    }

    @Test
    void reservaParcialNaoDeveSerConfirmadaNemAtualizadaSilenciosamente() {
        saved = mapper.toReservaAereo(detail, null);
        saved.setPassageiros(List.of(saved.getPassageiros().get(0)));
        assertStatus(409, request("JTTOQR", "2026-09-23", null));
        verifyNoInteractions(sync, issued);
    }

    @Test
    void managerSemConfirmarPersistenciaNaoDeveRetornarSucesso() {
        doReturn(WoobaAirReservationSyncResult.processed(new ReservaAereo())).when(sync).sincronizar(any());
        assertStatus(409, request("JTTOQR", "2026-09-23", null));
    }

    @Test
    void bilheteVinculadoNaoImportadoNaoDeveGerarSucessoParcial() throws Exception {
        detail = details("AIR-JTTOQR", "G3", 4);
        ((ObjectNode) detail.getTransaction()).withArray("Links").addObject()
                .put("TransactionType", 100).put("TransactionState", 4).put("Ticket", "1272312369758");
        when(client.details("AIR-JTTOQR")).thenReturn(detail);
        saved = mapper.toReservaAereo(detail, null);
        saved.setCodgReservaAereo(264800);
        assertStatus(409, request("JTTOQR", "2026-09-23", null));
    }

    @Test
    void codigoZeroNaoConfirmaGravacao() {
        saved = mapper.toReservaAereo(detail, null);
        saved.setCodgReservaAereo(0);
        doReturn(WoobaAirReservationSyncResult.processed(saved)).when(sync).sincronizar(any());
        assertStatus(409, request("JTTOQR", "2026-09-23", null));
    }

    @Test
    void falhaDeConsultaAoManagerNaoSignificaReservaAusente() {
        when(reservas.findByLocalizadorCompanhiaParaSincronizacao(anyString(), any())).thenThrow(new IllegalStateException("offline"));
        assertThrows(IllegalStateException.class, () -> service.importar(request("JTTOQR", "2026-09-23", null)));
        verifyNoInteractions(sync, issued);
    }

    @Test
    void listComErroNaoDeveProsseguir() {
        list.put("Success", false);
        assertStatus(502, request("JTTOQR", "2026-09-23", null));
        verify(client, never()).details(any());
    }

    @Test
    void detailsDivergentesNaoDevemGravar() {
        ((ObjectNode) detail.getTransaction().path("Header")).put("Locator", "OUTRO");
        assertStatus(409, request("JTTOQR", "2026-09-23", null));
        verifyNoInteractions(sync, issued);
    }

    @Test
    void entradaInvalidaNaoDeveChamarServicosExternos() {
        assertStatus(400, null);
        assertStatus(400, request("", "2026-09-23", null));
        assertStatus(400, new WoobaManualImportRequest("JTTOQR", null, null));
        assertStatus(400, request("JTTOQR", "2099-01-01", null));
        assertStatus(400, request("JTTOQR", "2026-09-23", "GOL"));
        verifyNoInteractions(client, reservas, sync, issued);
    }

    private void assertStatus(int status, WoobaManualImportRequest request) {
        assertEquals(status, assertThrows(ResponseStatusException.class, () -> service.importar(request)).getRawStatusCode());
    }

    private WoobaManualImportRequest request(String locator, String date, String company) {
        return new WoobaManualImportRequest(locator, LocalDate.parse(date), company);
    }

    private WoobaSalesDetailsResponse details(String id, String company, int state) throws Exception {
        var response = json.readValue("""
                {"Success":true,"Transaction":{"Header":{"TransactionType":1,"Locator":"JTTOQR","LastUpdate":"2026-09-24T10:00:00"},
                "Context":{"Customer":null,"Agency":{"Id":1872}},"User":{"Username":"teste"},
                "Payments":[],"Links":[],"ProductDetail":{"AirReservationDetail":{"Airline":{},
                "Passengers":[{"Person":{"FirstName":"ANA","Surname":"TESTE"},"ProviderId":"1.1","TypeCode":"ADT"},
                {"Person":{"FirstName":"BIA","Surname":"TESTE"},"ProviderId":"2.1","TypeCode":"ADT"}],"Flights":[]}}}}
                """, WoobaSalesDetailsResponse.class);
        ((ObjectNode) response.getTransaction().path("Header")).put("UniqueId", id).put("TransactionState", state);
        ((ObjectNode) response.getTransaction().path("ProductDetail").path("AirReservationDetail").path("Airline")).put("Code", company);
        return response;
    }
}
