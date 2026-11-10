package com.confApi.hoteis;

import com.confApi.db.confManager.hotel.model.*;
import com.confApi.db.confManager.recebimento.Recebimento;
import com.confApi.db.confManager.reservaHotel.dto.ReservaHotel;
import com.confApi.db.confManager.reservaPacote.ReservaPacote;
import com.confApi.endPoints.recebimento.*;
import com.confApi.hoteis.model.reserva.CancelarReservaRequestHotelFront;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class HotelV2MigrationTest {
    HotelServiceV2 service;
    RecebimentoApi recebimentos;
    HotelClientV2 client;
    ReservaHotelModel pedido;
    ReservaHotel registro;
    Recebimento recebimento;

    @BeforeEach void setup() {
        service = mock(HotelServiceV2.class);
        recebimentos = mock(RecebimentoApi.class);
        client = new HotelClientV2(service, null, null);
        ReflectionTestUtils.setField(client, "recebimentoApi", recebimentos);
        pedido = new ReservaHotelModel();
        pedido.setCodgReservaHotelDB(42L); pedido.setCodgHotel("999"); pedido.setLocalizador("HOT42");
        pedido.setHotel(new HotelResponse());
        registro = new ReservaHotel(); registro.setCodgReservaHotel(42); registro.setLocalizador("HOT42"); registro.setStatus(1);
        recebimento = new Recebimento(); recebimento.setCodgRecebimento(7); recebimento.setStatus(1);
        registro.setRecebimentos(List.of(recebimento));
        when(service.consultarReservaDB(pedido)).thenReturn(registro);
    }
    @Test void recusaFornecedorNaoBaixaCadastroNemRecebimentos() {
        when(service.cancelarReserva(any())).thenReturn(false);
        assertEquals("Erro ao cancelar a reserva", client.cancelarReserva(pedido));
        verify(service, never()).cancelarReservaDB(any()); verifyNoInteractions(recebimentos);
    }
    @Test void falhaPersistenciaNaoCancelaRecebimento() {
        when(service.cancelarReserva(any())).thenReturn(true);
        doThrow(new IllegalStateException("não confirmado")).when(service).cancelarReservaDB(pedido);
        assertThrows(IllegalStateException.class, () -> client.cancelarReserva(pedido));
        verifyNoInteractions(recebimentos);
    }
    @Test void confirmaFornecedorDepoisCadastroDepoisRecebimentoPersistido() {
        when(service.cancelarReserva(any())).thenReturn(true);
        when(recebimentos.cancelar(7, recebimento)).thenReturn(List.of(new CancelarResponse("Recebimento Banco", "Cancelamento integral confirmado.")));
        assertEquals("Cancelado com sucesso!", client.cancelarReserva(pedido));
        var order = inOrder(service, recebimentos);
        order.verify(service).consultarReservaDB(pedido);
        order.verify(service).cancelarReserva(any(CancelarReservaRequestHotelFront.class));
        order.verify(service).cancelarReservaDB(pedido);
        order.verify(recebimentos).cancelar(7, recebimento);
    }
    @Test void respostaFinanceiraSemConfirmacaoNaoViraSucesso() {
        when(service.cancelarReserva(any())).thenReturn(true);
        when(recebimentos.cancelar(7, recebimento)).thenReturn(List.of(new CancelarResponse("Recebimento Banco", "Cancelamento integral não confirmado.")));
        assertThrows(IllegalStateException.class, () -> client.cancelarReserva(pedido));
    }
    @Test void pacoteUsaSeuProprioFluxo() {
        registro.setCodgReservaPacote(new ReservaPacote(8));
        assertThrows(IllegalArgumentException.class, () -> client.cancelarReserva(pedido));
        verify(service, never()).cancelarReserva(any()); verifyNoInteractions(recebimentos);
    }
    @Test void repeticaoNaoRecancelaFornecedorNemRecebimentoBaixado() {
        registro.setStatus(2); recebimento.setStatus(0);
        assertEquals("Cancelado com sucesso!", client.cancelarReserva(pedido));
        verify(service, never()).cancelarReserva(any()); verifyNoInteractions(recebimentos);
    }
    @Test void pesquisaV2PreservaCalculosECacheAtuais() {
        HotelSearchService pesquisa = mock(HotelSearchService.class);
        ReflectionTestUtils.setField(client, "pesquisaAtual", pesquisa);
        var req = new com.confApi.hoteis.model.pesquisa.HotelPesquisaModelFront();
        List<HotelResponse> resultado = List.of(new HotelResponse());
        when(pesquisa.pesquisar(req)).thenReturn(resultado);
        assertSame(resultado, client.pesquisar(req));
        verify(pesquisa).pesquisar(req); verifyNoInteractions(service);
    }
}
