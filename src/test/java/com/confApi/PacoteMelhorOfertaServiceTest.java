package com.confApi;

import com.confApi.cacheHotel.PacoteMelhorOfertaClient;
import com.confApi.cacheHotel.PacoteMelhorOfertaDTO;
import com.confApi.cacheHotel.PacoteMelhorOfertaRequest;
import com.confApi.cacheHotel.PacoteMelhorOfertaService;
import com.confApi.chatgpt.dto.ChatActionDTO;
import com.confApi.chatgpt.tools.ToolSchemas;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PacoteMelhorOfertaServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void consultaJaneiroConverteCincoDiasEmQuatroNoitesEGeraAcao() {
        PacoteMelhorOfertaClient client = mock(PacoteMelhorOfertaClient.class);
        PacoteMelhorOfertaDTO oferta = new PacoteMelhorOfertaDTO();
        oferta.setOrigem("MAO");
        oferta.setDestino("FOR");
        oferta.setNomeCidade("Fortaleza");
        oferta.setNomeEstado("Ceara");
        oferta.setNomePais("Brasil");
        oferta.setDataIda(java.sql.Date.valueOf(LocalDate.of(2027, 1, 10)));
        oferta.setDataVolta(java.sql.Date.valueOf(LocalDate.of(2027, 1, 14)));
        oferta.setQuantidadeNoites(4);
        oferta.setQuantidadeAdultos(2);
        oferta.setQuantidadeQuartos(1);
        oferta.setValorTotal(4200.0);
        PacoteMelhorOfertaDTO.HotelResumo hotel = new PacoteMelhorOfertaDTO.HotelResumo();
        hotel.setNomeHotel("Hotel Fortaleza");
        oferta.setHotelEconomico(hotel);
        when(client.consultar(any())).thenReturn(List.of(oferta));

        Map<String, Object> resultado = new PacoteMelhorOfertaService(client).consultar(Map.of(
                "origem", "MAO",
                "destino", "FOR",
                "mesIda", "2027-01",
                "duracaoDias", 5));

        ArgumentCaptor<PacoteMelhorOfertaRequest> captor =
                ArgumentCaptor.forClass(PacoteMelhorOfertaRequest.class);
        verify(client).consultar(captor.capture());
        assertThat(captor.getValue().getDataIdaInicio())
                .isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(captor.getValue().getDataIdaFim())
                .isEqualTo(LocalDate.of(2027, 1, 31));
        assertThat(captor.getValue().getDuracaoMinimaNoites()).isEqualTo(4);
        assertThat(captor.getValue().getDuracaoMaximaNoites()).isEqualTo(4);
        assertThat(resultado.get("status")).isEqualTo("OK");
        assertThat(resultado.get("mensagem").toString())
                .contains("4 noites (5 dias)", "2 adultos", "4.200,00");
        List<ChatActionDTO> actions = (List<ChatActionDTO>) resultado.get("actions");
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).code()).isEqualTo("pesquisar_pacotes");
        assertThat(actions.get(0).localizador())
                .contains("origem=MAO", "destino=FOR", "dataIda=2027-01-10");
    }

    @Test
    void schemaDePacoteExplicaMetropolesEAnoFuturo() {
        assertThat(ToolSchemas.searchCheapestPackages().name())
                .isEqualTo("search_cheapest_packages");
        assertThat(ToolSchemas.searchCheapestPackages().jsonSchema().toString())
                .contains("RIO", "proxima ocorrencia futura", "duracaoDias");
    }
}
